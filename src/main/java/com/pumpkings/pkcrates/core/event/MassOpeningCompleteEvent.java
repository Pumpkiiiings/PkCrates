package com.pumpkings.pkcrates.core.event;

import com.pumpkings.pkcrates.core.model.Crate;
import com.pumpkings.pkcrates.core.model.reward.IReward;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.ArrayList;
import java.util.List;

public class MassOpeningCompleteEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Crate crate;
    private final int openedCount;
    private final List<IReward> rewards;
    private final long durationMillis;

    public MassOpeningCompleteEvent(Player player, Crate crate, int openedCount, List<IReward> rewards, long durationMillis) {
        this.player = player;
        this.crate = crate;
        this.openedCount = openedCount;
        this.rewards = rewards == null ? new ArrayList<>() : new ArrayList<>(rewards);
        this.durationMillis = durationMillis;
    }

    public Player getPlayer() {
        return player;
    }

    public Crate getCrate() {
        return crate;
    }

    public int getOpenedCount() {
        return openedCount;
    }

    public List<IReward> getRewards() {
        return new ArrayList<>(rewards);
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
