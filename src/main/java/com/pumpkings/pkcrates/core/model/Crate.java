package com.pumpkings.pkcrates.core.model;

import com.pumpkings.pkcrates.core.model.reward.IReward;
import com.pumpkings.pkcrates.infrastructure.config.MassOpeningConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Crate {

    private final String id;
    private String name;
    private List<IReward> rewards;
    private List<String> acceptedKeys;
    private double totalWeight;
    private HologramConfig hologramConfig;
    private String animationId;
    private MassOpeningConfig massOpeningConfig;

    public Crate(String id, String name, List<IReward> rewards, List<String> acceptedKeys, HologramConfig hologramConfig, String animationId) {
        this.id = id;
        this.name = name;
        this.rewards = rewards == null ? new ArrayList<>() : new ArrayList<>(rewards);
        this.acceptedKeys = acceptedKeys == null ? new ArrayList<>() : new ArrayList<>(acceptedKeys);
        this.hologramConfig = hologramConfig;
        this.animationId = animationId;
        
        // Pre-calculate the total weight so the random generator is ultra fast O(N)
        this.totalWeight = this.rewards.stream().mapToDouble(IReward::getWeight).sum();
        this.massOpeningConfig = MassOpeningConfig.createDefault(true);
    }

    public String getId() {
        return id;
    }

    public HologramConfig getHologramConfig() {
        return hologramConfig;
    }

    public MassOpeningConfig getMassOpeningConfig() {
        if (massOpeningConfig == null) {
            massOpeningConfig = MassOpeningConfig.createDefault(true);
        }
        return massOpeningConfig;
    }

    public void setMassOpeningConfig(MassOpeningConfig massOpeningConfig) {
        this.massOpeningConfig = massOpeningConfig;
    }

    public void setHologramConfig(HologramConfig hologramConfig) {
        this.hologramConfig = hologramConfig;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAnimationId() {
        return animationId;
    }

    public void setAnimationId(String animationId) {
        this.animationId = animationId;
    }

    public List<IReward> getRewards() {
        return new ArrayList<>(rewards);
    }

    public void addReward(IReward reward) {
        this.rewards.add(reward);
        recalculateWeight();
    }

    public void removeReward(IReward reward) {
        this.rewards.remove(reward);
        recalculateWeight();
    }

    private void recalculateWeight() {
        this.totalWeight = this.rewards.stream().mapToDouble(IReward::getWeight).sum();
    }

    public List<String> getAcceptedKeys() {
        return new ArrayList<>(acceptedKeys);
    }

    public void addAcceptedKey(String keyId) {
        if (!this.acceptedKeys.contains(keyId)) {
            this.acceptedKeys.add(keyId);
        }
    }

    public void removeAcceptedKey(String keyId) {
        this.acceptedKeys.remove(keyId);
    }

    /**
     * Selects a random reward based on the weight of each one.
     * @return IReward chosen, or null if the crate is empty.
     */
    public IReward pickRandomReward() {
        if (rewards.isEmpty() || totalWeight <= 0) {
            return null;
        }

        double randomValue = ThreadLocalRandom.current().nextDouble() * totalWeight;
        double currentWeight = 0;

        for (IReward reward : rewards) {
            currentWeight += reward.getWeight();
            if (randomValue <= currentWeight) {
                return reward;
            }
        }

        // Mathematical fallback (should never reach here if totalWeight was calculated correctly)
        return rewards.get(rewards.size() - 1);
    }
}
