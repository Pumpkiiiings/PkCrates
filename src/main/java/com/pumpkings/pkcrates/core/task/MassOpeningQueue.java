package com.pumpkings.pkcrates.core.task;

import com.pumpkings.pkcrates.core.animation.SummaryAnimation;
import com.pumpkings.pkcrates.core.event.MassOpeningRewardEvent;
import com.pumpkings.pkcrates.core.model.claim.ClaimReason;
import com.pumpkings.pkcrates.core.model.reward.IReward;
import com.pumpkings.pkcrates.core.model.reward.UnifiedReward;
import com.pumpkings.pkcrates.core.service.ClaimService;
import com.pumpkings.pkcrates.infrastructure.claim.ClaimConfig;
import com.pumpkings.pkcrates.infrastructure.config.MessageManager;
import com.pumpkings.pkcrates.infrastructure.config.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MassOpeningQueue extends BukkitRunnable {

    private final Plugin plugin;
    private final ClaimService claimService;
    private final ClaimConfig claimConfig;
    private final MessageManager messageManager;
    private final int rewardsPerTick;

    private final Queue<PendingMassOpening> queue = new ConcurrentLinkedQueue<>();

    public MassOpeningQueue(Plugin plugin, ClaimService claimService, ClaimConfig claimConfig, MessageManager messageManager, int rewardsPerTick) {
        this.plugin = plugin;
        this.claimService = claimService;
        this.claimConfig = claimConfig;
        this.messageManager = messageManager;
        this.rewardsPerTick = Math.max(1, rewardsPerTick);
    }

    public void addPending(PendingMassOpening pending) {
        if (pending != null && !pending.getRewards().isEmpty()) {
            queue.add(pending);
        }
    }

    @Override
    public void run() {
        if (queue.isEmpty()) return;

        Iterator<PendingMassOpening> iterator = queue.iterator();
        while (iterator.hasNext()) {
            PendingMassOpening pending = iterator.next();
            Player player = pending.getPlayer();

            if (player == null || !player.isOnline()) {
                // Never drop undelivered rewards — park them in the claim system so the
                // player collects them with /crate claim on their next login.
                storeRemaining(pending, ClaimReason.PLAYER_OFFLINE);
                iterator.remove();
                continue;
            }

            int processed = 0;
            List<IReward> rewards = pending.getRewards();
            int startIndex = pending.getCurrentIndex();

            while (processed < rewardsPerTick && startIndex + processed < rewards.size()) {
                int index = startIndex + processed;
                IReward reward = rewards.get(index);

                MassOpeningRewardEvent rewardEvent = new MassOpeningRewardEvent(player, pending.getCrate(), reward, index);
                Bukkit.getPluginManager().callEvent(rewardEvent);

                if (!rewardEvent.isCancelled()) {
                    deliverReward(player, pending.getCrate().getId(), reward);
                }

                processed++;
            }

            pending.incrementIndex(processed);

            if (pending.isFinished()) {
                SummaryAnimation.play(player, pending.getCrate(), rewards.size(), rewards);
                iterator.remove();
            }
        }
    }

    /**
     * Drains the queue into the claim system and clears it.
     *
     * <p>Called from {@code onDisable} so a restart mid-batch does not silently destroy
     * rewards the player already paid keys for.</p>
     *
     * @return The number of rewards parked as claims.
     */
    public int flushToClaims() {
        int stored = 0;
        PendingMassOpening pending;
        while ((pending = queue.poll()) != null) {
            stored += storeRemaining(pending, ClaimReason.DELIVERY_ERROR);
        }
        return stored;
    }

    /**
     * Moves every not-yet-delivered reward of a batch into the claim system.
     *
     * @return The number of rewards stored.
     */
    private int storeRemaining(PendingMassOpening pending, ClaimReason reason) {
        List<IReward> remaining = pending.getRemainingRewards();
        if (remaining.isEmpty()) return 0;

        if (!claimConfig.isEnabled()) {
            plugin.getLogger().warning("Claim system is disabled — " + remaining.size()
                    + " mass opening reward(s) for '" + pending.getPlayerName() + "' were lost.");
            return 0;
        }

        String crateId = pending.getCrate().getId();
        for (IReward reward : remaining) {
            claimService.addClaim(pending.getPlayerUuid(), reward, crateId, reason);
        }
        pending.incrementIndex(remaining.size());

        plugin.getLogger().info("Stored " + remaining.size() + " undelivered mass opening reward(s) for '"
                + pending.getPlayerName() + "' (" + reason.name() + ").");
        return remaining.size();
    }

    private void deliverReward(Player player, String crateId, IReward reward) {
        if (claimConfig.isEnabled() && claimConfig.storeIfInventoryFull()
                && reward instanceof UnifiedReward unified
                && !unified.getItems().isEmpty()
                && !hasInventorySpace(player, unified.getItems())) {

            claimService.addClaim(player, reward, crateId, ClaimReason.INVENTORY_FULL);
            if (claimConfig.notifyOnStore()) {
                messageManager.sendMessage(player, Messages.CLAIM_STORED_NOTIFICATION);
            }
            return;
        }

        try {
            reward.give(player);
            messageManager.sendMessage(player, Messages.REWARD_WON,
                    java.util.Map.of("<reward>", reward.getId(), "<crate>", crateId));
        } catch (Exception e) {
            plugin.getLogger().severe("Error delivering mass opening reward '" + reward.getId() + "' to '" + player.getName() + "': " + e.getMessage());
            if (claimConfig.isEnabled() && claimConfig.storeIfDeliveryFailed()) {
                claimService.addClaim(player, reward, crateId, ClaimReason.DELIVERY_ERROR);
                if (claimConfig.notifyOnStore()) {
                    messageManager.sendMessage(player, Messages.CLAIM_STORED_NOTIFICATION);
                }
            }
        }
    }

    private boolean hasInventorySpace(Player player, List<ItemStack> items) {
        long freeSlots = 0;
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (slot == null || slot.getType() == Material.AIR) {
                freeSlots++;
            }
        }
        return freeSlots >= items.size();
    }
}
