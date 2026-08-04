package com.pumpkings.pkcrates.core.model;

import com.pumpkings.pkcrates.core.effect.EffectSpec;
import com.pumpkings.pkcrates.core.effect.EffectTrigger;
import com.pumpkings.pkcrates.core.model.reward.IReward;
import com.pumpkings.pkcrates.infrastructure.config.MassOpeningConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    /**
     * Per-trigger effect lines exactly as written in the crate file.
     *
     * <p>Stored raw rather than as parsed specs so that saving the crate — which rewrites
     * the whole file from this model whenever the GUI editor touches it — writes back what
     * the operator wrote. Keeping only parsed objects would silently drop their formatting,
     * and any line the parser rejected would vanish from the file entirely.</p>
     */
    private final Map<String, List<String>> effectLines = new LinkedHashMap<>();

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

    /**
     * Parsed effects, built lazily from {@link #effectLines} on first use.
     *
     * <p>Not persisted and not part of the crate's identity — cleared whenever the raw
     * lines change so the two can never disagree.</p>
     */
    private final transient Map<EffectTrigger, List<EffectSpec>> compiledEffects = new EnumMap<>(EffectTrigger.class);

    public String getId() {
        return id;
    }

    /**
     * Replaces the effect lines for one trigger.
     *
     * @param configKey Trigger key as written in the file, e.g. {@code on-open}.
     * @param lines     Raw effect lines; an empty list removes the override.
     */
    public void setEffectLines(String configKey, List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            effectLines.remove(configKey);
        } else {
            effectLines.put(configKey, new ArrayList<>(lines));
        }
        compiledEffects.clear();
    }

    /**
     * @return The raw effect lines per trigger key, for writing back to the crate file.
     */
    public Map<String, List<String>> getEffectLines() {
        return Collections.unmodifiableMap(effectLines);
    }

    /**
     * @return {@code true} when this crate overrides the global effects for the trigger.
     */
    public boolean hasEffects(EffectTrigger trigger) {
        List<String> lines = effectLines.get(trigger.getConfigKey());
        return lines != null && !lines.isEmpty();
    }

    /**
     * Returns this crate's compiled effects for a trigger, compiling on first request.
     *
     * @param trigger  The trigger to look up.
     * @param compiler Turns raw lines into specs; supplied by the caller so the model stays
     *                 free of plugin and logging dependencies.
     * @return The specs, or {@code null} when this crate does not override the trigger.
     */
    public List<EffectSpec> getEffects(EffectTrigger trigger, EffectCompiler compiler) {
        if (!hasEffects(trigger)) return null;
        return compiledEffects.computeIfAbsent(trigger,
                key -> compiler.compile(effectLines.get(key.getConfigKey()),
                        "crate '" + id + "' effects." + key.getConfigKey()));
    }

    /** Compiles raw effect lines; implemented by the effect engine. */
    @FunctionalInterface
    public interface EffectCompiler {
        List<EffectSpec> compile(List<String> lines, String source);
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
