package com.pumpkings.pkcrates.infrastructure.item;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConfigItemSerializer {

    public static void serialize(ItemStack item, ConfigurationSection parent, String key) {
        if (item == null || item.getType().isAir()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        
        // Rebuild a lightweight version to see if we'd lose data.
        ItemStack lightweight = new ItemStack(item.getType(), item.getAmount());
        if (meta != null) {
            ItemMeta lightweightMeta = lightweight.getItemMeta();
            if (lightweightMeta != null) {
                if (meta.hasDisplayName()) lightweightMeta.setDisplayName(meta.getDisplayName());
                if (meta.hasLore()) lightweightMeta.setLore(meta.getLore());
                if (meta.hasCustomModelData()) lightweightMeta.setCustomModelData(meta.getCustomModelData());
                if (meta.hasEnchants()) {
                    meta.getEnchants().forEach((ench, level) -> lightweightMeta.addEnchant(ench, level, true));
                }
                lightweight.setItemMeta(lightweightMeta);
            }
        }

        // If the item has more data than what we just rebuilt (e.g. trim, attributes, components), use native serialization.
        if (!item.isSimilar(lightweight)) {
            parent.set(key, item);
            return;
        }

        // Otherwise, use the lightweight format.
        ConfigurationSection section = parent.createSection(key);
        serialize(item, section);
    }

    public static void serialize(ItemStack item, ConfigurationSection section) {
        if (item == null || item.getType().isAir()) {
            return;
        }

        section.set("id", item.getType().name());
        section.set("amount", item.getAmount());

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                section.set("name", meta.getDisplayName());
            }

            if (meta.hasLore()) {
                section.set("lore", meta.getLore());
            }

            if (meta.hasCustomModelData()) {
                section.set("custom_model_data", meta.getCustomModelData());
            }

            if (meta.hasEnchants()) {
                List<String> enchantsList = new ArrayList<>();
                for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                    String enchName = entry.getKey().getKey().getKey().toUpperCase();
                    enchantsList.add(enchName + ";" + entry.getValue());
                }
                section.set("enchants", enchantsList);
            }
        }
    }
}
