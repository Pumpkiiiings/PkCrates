package com.pumpkings.pkcrates.api.event.rarity;

import com.pumpkings.pkcrates.core.model.rarity.Rarity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class RarityUpdateEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Rarity oldRarity;
    private final Rarity newRarity;
    private boolean cancelled;

    public RarityUpdateEvent(Rarity oldRarity, Rarity newRarity) {
        this.oldRarity = oldRarity;
        this.newRarity = newRarity;
    }

    public Rarity getOldRarity() {
        return oldRarity;
    }

    public Rarity getNewRarity() {
        return newRarity;
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
