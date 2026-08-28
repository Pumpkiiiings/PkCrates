package com.pumpkings.pkcrates.core.model.session;

import com.pumpkings.pkcrates.core.model.Crate;
import com.pumpkings.pkcrates.core.model.reward.IReward;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class OpeningSession {

    private final Player player;
    private final Crate crate;
    private final Location blockLocation;
    
    private SessionState state;
    private IReward winningReward;
    
    // AnimationHandler will be injected here later
    
    public OpeningSession(Player player, Crate crate, Location blockLocation) {
        this.player = player;
        this.crate = crate;
        this.blockLocation = blockLocation;
        this.state = SessionState.STARTING;
    }

    public void start() {
        // On session start, RNG already decides which reward the player will win.
        // Animations are just visual theater (exactly like PhoenixCrates does).
        this.winningReward = crate.pickRandomReward();
        
        if (this.winningReward == null) {
            com.pumpkings.pkcrates.PkCratesPlugin plugin = com.pumpkings.pkcrates.PkCratesPlugin.getPlugin(com.pumpkings.pkcrates.PkCratesPlugin.class);
            plugin.getMessageManager().sendMessage(player, com.pumpkings.pkcrates.infrastructure.config.Messages.CRATE_NO_REWARDS);
            this.state = SessionState.FINISHED;
            return;
        }

        // Temporary logic to test without animations
        this.state = SessionState.PRE_OPEN;
        // ... (AnimationHandler will take control here)
    }

    public void finish() {
        this.state = SessionState.FINISHED;
        if (this.winningReward != null) {
            this.winningReward.give(player);
        }
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

    public SessionState getState() {
        return state;
    }

    public void setState(SessionState state) {
        this.state = state;
    }

    public IReward getWinningReward() {
        return winningReward;
    }
}
