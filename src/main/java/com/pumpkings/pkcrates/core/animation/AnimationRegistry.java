package com.pumpkings.pkcrates.core.animation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class AnimationRegistry {

    private final Map<String, Supplier<AnimationPhase>> registry = new HashMap<>();

    public void register(String id, Supplier<AnimationPhase> phaseSupplier) {
        registry.put(id.toUpperCase(), phaseSupplier);
    }

    public AnimationPhase createPhase(String id) {
        Supplier<AnimationPhase> supplier = registry.get(id.toUpperCase());
        if (supplier != null) {
            return supplier.get();
        }
        return null;
    }
    
    public java.util.Set<String> getRegisteredAnimations() {
        return registry.keySet();
    }
}
