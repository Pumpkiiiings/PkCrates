package com.pumpkings.pkcrates.core.task;

import com.pumpkings.pkcrates.core.animation.AnimationPhase;
import com.pumpkings.pkcrates.core.model.claim.ClaimReason;
import com.pumpkings.pkcrates.core.model.reward.IReward;
import com.pumpkings.pkcrates.core.model.reward.UnifiedReward;
import com.pumpkings.pkcrates.core.model.session.CrateSession;
import com.pumpkings.pkcrates.core.service.ClaimService;
import com.pumpkings.pkcrates.core.service.SessionManager;
import com.pumpkings.pkcrates.infrastructure.claim.ClaimConfig;
import com.pumpkings.pkcrates.infrastructure.config.MessageManager;
import com.pumpkings.pkcrates.infrastructure.config.Messages;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-tick task that advances all active crate-opening sessions.
 *
 * <p>When a session ends, the reward delivery follows this priority:
 * <ol>
 *   <li>If the player's inventory has space → deliver normally via {@link IReward#give}.</li>
 *   <li>If the inventory is full AND {@link ClaimConfig#storeIfInventoryFull()} → send to claims.</li>
 *   <li>Fallback: deliver anyway (items may drop to the ground — Bukkit default behaviour).</li>
 * </ol>
 * </p>
 */
public class CrateTickTask extends BukkitRunnable {

    private final SessionManager sessionManager;
    private final Plugin plugin;
    private final ClaimService claimService;
    private final ClaimConfig claimConfig;
    private final MessageManager messageManager;

    /** Tracks which animation phase is currently running for each session. */
    private final Map<CrateSession, AnimationPhase> currentAnimations = new HashMap<>();

    /**
     * @param plugin         Owning plugin.
     * @param sessionManager Manages active crate sessions.
     * @param claimService   Used to store rewards that cannot be delivered.
     * @param claimConfig    Claim module configuration.
     * @param messageManager Used to send feedback to players.
     */
    public CrateTickTask(Plugin plugin, SessionManager sessionManager,
                         ClaimService claimService, ClaimConfig claimConfig,
                         MessageManager messageManager) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
        this.claimService = claimService;
        this.claimConfig = claimConfig;
        this.messageManager = messageManager;
    }

    /**
     * Registers an animation phase for the given session and starts it immediately.
     *
     * @param session   The session to animate.
     * @param animation The animation phase to play.
     */
    public void playAnimation(CrateSession session, AnimationPhase animation) {
        currentAnimations.put(session, animation);
        animation.onStart(session);
    }

    @Override
    public void run() {
        if (sessionManager.getActiveSessions().isEmpty()) return;

        List<CrateSession> toRemove = new ArrayList<>();

        for (CrateSession session : sessionManager.getActiveSessions()) {
            AnimationPhase currentPhase = currentAnimations.get(session);

            if (currentPhase != null) {
                currentPhase.onTick(session);

                if (currentPhase.isFinished(session)) {
                    currentPhase.onEnd(session);
                    session.setFinished(true);
                }
            } else {
                // No animation registered → finish immediately
                session.setFinished(true);
            }

            if (session.isFinished()) {
                toRemove.add(session);
            } else {
                session.incrementTicks();
            }
        }

        // Clean up finished sessions and deliver rewards
        for (CrateSession session : toRemove) {
            currentAnimations.remove(session);
            sessionManager.endSession(session.getBlockLocation());

            IReward wonReward = session.getWonReward();
            if (wonReward == null) continue;

            Player player = session.getPlayer();
            deliverReward(session, wonReward);
        }
    }

    // -------------------------------------------------------------------------
    // Internal delivery logic
    // -------------------------------------------------------------------------

    /**
     * Attempts to deliver the reward to the player.
     *
     * <p>If the claim module is active and the inventory is full,
     * stores the reward via {@link ClaimService} instead of dropping items.</p>
     *
     * @param session   The crate session that finished.
     * @param reward    The reward to deliver.
     */
    private void deliverReward(CrateSession session, IReward reward) {
        
        Player player = session.getPlayer();
        String crateId = session.getCrate().getId();
        
        com.pumpkings.pkcrates.infrastructure.audit.api.AuditService audit = 
            ((com.pumpkings.pkcrates.PkCratesPlugin) plugin).getAuditService();
            
        // Check whether the claim module is active and configured to intercept full inventories
        if (claimConfig.isEnabled() && claimConfig.storeIfInventoryFull()
                && reward instanceof UnifiedReward unified
                && !unified.getItems().isEmpty()
                && !hasInventorySpace(player, unified.getItems())) {

            // Store in claims instead of delivering
            claimService.addClaim(player, reward, crateId, ClaimReason.INVENTORY_FULL);
            
            audit.info(com.pumpkings.pkcrates.infrastructure.audit.api.AuditEvent.CRATE_OPENED, player.getName(), crateId, 
                java.util.Map.of("world", player.getWorld().getName(), "reward", reward.getId()));

            if (claimConfig.notifyOnStore()) {
                messageManager.sendMessage(player, Messages.CLAIM_STORED_NOTIFICATION);
            }
            return;
        }

        // Normal delivery
        try {
            reward.give(player);
            
            audit.info(com.pumpkings.pkcrates.infrastructure.audit.api.AuditEvent.CRATE_OPENED, player.getName(), crateId, 
                java.util.Map.of("world", player.getWorld().getName()));
            audit.success(com.pumpkings.pkcrates.infrastructure.audit.api.AuditEvent.REWARD_WON, player.getName(), reward.getId(), 
                java.util.Map.of("crate", crateId));
                
            String rewardName = reward.getId();
            
            // Personal Message
            messageManager.sendMessage(player, com.pumpkings.pkcrates.infrastructure.config.Messages.REWARD_WON, 
                java.util.Map.of("<reward>", rewardName, "<crate>", session.getCrate().getName()));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            

            // Global Broadcast (Reward specific)
            if (reward.isBroadcastEnabled()) {
                for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                    messageManager.sendMessage(p, com.pumpkings.pkcrates.infrastructure.config.Messages.REWARD_WON_GLOBAL, 
                        java.util.Map.of("<player>", player.getName(), "<reward>", rewardName, "<crate>", session.getCrate().getName()));
                }
            }
                
            // Trigger Rarity Effects
            if (reward.getRarityId() != null) {
                com.pumpkings.pkcrates.api.rarity.RarityService rarityService = 
                    ((com.pumpkings.pkcrates.PkCratesPlugin) plugin).getRarityService();
                com.pumpkings.pkcrates.core.model.rarity.Rarity rarity = rarityService.get(reward.getRarityId());
                if (rarity != null) {
                    playRarityEffects(player, rarity);
                    if (rarity.isBroadcastEnabled() && rarity.getAnnouncementTemplate() != null && !rarity.getAnnouncementTemplate().isEmpty()) {
                        String msg = rarity.getAnnouncementTemplate()
                            .replace("<player>", player.getName())
                            .replace("<crate>", session.getCrate().getName())
                            .replace("<reward>", reward.getId())
                            .replace("<rarity>", rarity.getDisplayName() != null ? rarity.getDisplayName() : rarity.getId());
                            
                        org.bukkit.Bukkit.getServer().sendMessage(
                            com.pumpkings.pkcrates.presentation.utils.TextUtil.parse(msg)
                        );
                    }
                }
            }
                
        } catch (Exception e) {
            plugin.getLogger().severe("Error delivering reward '" + reward.getId()
                    + "' to player '" + player.getName() + "': " + e.getMessage());

            // Store in claims on unexpected error if configured to do so
            if (claimConfig.isEnabled() && claimConfig.storeIfDeliveryFailed()) {
                claimService.addClaim(player, reward, crateId, ClaimReason.DELIVERY_ERROR);
                if (claimConfig.notifyOnStore()) {
                    messageManager.sendMessage(player, Messages.CLAIM_STORED_NOTIFICATION);
                }
            }
        }
    }

    private void playRarityEffects(Player player, com.pumpkings.pkcrates.core.model.rarity.Rarity rarity) {
        if (rarity.getSound() != null && !rarity.getSound().isEmpty()) {
            try {
                String[] split = rarity.getSound().split(";");
                String soundName = split[0];
                float volume = split.length > 1 ? Float.parseFloat(split[1]) : 1.0f;
                float pitch = split.length > 2 ? Float.parseFloat(split[2]) : 1.0f;
                player.playSound(player.getLocation(), org.bukkit.Sound.valueOf(soundName.toUpperCase()), volume, pitch);
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid sound format in rarity '" + rarity.getId() + "': " + rarity.getSound());
            }
        }
        
        if (rarity.getParticle() != null && !rarity.getParticle().isEmpty()) {
            try {
                org.bukkit.Particle particle = org.bukkit.Particle.valueOf(rarity.getParticle().toUpperCase());
                player.spawnParticle(particle, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid particle name in rarity '" + rarity.getId() + "': " + rarity.getParticle());
            }
        }
        
        if (rarity.getFireworkColor() != null && !rarity.getFireworkColor().isEmpty()) {
            try {
                String hex = rarity.getFireworkColor();
                if (hex.startsWith("#")) {
                    hex = hex.substring(1);
                }
                int rgb = Integer.parseInt(hex, 16);
                org.bukkit.Color color = org.bukkit.Color.fromRGB(rgb);
                
                org.bukkit.entity.Firework fw = player.getWorld().spawn(player.getLocation(), org.bukkit.entity.Firework.class);
                org.bukkit.inventory.meta.FireworkMeta fm = fw.getFireworkMeta();
                fm.addEffect(org.bukkit.FireworkEffect.builder()
                        .flicker(true)
                        .trail(true)
                        .with(org.bukkit.FireworkEffect.Type.BALL_LARGE)
                        .withColor(color)
                        .withFade(org.bukkit.Color.WHITE)
                        .build());
                fm.setPower(1);
                fw.setFireworkMeta(fm);
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid firework color in rarity '" + rarity.getId() + "': " + rarity.getFireworkColor());
            }
        }
    }

    /**
     * Returns {@code true} if the player's inventory can accept all the given items
     * without any of them overflowing.
     *
     * <p>Uses a trial {@link org.bukkit.inventory.Inventory#addItem} on a cloned
     * inventory simulation is not available in Bukkit, so we conservatively
     * check the number of empty slots against the number of distinct item stacks.</p>
     *
     * @param player The player whose inventory to inspect.
     * @param items  The items that need to fit.
     * @return {@code true} if all items can be added without leftovers.
     */
    private boolean hasInventorySpace(Player player, List<ItemStack> items) {
        // Count available inventory slots (null = empty)
        long freeSlots = 0;
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (slot == null || slot.getType() == org.bukkit.Material.AIR) {
                freeSlots++;
            }
        }
        // Conservative check: at minimum we need one slot per distinct item stack
        return freeSlots >= items.size();
    }
}
