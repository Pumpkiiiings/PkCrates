package com.pumpkings.pkcrates.core.event;

import com.pumpkings.pkcrates.core.model.claim.ClaimedReward;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a claim delivery attempt fails.
 *
 * <p>This event is informational — it cannot be cancelled.
 * Handlers can use it for logging, metrics, or notifying the player.</p>
 *
 * <p>The reward remains in the claim repository after this event fires.</p>
 */
public class RewardClaimFailedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ClaimedReward reward;
    private final String reason;

    /**
     * @param player The online player whose claim attempt failed.
     * @param reward The reward that could not be delivered.
     * @param reason A short human-readable explanation (e.g., {@code "inventory-full"}).
     */
    public RewardClaimFailedEvent(Player player, ClaimedReward reward, String reason) {
        this.player = player;
        this.reward = reward;
        this.reason = reason == null ? "" : reason;
    }

    /** @return The player whose delivery failed. */
    public Player getPlayer() { return player; }

    /** @return The reward that could not be delivered. */
    public ClaimedReward getReward() { return reward; }

    /** @return Short reason string (e.g., {@code "inventory-full"}, {@code "Cancelled by plugin."}). */
    public String getReason() { return reason; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
