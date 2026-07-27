package com.pumpkings.pkcrates.core.service;

import com.pumpkings.pkcrates.api.event.rarity.RarityCreateEvent;
import com.pumpkings.pkcrates.api.event.rarity.RarityDeleteEvent;
import com.pumpkings.pkcrates.api.event.rarity.RarityUpdateEvent;
import com.pumpkings.pkcrates.api.rarity.RarityService;
import com.pumpkings.pkcrates.core.model.rarity.Rarity;
import com.pumpkings.pkcrates.core.model.rarity.RarityChanceMode;
import com.pumpkings.pkcrates.infrastructure.config.RarityConfig;
import com.pumpkings.pkcrates.infrastructure.config.RarityRegistry;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.Collection;

public class RarityServiceImpl implements RarityService {

    private final Plugin plugin;
    private final RarityRegistry registry;
    private final RarityConfig config;

    public RarityServiceImpl(Plugin plugin, RarityRegistry registry, RarityConfig config) {
        this.plugin = plugin;
        this.registry = registry;
        this.config = config;
    }

    @Override
    public Rarity create(String id) {
        if (registry.exists(id)) {
            return null;
        }

        Rarity rarity = new Rarity(id);
        rarity.setDisplayName("<gray>" + id + "</gray>");
        rarity.setPriority(1);
        rarity.setEnabled(true);
        rarity.setChanceMode(RarityChanceMode.INDEPENDENT);
        rarity.setWeight(10.0);
        rarity.setIcon("PAPER");
        rarity.setColor("<white>");
        rarity.setMiniMessageFormat("{color}{text}");

        RarityCreateEvent event = new RarityCreateEvent(rarity);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return null;
        }

        registry.register(rarity);
        config.save(rarity);

        return rarity;
    }

    @Override
    public boolean delete(String id) {
        Rarity rarity = registry.getRarity(id);
        if (rarity == null) {
            return false;
        }

        RarityDeleteEvent event = new RarityDeleteEvent(rarity);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return false;
        }

        registry.unregister(id);
        config.delete(id);

        return true;
    }

    @Override
    public boolean update(Rarity rarity) {
        if (!registry.exists(rarity.getId())) {
            return false;
        }

        Rarity oldRarity = registry.getRarity(rarity.getId());
        RarityUpdateEvent event = new RarityUpdateEvent(oldRarity, rarity);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return false;
        }

        registry.register(rarity);
        config.save(rarity);

        return true;
    }

    @Override
    public Rarity get(String id) {
        return registry.getRarity(id);
    }

    @Override
    public Collection<Rarity> getAll() {
        return registry.getAllRarities();
    }

    @Override
    public boolean exists(String id) {
        return registry.exists(id);
    }
}
