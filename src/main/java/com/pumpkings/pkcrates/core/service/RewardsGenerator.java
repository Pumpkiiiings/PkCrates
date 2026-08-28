package com.pumpkings.pkcrates.core.service;

import com.pumpkings.pkcrates.api.rarity.RarityService;
import com.pumpkings.pkcrates.core.model.Crate;
import com.pumpkings.pkcrates.core.model.key.IKey;
import com.pumpkings.pkcrates.core.model.rarity.Rarity;
import com.pumpkings.pkcrates.core.model.rarity.RarityChanceMode;
import com.pumpkings.pkcrates.core.model.reward.IReward;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class RewardsGenerator {

    private static final Random RANDOM = new Random();

    /**
     * Calculates which reward the player wins using a weight/chance system.
     * @param crate The crate containing the rewards.
     * @param usedKey The key used to open the crate (can be null).
     * @param rarityService The rarity service to handle synced percentages and restrictions.
     * @return The winning reward, or null if the crate has no valid rewards.
     */
    public static IReward generateReward(Crate crate, IKey usedKey, RarityService rarityService) {
        List<IReward> rewards = crate.getRewards();
        if (rewards == null || rewards.isEmpty()) {
            return null;
        }

        // 1. Filter by key restrictions
        List<IReward> validRewards = new ArrayList<>();
        Map<String, Integer> rarityCounts = new HashMap<>(); // For synced percentage calculation
        
        for (IReward reward : rewards) {
            if (isValidWithKey(reward, usedKey, rarityService)) {
                validRewards.add(reward);
                
                String rId = reward.getRarityId();
                if (rId != null) {
                    rarityCounts.put(rId, rarityCounts.getOrDefault(rId, 0) + 1);
                }
            }
        }

        if (validRewards.isEmpty()) {
            return null; // No rewards match the key restriction
        }

        // 2. Calculate effective weights
        Map<IReward, Double> effectiveWeights = new HashMap<>();
        double totalWeight = 0.0;
        
        for (IReward reward : validRewards) {
            double eWeight = reward.getWeight();
            
            if (reward.getRarityId() != null && rarityService != null) {
                Rarity rarity = rarityService.get(reward.getRarityId());
                if (rarity != null && rarity.getChanceMode() == RarityChanceMode.SYNCED) {
                    int count = rarityCounts.get(reward.getRarityId());
                    eWeight = rarity.getWeight() / (double) count;
                }
            }
            
            effectiveWeights.put(reward, eWeight);
            totalWeight += eWeight;
        }

        if (totalWeight <= 0.0) {
            return validRewards.get(RANDOM.nextInt(validRewards.size()));
        }

        // 3. Roll
        double randomValue = RANDOM.nextDouble() * totalWeight;
        double accumulatedWeight = 0.0;

        for (IReward reward : validRewards) {
            accumulatedWeight += effectiveWeights.get(reward);
            if (randomValue <= accumulatedWeight) {
                return reward;
            }
        }

        return validRewards.get(validRewards.size() - 1);
    }
    
    private static boolean isValidWithKey(IReward reward, IKey key, RarityService rarityService) {
        if (key == null || rarityService == null) return true;
        if (key.getRarityRestriction() == null || key.getRarityRestriction() == IKey.RarityRestriction.ANY) return true;
        
        String reqTarget = key.getRarityTarget();
        Rarity rewardRarity = reward.getRarityId() != null ? rarityService.get(reward.getRarityId()) : null;
        
        switch (key.getRarityRestriction()) {
            case SPECIFIC_LIST:
                // Assuming target is comma-separated IDs
                if (rewardRarity == null) return false;
                if (reqTarget == null || reqTarget.isEmpty()) return true;
                for (String allowedId : reqTarget.split(",")) {
                    if (allowedId.trim().equalsIgnoreCase(rewardRarity.getId())) {
                        return true;
                    }
                }
                return false;
                
            case EXACT:
                if (rewardRarity == null) return false;
                return rewardRarity.getId().equalsIgnoreCase(reqTarget);
                
            case MINIMUM_PRIORITY:
                int targetPriority = 0;
                try {
                    targetPriority = Integer.parseInt(reqTarget);
                } catch (NumberFormatException ignored) {}
                
                if (rewardRarity == null) return targetPriority <= 0;
                return rewardRarity.getPriority() >= targetPriority;
                
            default:
                return true;
        }
    }
}
