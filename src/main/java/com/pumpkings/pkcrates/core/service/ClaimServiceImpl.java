package com.pumpkings.pkcrates.core.service;

import com.pumpkings.pkcrates.core.event.PlayerClaimAllEvent;
import com.pumpkings.pkcrates.core.event.PlayerClaimRewardEvent;
import com.pumpkings.pkcrates.core.event.RewardClaimFailedEvent;
import com.pumpkings.pkcrates.core.event.RewardStoredEvent;
import com.pumpkings.pkcrates.core.model.claim.ClaimReason;
import com.pumpkings.pkcrates.core.model.claim.ClaimResult;
import com.pumpkings.pkcrates.core.model.claim.ClaimedReward;
import com.pumpkings.pkcrates.core.model.reward.IReward;
import com.pumpkings.pkcrates.core.model.reward.UnifiedReward;
import com.pumpkings.pkcrates.infrastructure.claim.ClaimConfig;
import com.pumpkings.pkcrates.infrastructure.claim.ClaimRepository;
import com.pumpkings.pkcrates.infrastructure.permission.PermissionLimitResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Concrete implementation of {@link ClaimService}.
 *
 * <h3>Limit resolution order</h3>
 * <ol>
 *   <li>{@code pkcrates.admin} or {@code pkcrates.claim.limit.unlimited} → unlimited.</li>
 *   <li>Highest {@code pkcrates.claim.limit.<N>} permission held by the online player.</li>
 *   <li>Global {@code claim.limits.maximum-stored} from {@code config.yml} (offline fallback).</li>
 * </ol>
 *
 * <p>All public methods run on the Bukkit main thread.
 * Repository I/O for YAML is synchronous by design; a future SQLite adapter
 * may wrap calls in async tasks without changing this class.</p>
 */
public class ClaimServiceImpl implements ClaimService {

    /** Permission prefix scanned for numeric limit nodes. */
    private static final String LIMIT_PREFIX = "pkcrates.claim.limit.";

    /** Main inventory + hotbar; matches the length of {@code getStorageContents()}. */
    private static final int PLAYER_STORAGE_SLOTS = 36;

    private final Plugin plugin;
    private final ClaimRepository repository;
    private final ClaimConfig config;
    private final PermissionLimitResolver limitResolver;
    private final Logger logger;

    /**
     * @param plugin     Owning plugin (event dispatch, logging).
     * @param repository Persistence adapter.
     * @param config     Claim module configuration (provides offline fallback limit).
     */
    public ClaimServiceImpl(Plugin plugin, ClaimRepository repository, ClaimConfig config) {
        this.plugin = plugin;
        this.repository = repository;
        this.config = config;
        this.logger = plugin.getLogger();

        // Build the permission-based limit resolver once — reused for every addClaim call
        this.limitResolver = PermissionLimitResolver.builder(LIMIT_PREFIX)
                .unlimitedPermission("pkcrates.claim.limit.unlimited")
                .unlimitedPermission("pkcrates.admin")
                .defaultLimit(config.getMaxStored()) // global fallback from config.yml
                .build();
    }

    // -------------------------------------------------------------------------
    // ClaimService — online player overload (permission-aware)
    // -------------------------------------------------------------------------

    @Override
    public void addClaim(Player player, IReward reward, String crateId, ClaimReason reason) {
        if (!config.isEnabled()) return;

        int pending = getPendingAmount(player.getUniqueId());

        if (!limitResolver.hasCapacity(player, pending)) {
            int limit = limitResolver.resolve(player);
            logger.warning("Player " + player.getName() + " has reached their claim limit ("
                    + limit + "). Reward '" + reward.getId() + "' from crate '"
                    + crateId + "' will not be stored.");
            return;
        }

        persistClaim(player.getUniqueId(), reward, crateId, reason);
    }

    // -------------------------------------------------------------------------
    // ClaimService — offline UUID overload (config-limit fallback)
    // -------------------------------------------------------------------------

    @Override
    public void addClaim(UUID playerUuid, IReward reward, String crateId, ClaimReason reason) {
        if (!config.isEnabled()) return;

        // For offline players we use the global config limit as the fallback
        int max = config.getMaxStored();
        int pending = getPendingAmount(playerUuid);

        if (max != PermissionLimitResolver.UNLIMITED && pending >= max) {
            logger.warning("Offline player " + playerUuid + " has reached the claim limit ("
                    + max + "). Reward '" + reward.getId() + "' from crate '"
                    + crateId + "' will not be stored.");
            return;
        }

        persistClaim(playerUuid, reward, crateId, reason);
    }

    // -------------------------------------------------------------------------
    // ClaimService — remaining methods
    // -------------------------------------------------------------------------

    @Override
    public boolean removeClaim(UUID playerUuid, UUID claimId) {
        List<ClaimedReward> claims = repository.findByPlayer(playerUuid);
        boolean exists = claims.stream().anyMatch(c -> c.getId().equals(claimId));
        if (exists) {
            repository.delete(playerUuid, claimId);
        }
        return exists;
    }

    @Override
    public ClaimResult claim(Player player, UUID claimId) {
        List<ClaimedReward> claims = repository.findByPlayer(player.getUniqueId());
        ClaimedReward target = claims.stream()
                .filter(c -> c.getId().equals(claimId))
                .findFirst()
                .orElse(null);

        if (target == null) {
            return ClaimResult.failure(null, Collections.emptyList(), "Claim not found.");
        }

        // Fire PlayerClaimRewardEvent — cancellable
        PlayerClaimRewardEvent event = new PlayerClaimRewardEvent(player, target);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return ClaimResult.failure(target, Collections.emptyList(), "Cancelled by plugin.");
        }

        ClaimResult result = deliver(player, target);

        if (result.isSuccess()) {
            repository.delete(player.getUniqueId(), claimId);
            
            ((com.pumpkings.pkcrates.PkCratesPlugin) plugin).getAuditService()
                .success(com.pumpkings.pkcrates.infrastructure.audit.api.AuditEvent.CLAIM_CLAIMED, player.getName(), target.getRewardId(), 
                Map.of("crate", target.getCrateId()));
                
        } else {
            RewardClaimFailedEvent failedEvent = new RewardClaimFailedEvent(
                    player, target, result.getFailureReason());
            plugin.getServer().getPluginManager().callEvent(failedEvent);
        }

        return result;
    }

    @Override
    public List<ClaimResult> claimAll(Player player) {
        List<ClaimedReward> claims = getClaims(player.getUniqueId());
        if (claims.isEmpty()) return Collections.emptyList();

        PlayerClaimAllEvent event = new PlayerClaimAllEvent(player, new ArrayList<>(claims));
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return Collections.emptyList();

        List<ClaimResult> results = new ArrayList<>();
        for (ClaimedReward claimed : event.getRewards()) {
            ClaimResult result = deliver(player, claimed);
            results.add(result);
            if (result.isSuccess()) {
                repository.delete(player.getUniqueId(), claimed.getId());
                
                ((com.pumpkings.pkcrates.PkCratesPlugin) plugin).getAuditService()
                    .success(com.pumpkings.pkcrates.infrastructure.audit.api.AuditEvent.CLAIM_CLAIMED, player.getName(), claimed.getRewardId(), 
                    Map.of("crate", claimed.getCrateId()));
                    
            } else {
                RewardClaimFailedEvent failedEvent = new RewardClaimFailedEvent(
                        player, claimed, result.getFailureReason());
                plugin.getServer().getPluginManager().callEvent(failedEvent);
            }
        }

        return Collections.unmodifiableList(results);
    }

    @Override
    public List<ClaimedReward> getClaims(UUID playerUuid) {
        return repository.findByPlayer(playerUuid);
    }

    @Override
    public boolean hasClaims(UUID playerUuid) {
        return !repository.findByPlayer(playerUuid).isEmpty();
    }

    @Override
    public int clearClaims(UUID playerUuid) {
        int count = repository.findByPlayer(playerUuid).size();
        repository.deleteAll(playerUuid);
        return count;
    }

    @Override
    public int clearAllClaims() {
        // Without an explicit count method in the repo, we just return -1 or a dummy value.
        // It's mostly an admin command anyway.
        repository.deleteAllPlayers();
        return -1;
    }

    @Override
    public int getPendingAmount(UUID playerUuid) {
        return repository.findByPlayer(playerUuid).size();
    }

    @Override
    public int getEffectiveLimit(Player player) {
        return limitResolver.resolve(player);
    }

    /**
     * Exposes the configured {@link PermissionLimitResolver} so that other
     * components (e.g. the {@code ClaimMenu} info button) can display the
     * player's effective limit without duplicating the lookup logic.
     *
     * @return The claim limit resolver instance.
     */
    public PermissionLimitResolver getLimitResolver() {
        return limitResolver;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a {@link ClaimedReward} snapshot, fires {@link RewardStoredEvent},
     * and persists it if the event was not cancelled.
     */
    private void persistClaim(UUID playerUuid, IReward reward,
                               String crateId, ClaimReason reason) {
        List<ItemStack> items = new ArrayList<>();
        List<String> commands = new ArrayList<>();

        if (reward instanceof UnifiedReward unified) {
            items.addAll(unified.getItems());
            commands.addAll(unified.getCommands());
        }

        ClaimedReward claimed = ClaimedReward.create(
                playerUuid, crateId, reward.getId(),
                reward.getPreviewItem(), items, commands, reason);

        // Fire RewardStoredEvent — cancellable
        RewardStoredEvent event = new RewardStoredEvent(playerUuid, claimed, reason);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        repository.save(claimed);
        
        org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
        String actor = offlinePlayer.getName() != null ? offlinePlayer.getName() : playerUuid.toString();
        
        com.pumpkings.pkcrates.infrastructure.audit.api.AuditService audit = 
            ((com.pumpkings.pkcrates.PkCratesPlugin) plugin).getAuditService();
        audit.warning(com.pumpkings.pkcrates.infrastructure.audit.api.AuditEvent.CLAIM_STORED, actor, reward.getId(), 
            Map.of("crate", crateId, "reason", reason.name()));
    }

    /**
     * Attempts to deliver all items and execute all commands of a claimed reward.
     *
     * <p>Delivery is all-or-nothing. Capacity is verified against a throwaway copy of
     * the player's storage contents <em>before</em> anything is handed over, so a claim
     * can never be partially delivered while remaining in the repository — that would
     * let the player re-claim it and duplicate the items that already fit.</p>
     */
    private ClaimResult deliver(Player player, ClaimedReward claimed) {
        List<ItemStack> items = claimed.getItems();

        if (!items.isEmpty()) {
            List<ItemStack> leftovers = simulateDelivery(player, items);
            if (!leftovers.isEmpty()) {
                return ClaimResult.failure(claimed, leftovers, "inventory-full");
            }

            // Capacity confirmed — the real hand-over cannot leave anything behind.
            for (ItemStack item : items) {
                if (item == null) continue;
                player.getInventory().addItem(item.clone());
            }
        }

        // Commands run only after all items have been placed successfully
        for (String command : claimed.getCommands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    command.replace("%player%", player.getName()));
        }

        return ClaimResult.success(claimed);
    }

    /**
     * Dry-runs the delivery against a detached inventory holding a snapshot of the
     * player's storage slots.
     *
     * @return The items that would not fit; empty when the whole batch fits.
     */
    private List<ItemStack> simulateDelivery(Player player, List<ItemStack> items) {
        Inventory probe = Bukkit.createInventory(null, PLAYER_STORAGE_SLOTS);
        probe.setContents(player.getInventory().getStorageContents());

        List<ItemStack> leftovers = new ArrayList<>();
        for (ItemStack item : items) {
            if (item == null) continue;
            leftovers.addAll(probe.addItem(item.clone()).values());
        }
        return leftovers;
    }
}
