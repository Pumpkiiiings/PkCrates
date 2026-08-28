package com.pumpkings.pkcrates.infrastructure.config;

import com.pumpkings.pkcrates.core.model.rarity.Rarity;
import com.pumpkings.pkcrates.core.model.rarity.RarityChanceMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class RarityConfig {

    private final Plugin plugin;
    private final RarityRegistry registry;
    private File file;
    private YamlConfiguration config;

    public RarityConfig(Plugin plugin, RarityRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
        this.file = new File(plugin.getDataFolder(), "rarities.yml");
    }

    public void load() {
        if (!file.exists()) {
            plugin.saveResource("rarities.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
        registry.clear();

        ConfigurationSection raritiesSec = config.getConfigurationSection("rarities");
        if (raritiesSec == null) return;

        for (String id : raritiesSec.getKeys(false)) {
            ConfigurationSection sec = raritiesSec.getConfigurationSection(id);
            if (sec == null) continue;

            try {
                Rarity rarity = new Rarity(id);
                rarity.setDisplayName(sec.getString("display-name", id));
                rarity.setDescription(sec.getString("description", ""));
                rarity.setPriority(sec.getInt("priority", 1));
                rarity.setEnabled(sec.getBoolean("enabled", true));

                // Chance
                ConfigurationSection chanceSec = sec.getConfigurationSection("chance");
                if (chanceSec != null) {
                    String modeStr = chanceSec.getString("mode", "independent").toUpperCase();
                    try {
                        rarity.setChanceMode(RarityChanceMode.valueOf(modeStr));
                    } catch (IllegalArgumentException e) {
                        rarity.setChanceMode(RarityChanceMode.INDEPENDENT);
                    }
                    rarity.setWeight(chanceSec.getDouble("weight", 10.0));
                } else {
                    rarity.setChanceMode(RarityChanceMode.INDEPENDENT);
                    rarity.setWeight(10.0);
                }

                // Visuals
                ConfigurationSection visualsSec = sec.getConfigurationSection("visuals");
                if (visualsSec != null) {
                    rarity.setColor(visualsSec.getString("color", "<white>"));
                    rarity.setMiniMessageFormat(visualsSec.getString("format", "{color}{text}"));
                    rarity.setIcon(visualsSec.getString("icon", "PAPER"));
                    rarity.setGlow(visualsSec.getBoolean("glow", false));
                }

                // Effects
                ConfigurationSection effectsSec = sec.getConfigurationSection("effects");
                if (effectsSec != null) {
                    rarity.setParticle(effectsSec.getString("particle", ""));
                    rarity.setSound(effectsSec.getString("sound", ""));
                    rarity.setFireworkColor(effectsSec.getString("firework-color", ""));
                    rarity.setEffectLines(effectsSec.getStringList("list"));
                }

                // Broadcast
                ConfigurationSection broadcastSec = sec.getConfigurationSection("broadcast");
                if (broadcastSec != null) {
                    rarity.setBroadcastEnabled(broadcastSec.getBoolean("enabled", false));
                    rarity.setAnnouncementTemplate(broadcastSec.getString("message", ""));
                }

                // Settings
                ConfigurationSection settingsSec = sec.getConfigurationSection("settings");
                if (settingsSec != null) {
                    rarity.setDefaultAnimation(settingsSec.getString("default-animation", ""));
                    rarity.setPermission(settingsSec.getString("permission", ""));
                }
                
                // Placeholders
                ConfigurationSection placeholdersSec = sec.getConfigurationSection("placeholders");
                if (placeholdersSec != null) {
                    Map<String, String> phs = new HashMap<>();
                    for (String key : placeholdersSec.getKeys(false)) {
                        phs.put(key, placeholdersSec.getString(key));
                    }
                    rarity.setPlaceholders(phs);
                }

                registry.register(rarity);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load rarity: " + id, e);
            }
        }
        plugin.getLogger().info("Loaded " + registry.getAllRarities().size() + " rarities.");
    }

    public void save(Rarity rarity) {
        ConfigurationSection raritiesSec = config.getConfigurationSection("rarities");
        if (raritiesSec == null) {
            raritiesSec = config.createSection("rarities");
        }
        ConfigurationSection sec = raritiesSec.getConfigurationSection(rarity.getId());
        if (sec == null) {
            sec = raritiesSec.createSection(rarity.getId());
        }

        sec.set("display-name", rarity.getDisplayName());
        sec.set("description", rarity.getDescription());
        sec.set("priority", rarity.getPriority());
        sec.set("enabled", rarity.isEnabled());
        
        // Chance
        sec.set("chance.mode", rarity.getChanceMode().name().toLowerCase());
        sec.set("chance.weight", rarity.getWeight());
        
        // Visuals
        sec.set("visuals.color", rarity.getColor());
        sec.set("visuals.format", rarity.getMiniMessageFormat());
        sec.set("visuals.icon", rarity.getIcon());
        sec.set("visuals.glow", rarity.isGlow());
        
        // Effects
        sec.set("effects.particle", rarity.getParticle());
        sec.set("effects.sound", rarity.getSound());
        sec.set("effects.firework-color", rarity.getFireworkColor());
        if (!rarity.getEffectLines().isEmpty()) {
            sec.set("effects.list", rarity.getEffectLines());
        }
        
        // Broadcast
        sec.set("broadcast.enabled", rarity.isBroadcastEnabled());
        sec.set("broadcast.message", rarity.getAnnouncementTemplate());
        
        // Settings
        sec.set("settings.default-animation", rarity.getDefaultAnimation());
        sec.set("settings.permission", rarity.getPermission());
        
        // Placeholders
        if (rarity.getPlaceholders() != null && !rarity.getPlaceholders().isEmpty()) {
            for (Map.Entry<String, String> entry : rarity.getPlaceholders().entrySet()) {
                sec.set("placeholders." + entry.getKey(), entry.getValue());
            }
        } else {
            sec.set("placeholders", null); // Remove section if empty
        }

        saveFile();
    }

    public void delete(String id) {
        ConfigurationSection raritiesSec = config.getConfigurationSection("rarities");
        if (raritiesSec != null) {
            raritiesSec.set(id, null);
            saveFile();
        }
    }

    private void saveFile() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save rarities.yml", e);
        }
    }
}
