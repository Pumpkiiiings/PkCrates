package com.pumpkings.pkcrates.core.event;

import com.pumpkings.pkcrates.core.model.Crate;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MassOpeningCancelEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Crate crate;
    private final int cancelledAt;
    private final String reason;

    public MassOpeningCancelEvent(Player player, Crate crate, int cancelledAt, String reason) {
        this.player = player;
        this.crate = crate;
        this.cancelledAt = cancelledAt;
        this.reason = reason;
    }

    public Player getPlayer() {
        return player;
    }

    public Crate getCrate() {
        return crate;
    }

    public int getCancelledAt() {
        return cancelledAt;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
