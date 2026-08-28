package com.pumpkings.pkcrates.api.event.rarity;

import com.pumpkings.pkcrates.core.model.key.IKey;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class KeyRarityChangeEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final IKey key;
    private final String oldRarityId;
    private final String newRarityId;
    private boolean cancelled;

    public KeyRarityChangeEvent(IKey key, String oldRarityId, String newRarityId) {
        this.key = key;
        this.oldRarityId = oldRarityId;
        this.newRarityId = newRarityId;
    }

    public IKey getKey() {
        return key;
    }

    public String getOldRarityId() {
        return oldRarityId;
    }

    public String getNewRarityId() {
        return newRarityId;
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
