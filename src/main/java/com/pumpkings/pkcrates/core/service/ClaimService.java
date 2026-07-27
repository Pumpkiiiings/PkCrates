package com.pumpkings.pkcrates.core.service;

import com.pumpkings.pkcrates.core.model.claim.ClaimReason;
import com.pumpkings.pkcrates.core.model.claim.ClaimedReward;
import com.pumpkings.pkcrates.core.model.claim.ClaimResult;
import com.pumpkings.pkcrates.core.model.reward.IReward;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * Primary port for the Crate Claim system.
 *
 * <p>All claim-related business operations pass through this interface.
 * Implementations must be framework-agnostic — they receive Bukkit
 * {@link Player} objects solely for inventory interaction and event dispatch,
 * not as a persistence key.</p>
 *
 * <p>Thread-safety: implementations must be safe to call from the main
 * Bukkit thread. Any async work (database I/O) is the responsibility of
 * the implementing class.</p>
 */
public interface ClaimService {

    /**
     * Stores a reward in the claim system without delivering it to the player.
     *
     * <p>This method resolves the effective storage limit from the player's
     * permissions using {@code pkcrates.claim.limit.*} nodes.  If the player
     * is offline, the global {@code claim.limits.maximum-stored} value from
     * {@code config.yml} is used as the fallback.</p>
     *
     * <p>This method should fire a {@code RewardStoredEvent} before persisting.
     * If the event is cancelled, the reward must NOT be stored.</p>
     *
     * @param player  The online player who should receive the reward.
     * @param reward  The {@link IReward} to snapshot and store.
     * @param crateId ID of the crate that generated the reward.
     * @param reason  Why the reward is being stored rather than delivered.
     */
    void addClaim(Player player, IReward reward, String crateId, ClaimReason reason);

    /**
     * Stores a reward for an offline player using the global config limit as fallback.
     *
     * @param playerUuid UUID of the player who should receive the reward.
     * @param reward     The {@link IReward} to snapshot and store.
     * @param crateId    ID of the crate that generated the reward.
     * @param reason     Why the reward is being stored rather than delivered.
     */
    void addClaim(UUID playerUuid, IReward reward, String crateId, ClaimReason reason);

    /**
     * Removes a specific claim entry permanently.
     *
     * <p>Does not attempt delivery — use {@link #claim} for that.</p>
     *
     * @param playerUuid UUID of the owning player.
     * @param claimId    UUID of the specific claim to remove.
     * @return {@code true} if the claim existed and was removed.
     */
    boolean removeClaim(UUID playerUuid, UUID claimId);

    /**
     * Attempts to deliver a single pending reward to an online player.
     *
     * <p>If delivery succeeds, the claim is removed from the repository.
     * If delivery fails (e.g., inventory full), the claim is kept and
     * a {@code RewardClaimFailedEvent} is fired.</p>
     *
     * @param player  The online player claiming the reward.
     * @param claimId UUID of the specific claim to deliver.
     * @return A {@link ClaimResult} describing the outcome.
     */
    ClaimResult claim(Player player, UUID claimId);

    /**
     * Attempts to deliver all pending rewards to an online player.
     *
     * <p>Claims that cannot be delivered (e.g., inventory full mid-way)
     * remain in the repository. Fires {@code PlayerClaimAllEvent} before
     * any delivery is attempted.</p>
     *
     * @param player The online player claiming all rewards.
     * @return A list of {@link ClaimResult}, one per attempted claim.
     */
    List<ClaimResult> claimAll(Player player);

    /**
     * Returns all pending claims for the given player.
     *
     * @param playerUuid UUID of the player to query.
     * @return Immutable list of pending {@link ClaimedReward} entries, newest first.
     */
    List<ClaimedReward> getClaims(UUID playerUuid);

    /**
     * Returns {@code true} if the player has at least one pending claim.
     *
     * @param playerUuid UUID of the player to check.
     * @return {@code true} if there is at least one pending claim.
     */
    boolean hasClaims(UUID playerUuid);

    /**
     * Wipes all pending claims for a specific offline/online player.
     * @param playerUuid The UUID of the player.
     * @return The number of claims deleted.
     */
    int clearClaims(UUID playerUuid);

    /**
     * Wipes ALL pending claims for ALL players across the entire server.
     * Use with extreme caution.
     * @return The number of claims deleted.
     */
    int clearAllClaims();

    /**
     * Returns the number of pending claims for the given player.
     *
     * @param playerUuid UUID of the player to check.
     * @return The count of pending claims, or 0 if none.
     */
    int getPendingAmount(UUID playerUuid);
    /**
     * Returns the effective maximum number of claims the player may store,
     * resolved from their permissions (or the global config fallback).
     *
     * @param player The online player to evaluate.
     * @return The effective limit, or {@link com.pumpkings.pkcrates.infrastructure.permission.PermissionLimitResolver#UNLIMITED}
     *         ({@code -1}) if no cap applies.
     */
    int getEffectiveLimit(Player player);
}
