package com.pumpkings.pkcrates.infrastructure.key;

import com.pumpkings.pkcrates.core.model.key.IKey;
import com.pumpkings.pkcrates.core.model.key.KeyRecord;
import com.pumpkings.pkcrates.infrastructure.item.ConfigItemParser;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeyRegistry {

    private final Plugin plugin;
    private final Map<String, IKey> keys;

    public KeyRegistry(Plugin plugin) {
        this.plugin = plugin;
        this.keys = new HashMap<>();
    }

    public void loadAll() {
        keys.clear();
        File keysFolder = new File(plugin.getDataFolder(), "keys");
        if (!keysFolder.exists()) {
            keysFolder.mkdirs();
        }

        File[] files = keysFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            String keyId = file.getName().replace(".yml", "");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            
            boolean isVirtual = config.getBoolean("virtual", false);
            
            ConfigurationSection itemSection = config.getConfigurationSection("item");
            ItemStack baseItem = null;
            if (itemSection != null) {
                baseItem = ConfigItemParser.parse(itemSection);
            }

            IKey key = new KeyRecord(keyId, isVirtual, baseItem);
            
            String resType = config.getString("rarity.restriction", "ANY").toUpperCase();
            try {
                key.setRarityRestriction(IKey.RarityRestriction.valueOf(resType));
            } catch (IllegalArgumentException e) {
                key.setRarityRestriction(IKey.RarityRestriction.ANY);
            }
            key.setRarityTarget(config.getString("rarity.target", ""));
            
            keys.put(keyId, key);
        }
        
        plugin.getLogger().info("Loaded " + keys.size() + " keys.");
    }

    public IKey getKey(String id) {
        return keys.get(id);
    }

    public List<IKey> getAllKeys() {
        return new ArrayList<>(keys.values());
    }
    
    public void updateKey(IKey key) {
        keys.put(key.getId(), key);
        saveKey(key);
    }

    public void createKey(String id, boolean isVirtual) {
        if (keys.containsKey(id)) return;
        
        IKey newKey = new KeyRecord(id, isVirtual, new ItemStack(org.bukkit.Material.TRIPWIRE_HOOK));
        keys.put(id, newKey);
        saveKey(newKey);
    }

    public void deleteKey(String id) {
        keys.remove(id);
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            File file = new File(plugin.getDataFolder() + File.separator + "keys", id + ".yml");
            if (file.exists()) {
                file.delete();
            }
        });
    }

    public void saveKey(IKey key) {
        // Anti-lag asynchronous saving
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            File file = new File(plugin.getDataFolder() + File.separator + "keys", key.getId() + ".yml");
            YamlConfiguration config = new YamlConfiguration();
            
            config.set("virtual", key.isVirtual());
            
            if (key.getBaseItem() != null) {
                com.pumpkings.pkcrates.infrastructure.item.ConfigItemSerializer.serialize(key.getBaseItem(), config.createSection("item"));
            }
            
            if (key.getRarityRestriction() != null && key.getRarityRestriction() != IKey.RarityRestriction.ANY) {
                config.set("rarity.restriction", key.getRarityRestriction().name());
                if (key.getRarityTarget() != null && !key.getRarityTarget().isEmpty()) {
                    config.set("rarity.target", key.getRarityTarget());
                }
            }
            
            try {
                config.save(file);
            } catch (Exception e) {
                plugin.getLogger().severe("Error saving key " + key.getId() + ": " + e.getMessage());
            }
        });
    }
}
