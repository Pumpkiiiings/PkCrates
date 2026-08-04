package com.pumpkings.pkcrates.core.task;

import com.pumpkings.pkcrates.core.model.Crate;
import com.pumpkings.pkcrates.core.model.reward.IReward;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * A batch of rewards queued for tick-paced delivery to one player.
 *
 * <p>Only the player's UUID is retained. Holding the {@link Player} object across ticks
 * would pin a disconnected player's entity in memory and would go stale if the player
 * reconnects mid-batch.</p>
 */
public class PendingMassOpening {

    private final UUID playerUuid;
    private final String playerName;
    private final Crate crate;
    private final List<IReward> rewards;
    private int currentIndex;
    private boolean finished;

    public PendingMassOpening(Player player, Crate crate, List<IReward> rewards) {
        this.playerUuid = player.getUniqueId();
        this.playerName = player.getName();
        this.crate = crate;
        this.rewards = rewards == null ? new ArrayList<>() : new ArrayList<>(rewards);
        this.currentIndex = 0;
        this.finished = false;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    /** Name captured when the batch was queued; used for logging after disconnect. */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * @return The player if currently online, otherwise {@code null}.
     */
    public @Nullable Player getPlayer() {
        return Bukkit.getPlayer(playerUuid);
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

    /**
     * @return The rewards not yet delivered, in order. Empty when the batch is done.
     */
    public List<IReward> getRemainingRewards() {
        if (currentIndex >= rewards.size()) return Collections.emptyList();
        return List.copyOf(rewards.subList(currentIndex, rewards.size()));
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
