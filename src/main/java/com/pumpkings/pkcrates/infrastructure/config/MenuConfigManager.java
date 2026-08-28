package com.pumpkings.pkcrates.infrastructure.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuConfigManager {

    private final Plugin plugin;
    private final Map<String, MenuConfig> menus;

    public MenuConfigManager(Plugin plugin) {
        this.plugin = plugin;
        this.menus = new HashMap<>();
    }

    public void loadAll() {
        menus.clear();
        File menusFolder = new File(plugin.getDataFolder(), "menus");
        if (!menusFolder.exists()) {
            menusFolder.mkdirs();
            // Guardar menus por defecto
            saveDefaultMenu("crates_dashboard.yml");
            saveDefaultMenu("crate_editor.yml");
            saveDefaultMenu("crate_keys.yml");
            saveDefaultMenu("crate_location.yml");
            saveDefaultMenu("crate_rewards.yml");
            saveDefaultMenu("key_list.yml");
            saveDefaultMenu("key_editor.yml");
            saveDefaultMenu("reward_editor.yml");
            saveDefaultMenu("crate_preview.yml");
            saveDefaultMenu("claim_menu.yml");
            saveDefaultMenu("mass_opening_menu.yml");
            saveDefaultMenu("crate_animation.yml");
            saveDefaultMenu("rarities_dashboard.yml");
            saveDefaultMenu("rarity_editor.yml");
            saveDefaultMenu("reward_items.yml");
            saveDefaultMenu("reward_rarity_select.yml");
            saveDefaultMenu("mass_opening_editor.yml");
            saveDefaultMenu("rarities_dashboard.yml");
            saveDefaultMenu("crate_ambient.yml");
            saveDefaultMenu("crate_visuals.yml");
        }

        File[] files = menusFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String menuId = file.getName().replace(".yml", "");
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                menus.put(menuId, new MenuConfig(config));
            }
        }
    }

    private void saveDefaultMenu(String filename) {
        File file = new File(plugin.getDataFolder(), "menus/" + filename);
        if (!file.exists()) {
            plugin.saveResource("menus/" + filename, false);
        }
    }

    public MenuConfig getMenu(String menuId) {
        return menus.get(menuId);
    }
}
