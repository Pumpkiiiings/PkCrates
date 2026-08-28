package com.pumpkings.pkcrates.core.event;

import com.pumpkings.pkcrates.core.model.claim.ClaimedReward;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Collections;
import java.util.List;

/**
 * Fired when a player uses the "Claim All" action to attempt delivery
 * of all their pending rewards at once.
 *
 * <p>The list of rewards can be inspected and modified before delivery.
 * If cancelled, no rewards are delivered.</p>
 */
public class PlayerClaimAllEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    /** Mutable so listeners can filter out specific rewards. */
    private List<ClaimedReward> rewards;
    private boolean cancelled;

    /**
     * @param player  The online player claiming all rewards.
     * @param rewards The list of rewards that will be attempted.
     */
    public PlayerClaimAllEvent(Player player, List<ClaimedReward> rewards) {
        this.player = player;
        this.rewards = rewards;
        this.cancelled = false;
    }

    /** @return The player claiming all rewards. */
    public Player getPlayer() { return player; }

    /**
     * Returns the mutable list of rewards to be delivered.
     * Listeners may remove entries to prevent their delivery.
     *
     * @return Mutable list of pending {@link ClaimedReward}.
     */
    public List<ClaimedReward> getRewards() { return rewards; }

    /** Replaces the entire reward list. */
    public void setRewards(List<ClaimedReward> rewards) {
        this.rewards = rewards == null ? Collections.emptyList() : rewards;
    }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
