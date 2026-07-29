package com.pumpkings.pkcrates.core.event;

import com.pumpkings.pkcrates.core.model.Crate;
import com.pumpkings.pkcrates.core.model.reward.IReward;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MassOpeningRewardEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Crate crate;
    private final IReward reward;
    private final int rewardIndex;
    private boolean cancelled;

    public MassOpeningRewardEvent(Player player, Crate crate, IReward reward, int rewardIndex) {
        this.player = player;
        this.crate = crate;
        this.reward = reward;
        this.rewardIndex = rewardIndex;
        this.cancelled = false;
    }

    public Player getPlayer() {
        return player;
    }

    public Crate getCrate() {
        return crate;
    }

    public IReward getReward() {
        return reward;
    }

    public int getRewardIndex() {
        return rewardIndex;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
