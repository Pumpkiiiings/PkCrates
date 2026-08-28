package com.pumpkings.pkcrates.infrastructure.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;

public class ConfigManager {

    private final Plugin plugin;
    private YamlConfiguration config;

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        plugin.getLogger().info("Global configuration loaded.");
    }

    public boolean isMetricsEnabled() {
        return config != null && config.getBoolean("settings.metrics", true);
    }

    public String getPrefix() {
        return config != null ? config.getString("settings.prefix", "<gray>[<aqua>PkCrates</aqua>] <reset>") : "";
    }

    public YamlConfiguration getConfig() {
        return config;
    }
}
