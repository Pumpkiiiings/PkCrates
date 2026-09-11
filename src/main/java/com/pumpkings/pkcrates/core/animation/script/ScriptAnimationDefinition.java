package com.pumpkings.pkcrates.core.animation.script;

import java.util.List;
import java.util.Map;

/** Immutable, validated animation produced by {@link ScriptAnimationLoader}. */
public record ScriptAnimationDefinition(
        String id,
        int duration,
        long seed,
        Map<String, Double> variables,
        List<ScriptAction> actions,
        int maxActionsPerTick,
        int maxDisplays
) {}
