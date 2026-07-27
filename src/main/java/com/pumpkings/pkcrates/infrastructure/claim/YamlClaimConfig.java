package com.pumpkings.pkcrates.infrastructure.claim;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;

/**
 * YAML-backed implementation of {@link ClaimConfig}.
 *
 * <p>Reads the {@code claim:} section from {@code config.yml}.
 * Values are read lazily on every call so that a {@code /crate reload}
 * picks up changes without needing a server restart.</p>
 */
public class YamlClaimConfig implements ClaimConfig {

    private final Plugin plugin;

    /**
     * @param plugin The owning plugin, used to locate {@code config.yml}.
     */
    public YamlClaimConfig(Plugin plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // ClaimConfig
    // -------------------------------------------------------------------------

    @Override
    public boolean isEnabled() {
        return cfg().getBoolean("claim.enabled", true);
    }

    @Override
    public boolean storeIfInventoryFull() {
        return cfg().getBoolean("claim.store-if.inventory-full", true);
    }

    @Override
    public boolean storeIfPlayerOffline() {
        return cfg().getBoolean("claim.store-if.player-offline", true);
    }

    @Override
    public boolean storeIfDeliveryFailed() {
        return cfg().getBoolean("claim.store-if.delivery-failed", true);
    }

    @Override
    public int getMaxStored() {
        return cfg().getInt("claim.limits.maximum-stored", -1);
    }

    @Override
    public boolean notifyOnLogin() {
        return cfg().getBoolean("claim.notifications.login", true);
    }

    @Override
    public boolean notifyOnStore() {
        return cfg().getBoolean("claim.notifications.reward-stored", true);
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    /** Loads the configuration fresh every call to reflect live reloads. */
    private YamlConfiguration cfg() {
        File file = new File(plugin.getDataFolder(), "config.yml");
        return YamlConfiguration.loadConfiguration(file);
    }
}
