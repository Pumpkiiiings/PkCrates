package com.pumpkings.pkcrates.core.event;

import com.pumpkings.pkcrates.core.model.claim.ClaimedReward;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a player attempts to claim a single pending reward.
 *
 * <p>If cancelled, the reward remains stored and is not delivered.</p>
 */
public class PlayerClaimRewardEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ClaimedReward reward;
    private boolean cancelled;

    /**
     * @param player The online player attempting to claim.
     * @param reward The specific reward being claimed.
     */
    public PlayerClaimRewardEvent(Player player, ClaimedReward reward) {
        this.player = player;
        this.reward = reward;
        this.cancelled = false;
    }

    /** @return The player claiming the reward. */
    public Player getPlayer() { return player; }

    /** @return The reward being claimed. */
    public ClaimedReward getReward() { return reward; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
