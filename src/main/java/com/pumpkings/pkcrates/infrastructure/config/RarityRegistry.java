package com.pumpkings.pkcrates.infrastructure.config;

import com.pumpkings.pkcrates.core.model.rarity.Rarity;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RarityRegistry {

    private final Plugin plugin;
    private final Map<String, Rarity> rarities;

    public RarityRegistry(Plugin plugin) {
        this.plugin = plugin;
        this.rarities = new ConcurrentHashMap<>();
    }

    public void register(Rarity rarity) {
        rarities.put(rarity.getId(), rarity);
    }

    public void unregister(String id) {
        rarities.remove(id);
    }

    public Rarity getRarity(String id) {
        return rarities.get(id);
    }

    public boolean exists(String id) {
        return rarities.containsKey(id);
    }

    public Collection<Rarity> getAllRarities() {
        return Collections.unmodifiableCollection(rarities.values());
    }

    public void clear() {
        rarities.clear();
    }
}
