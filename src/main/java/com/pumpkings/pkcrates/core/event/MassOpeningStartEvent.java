package com.pumpkings.pkcrates.core.event;

import com.pumpkings.pkcrates.core.model.Crate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MassOpeningStartEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Crate crate;
    private final int requestedAmount;
    private int effectiveAmount;
    private boolean cancelled;

    public MassOpeningStartEvent(Player player, Crate crate, int requestedAmount, int effectiveAmount) {
        this.player = player;
        this.crate = crate;
        this.requestedAmount = requestedAmount;
        this.effectiveAmount = effectiveAmount;
        this.cancelled = false;
    }

    public Player getPlayer() {
        return player;
    }

    public Crate getCrate() {
        return crate;
    }

    public int getRequestedAmount() {
        return requestedAmount;
    }

    public int getEffectiveAmount() {
        return effectiveAmount;
    }

    public void setEffectiveAmount(int effectiveAmount) {
        this.effectiveAmount = effectiveAmount;
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
