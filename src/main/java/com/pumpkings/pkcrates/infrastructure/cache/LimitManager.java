package com.pumpkings.pkcrates.infrastructure.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Anti-leak cache manager to track how many times a player has won a reward.
 */
public class LimitManager {

    private static LimitManager instance;

    // Key: "uuid_rewardId", Value: amount of times won
    // Expires after 24 hours to prevent infinite memory leak.
    private final Cache<String, Integer> winCache;

    private LimitManager() {
        this.winCache = CacheBuilder.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .maximumSize(50000) // Safe entry limit
                .build();
    }

    public static LimitManager getInstance() {
        if (instance == null) {
            instance = new LimitManager();
        }
        return instance;
    }

    /**
     * Returns how many times the player has won this specific reward in cache.
     */
    public int getWins(UUID playerId, String rewardId) {
        String key = playerId.toString() + "_" + rewardId;
        Integer wins = winCache.getIfPresent(key);
        return wins == null ? 0 : wins;
    }

    /**
     * Registers a win by adding 1 to the player's current counter for that reward.
     */
    public void addWin(UUID playerId, String rewardId) {
        String key = playerId.toString() + "_" + rewardId;
        int currentWins = getWins(playerId, rewardId);
        winCache.put(key, currentWins + 1);
    }
}
