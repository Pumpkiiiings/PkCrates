package com.pumpkings.pkcrates.core.model.session;

import com.pumpkings.pkcrates.core.model.Crate;
import com.pumpkings.pkcrates.core.model.reward.IReward;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class CrateSession {

    private final Player player;
    private final Crate crate;
    private final Location blockLocation;
    private final IReward wonReward;
    private boolean isFinished;
    private int ticksLived;

    public CrateSession(Player player, Crate crate, Location blockLocation, IReward wonReward) {
        this.player = player;
        this.crate = crate;
        this.blockLocation = blockLocation;
        this.wonReward = wonReward;
        this.isFinished = false;
        this.ticksLived = 0;
    }

    public Player getPlayer() {
        return player;
    }

    public Crate getCrate() {
        return crate;
    }

    public Location getBlockLocation() {
        return blockLocation;
    }

    public IReward getWonReward() {
        return wonReward;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean finished) {
        isFinished = finished;
    }

    public int getTicksLived() {
        return ticksLived;
    }

    public void incrementTicks() {
        this.ticksLived++;
    }
}

