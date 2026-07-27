package com.pumpkings.pkcrates.infrastructure.item;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConfigItemSerializer {

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
