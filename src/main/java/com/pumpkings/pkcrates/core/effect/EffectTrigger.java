package com.pumpkings.pkcrates.core.effect;

import java.util.Locale;

/**
 * Points in a crate opening at which a configured effect bundle fires.
 */
public enum EffectTrigger {

    /** The moment a key is consumed and the animation begins. */
    ON_OPEN("on-open"),

    /** The reward has been decided and handed over. */
    ON_REWARD("on-reward"),

    /** The player could not receive the reward and it went to the claim system. */
    ON_CLAIM_STORED("on-claim-stored");

    private final String configKey;

    EffectTrigger(String configKey) {
        this.configKey = configKey;
    }

    /** @return The key this trigger reads from in {@code config.yml} and crate files. */
    public String getConfigKey() {
        return configKey;
    }

    /**
     * @param key Config key, e.g. {@code on-open}.
     * @return The matching trigger, or {@code null} when unknown.
     */
    public static EffectTrigger fromConfigKey(String key) {
        if (key == null) return null;
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        for (EffectTrigger trigger : values()) {
            if (trigger.configKey.equals(normalized)) return trigger;
        }
        return null;
    }
}
