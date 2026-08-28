package com.pumpkings.pkcrates.core.event;

import com.pumpkings.pkcrates.core.model.claim.ClaimReason;
import com.pumpkings.pkcrates.core.model.claim.ClaimedReward;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Fired when a reward is about to be stored in the claim system
 * instead of being delivered directly to a player.
 *
 * <p>If this event is cancelled, the reward will NOT be stored.</p>
 */
public class RewardStoredEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerUuid;
    private final ClaimedReward reward;
    private final ClaimReason reason;
    private boolean cancelled;

    /**
     * @param playerUuid The UUID of the player who should receive the reward.
     * @param reward     The snapshot about to be stored.
     * @param reason     Why it is being stored instead of delivered.
     */
    public RewardStoredEvent(UUID playerUuid, ClaimedReward reward, ClaimReason reason) {
        this.playerUuid = playerUuid;
        this.reward = reward;
        this.reason = reason;
        this.cancelled = false;
    }

    /** @return The UUID of the player who owns the reward. */
    public UUID getPlayerUuid() { return playerUuid; }

    /** @return The reward snapshot being stored. */
    public ClaimedReward getReward() { return reward; }

    /** @return The reason for storing instead of delivering. */
    public ClaimReason getReason() { return reason; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
