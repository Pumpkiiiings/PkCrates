package com.pumpkings.pkcrates.infrastructure.item;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class ItemDataManager {

    private final NamespacedKey crateKey;
    private final NamespacedKey itemTypeKey;

    public ItemDataManager(Plugin plugin) {
        this.crateKey = new NamespacedKey(plugin, "crate_id");
        this.itemTypeKey = new NamespacedKey(plugin, "item_type"); // 'key', 'crate_block', etc.
    }

    /**
     * Tags an ItemStack as a Physical Key for a specific crate.
     */
    public ItemStack tagAsKey(ItemStack item, String crateId) {
        if (item == null || item.getItemMeta() == null) return item;
        
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        
        pdc.set(itemTypeKey, PersistentDataType.STRING, "key");
        pdc.set(crateKey, PersistentDataType.STRING, crateId);
        
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Reads which crate this key belongs to (If it is a key).
     * Returns the crate ID, or null if it is not a valid key.
     */
    public String getCrateIdFromKey(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        
        if (!"key".equals(pdc.get(itemTypeKey, PersistentDataType.STRING))) {
            return null;
        }
        
        return pdc.get(crateKey, PersistentDataType.STRING);
    }
}
