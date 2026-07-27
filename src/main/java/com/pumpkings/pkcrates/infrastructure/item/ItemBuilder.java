package com.pumpkings.pkcrates.infrastructure.item;

import com.pumpkings.pkcrates.presentation.utils.TextUtil;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    private ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public static ItemBuilder of(Material material) {
        return new ItemBuilder(material);
    }

    public ItemBuilder setAmount(int amount) {
        item.setAmount(amount);
        return this;
    }

    public ItemBuilder setName(String name) {
        if (meta != null && name != null && !name.isEmpty()) {
            meta.displayName(TextUtil.parse(name));
        }
        return this;
    }

    public ItemBuilder setLore(List<String> lore) {
        if (meta != null && lore != null && !lore.isEmpty()) {
            List<net.kyori.adventure.text.Component> components = new ArrayList<>();
            for (String line : lore) {
                components.add(TextUtil.parse(line));
            }
            meta.lore(components);
        }
        return this;
    }

    public ItemBuilder setLore(String... lore) {
        return setLore(java.util.Arrays.asList(lore));
    }
    
    public ItemBuilder addLoreLine(String line) {
        if (meta != null && line != null) {
            List<net.kyori.adventure.text.Component> lore = meta.lore();
            if (lore == null) {
                lore = new ArrayList<>();
            }
            lore.add(TextUtil.parse(line));
            meta.lore(lore);
        }
        return this;
    }
    
    public static ItemBuilder of(ItemStack item) {
        return new ItemBuilder(item);
    }
    
    private ItemBuilder(ItemStack item) {
        this.item = item.clone();
        this.meta = this.item.getItemMeta();
    }

    public ItemBuilder addEnchant(Enchantment enchantment, int level) {
        if (meta != null && enchantment != null) {
            meta.addEnchant(enchantment, level, true);
        }
        return this;
    }

    public ItemBuilder addFlag(ItemFlag flag) {
        if (meta != null) {
            meta.addItemFlags(flag);
        }
        return this;
    }
    
    public ItemBuilder setCustomModelData(int customModelData) {
        if (meta != null) {
            meta.setCustomModelData(customModelData);
        }
        return this;
    }

    public ItemStack build() {
        if (meta != null) {
            item.setItemMeta(meta);
        }
        return item;
    }
}
