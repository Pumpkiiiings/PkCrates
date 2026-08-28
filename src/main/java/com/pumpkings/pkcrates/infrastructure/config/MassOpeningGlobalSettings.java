package com.pumpkings.pkcrates.infrastructure.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class MassOpeningGlobalSettings {

    private boolean enabled = true;
    private boolean skipSingleKeyMenu = false;
    private boolean shiftRightClickOpenAll = false;
    private boolean disableMenu = false;
    private int defaultLimit = 10;
    private int rewardsPerTick = 5;
    private int asyncGenerationThreshold = 500;

    public void load(YamlConfiguration config) {
        if (config == null) return;

        ConfigurationSection section = config.getConfigurationSection("mass-opening");
        if (section != null) {
            this.enabled = section.getBoolean("enabled", true);
            ConfigurationSection settings = section.getConfigurationSection("settings");
            if (settings != null) {
                this.skipSingleKeyMenu = settings.getBoolean("skip-single-key-menu", false);
                this.shiftRightClickOpenAll = settings.getBoolean("shift-right-click-open-all", false);
                this.disableMenu = settings.getBoolean("disable-menu", false);
                this.defaultLimit = settings.getInt("default-limit", 10);
                this.rewardsPerTick = Math.max(1, settings.getInt("rewards-per-tick", 5));
                this.asyncGenerationThreshold = Math.max(10, settings.getInt("async-generation-threshold", 500));
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isSkipSingleKeyMenu() {
        return skipSingleKeyMenu;
    }

    public boolean isShiftRightClickOpenAll() {
        return shiftRightClickOpenAll;
    }

    public boolean isDisableMenu() {
        return disableMenu;
    }

    public int getDefaultLimit() {
        return defaultLimit;
    }

    public int getRewardsPerTick() {
        return rewardsPerTick;
    }

    public int getAsyncGenerationThreshold() {
        return asyncGenerationThreshold;
    }
}
