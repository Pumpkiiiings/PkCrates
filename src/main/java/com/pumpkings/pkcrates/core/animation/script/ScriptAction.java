package com.pumpkings.pkcrates.core.animation.script;

import com.pumpkings.pkcrates.core.effect.EffectSpec;

import java.util.List;
import java.util.Map;

/** One validated action in a compiled animation timeline. */
public record ScriptAction(
        String source,
        int from,
        int to,
        int every,
        Object condition,
        Type type,
        Map<String, Object> arguments,
        List<EffectSpec> effects
) {
    public enum Type { EFFECT, SPAWN_DISPLAY, ANIMATE_DISPLAY, REMOVE_DISPLAY, TITLE }

    public boolean runsAt(int tick) {
        return tick >= from && tick <= to && (tick - from) % every == 0;
    }

    public double progress(int tick) {
        return to <= from ? 1.0 : Math.max(0.0, Math.min(1.0, (tick - from) / (double) (to - from)));
    }
}
