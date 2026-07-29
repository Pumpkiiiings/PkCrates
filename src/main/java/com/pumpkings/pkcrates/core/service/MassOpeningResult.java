package com.pumpkings.pkcrates.core.service;

import com.pumpkings.pkcrates.core.model.reward.IReward;

import java.util.ArrayList;
import java.util.List;

public class MassOpeningResult {

    private final int requestedCount;
    private final int openedCount;
    private final List<IReward> rewards;
    private final long durationMillis;
    private final boolean cancelled;
    private final String cancelReason;

    public MassOpeningResult(int requestedCount, int openedCount, List<IReward> rewards, long durationMillis, boolean cancelled, String cancelReason) {
        this.requestedCount = requestedCount;
        this.openedCount = openedCount;
        this.rewards = rewards == null ? new ArrayList<>() : new ArrayList<>(rewards);
        this.durationMillis = durationMillis;
        this.cancelled = cancelled;
        this.cancelReason = cancelReason;
    }

    public int getRequestedCount() {
        return requestedCount;
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

    public boolean isCancelled() {
        return cancelled;
    }

    public String getCancelReason() {
        return cancelReason;
    }
}
