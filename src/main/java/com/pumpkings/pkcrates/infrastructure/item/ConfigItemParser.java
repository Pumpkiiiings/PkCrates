package com.pumpkings.pkcrates.infrastructure.item;

import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ConfigItemParser {

    /**
     * Converts a YAML section into a real ItemStack using the Bukkit/Paper API.
     * Expected YAML example:
     * id: STONE_SWORD
     * name: '&fEspada'
     * amount: 1
     * enchants:
     * - MENDING;1
     */
    public static @Nullable ItemStack parse(ConfigurationSection section) {
        if (section == null) return null;

        String matName = section.getString("id", "STONE");
        Material material = Material.matchMaterial(matName);
        if (material == null) {
            return null;
        }

        ItemBuilder builder = ItemBuilder.of(material);

        if (section.contains("amount")) {
            builder.setAmount(section.getInt("amount", 1));
        }

        if (section.contains("name")) {
            builder.setName(section.getString("name"));
        }

        if (section.contains("lore")) {
            builder.setLore(section.getStringList("lore"));
        }
        
        if (section.contains("custom_model_data")) {
            builder.setCustomModelData(section.getInt("custom_model_data"));
        }

        if (section.contains("enchants")) {
            List<String> enchants = section.getStringList("enchants");
            for (String enchString : enchants) {
                // Expected format: ENCHANT_NAME;LEVEL (e.g. MENDING;1)
                String[] parts = enchString.split(";");
                if (parts.length >= 2) {
                    try {
                        String eName = parts[0].toLowerCase();
                        int level = Integer.parseInt(parts[1]);
                        Enchantment enchantment = Registry.ENCHANTMENT.get(org.bukkit.NamespacedKey.minecraft(eName));
                        if (enchantment != null) {
                            builder.addEnchant(enchantment, level);
                        }
                    } catch (Exception ignored) {
                        // We ignore badly formatted enchantments
                    }
                }
            }
        }

        return builder.build();
    }
}
