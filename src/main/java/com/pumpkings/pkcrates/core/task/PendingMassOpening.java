package com.pumpkings.pkcrates.core.task;

import com.pumpkings.pkcrates.core.model.Crate;
import com.pumpkings.pkcrates.core.model.reward.IReward;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PendingMassOpening {

    private final Player player;
    private final Crate crate;
    private final List<IReward> rewards;
    private int currentIndex;
    private boolean finished;

    public PendingMassOpening(Player player, Crate crate, List<IReward> rewards) {
        this.player = player;
        this.crate = crate;
        this.rewards = rewards == null ? new ArrayList<>() : new ArrayList<>(rewards);
        this.currentIndex = 0;
        this.finished = false;
    }

    public Player getPlayer() {
        return player;
    }

    public Crate getCrate() {
        return crate;
    }

    public List<IReward> getRewards() {
        return rewards;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void incrementIndex(int amount) {
        this.currentIndex += amount;
        if (this.currentIndex >= this.rewards.size()) {
            this.finished = true;
        }
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }
}
