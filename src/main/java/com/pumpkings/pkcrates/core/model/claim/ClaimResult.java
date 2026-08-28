package com.pumpkings.pkcrates.core.model.claim;

import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

/**
 * Represents the outcome of a single claim attempt.
 *
 * <p>Used as the return value of {@link com.pumpkings.pkcrates.core.service.ClaimService#claim}
 * and {@link com.pumpkings.pkcrates.core.service.ClaimService#claimAll}
 * so callers can react to partial failures without throwing exceptions.</p>
 */
public final class ClaimResult {

    /** Whether the delivery was fully successful. */
    private final boolean success;

    /** The claim that was attempted. */
    private final ClaimedReward reward;

    /**
     * Items that could not be placed in the player's inventory.
     * Non-empty only when {@code success} is {@code false} due to full inventory.
     */
    private final List<ItemStack> failedItems;

    /** Human-readable reason for failure (empty string on success). */
    private final String failureReason;

    private ClaimResult(boolean success, ClaimedReward reward,
                        List<ItemStack> failedItems, String failureReason) {
        this.success = success;
        this.reward = reward;
        this.failedItems = failedItems == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(failedItems);
        this.failureReason = failureReason == null ? "" : failureReason;
    }

    // -------------------------------------------------------------------------
    // Factories
    // -------------------------------------------------------------------------

    /**
     * Creates a successful result.
     *
     * @param reward The reward that was successfully delivered.
     * @return A successful {@code ClaimResult}.
     */
    public static ClaimResult success(ClaimedReward reward) {
        return new ClaimResult(true, reward, null, "");
    }

    /**
     * Creates a failed result.
     *
     * @param reward       The reward that could not be delivered.
     * @param failedItems  Items that were not delivered (e.g., due to full inventory).
     * @param reason       Short human-readable explanation.
     * @return A failed {@code ClaimResult}.
     */
    public static ClaimResult failure(ClaimedReward reward,
                                     List<ItemStack> failedItems,
                                     String reason) {
        return new ClaimResult(false, reward, failedItems, reason);
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /** @return {@code true} if the reward was fully delivered. */
    public boolean isSuccess() { return success; }

    /** @return The {@link ClaimedReward} that was attempted. */
    public ClaimedReward getReward() { return reward; }

    /** @return Items that could not be delivered, or an empty list on success. */
    public List<ItemStack> getFailedItems() { return failedItems; }

    /** @return Reason for failure, or an empty string on success. */
    public String getFailureReason() { return failureReason; }
}
