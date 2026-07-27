package com.pumpkings.pkcrates.infrastructure.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MenuConfig {

    private final FileConfiguration config;

    public MenuConfig(FileConfiguration config) {
        this.config = config;
    }

    public Component getTitle(Map<String, String> placeholders) {
        return parse(config.getString("title", "<gray>Menu"), placeholders);
    }

    public Component getItemName(String path, Map<String, String> placeholders) {
        return parse(config.getString(path + ".name", "<red>Unknown"), placeholders);
    }

    public List<Component> getItemLore(String path, Map<String, String> placeholders) {
        List<String> rawLore = config.getStringList(path + ".lore");
        List<Component> parsed = new ArrayList<>();
        for (String line : rawLore) {
            parsed.add(parse(line, placeholders));
        }
        return parsed;
    }
    
    public List<Component> getExactLore(String exactPath, Map<String, String> placeholders) {
        List<String> rawLore = config.getStringList(exactPath);
        List<Component> parsed = new ArrayList<>();
        for (String line : rawLore) {
            parsed.add(parse(line, placeholders));
        }
        return parsed;
    }
    
    public Material getItemMaterial(String path, Material defaultMat) {
        String matStr = config.getString(path + ".material");
        if (matStr != null) {
            try {
                return Material.valueOf(matStr.toUpperCase());
            } catch (Exception ignored) {}
        }
        return defaultMat;
    }

    private Component parse(String text, Map<String, String> placeholders) {
        if (text == null) return Component.empty();
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                text = text.replace(entry.getKey(), entry.getValue());
            }
        }
        return com.pumpkings.pkcrates.presentation.utils.TextUtil.parse(text);
    }
}
