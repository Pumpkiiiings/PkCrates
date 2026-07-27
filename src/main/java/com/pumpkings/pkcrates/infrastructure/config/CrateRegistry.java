package com.pumpkings.pkcrates.infrastructure.config;

import com.pumpkings.pkcrates.core.model.Crate;
import com.pumpkings.pkcrates.core.model.reward.IReward;
import com.pumpkings.pkcrates.core.model.reward.UnifiedReward;
import com.pumpkings.pkcrates.core.model.HologramConfig;
import com.pumpkings.pkcrates.infrastructure.item.ConfigItemParser;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CrateRegistry {

    private final Plugin plugin;
    private final Map<String, Crate> crates;

    public CrateRegistry(Plugin plugin) {
        this.plugin = plugin;
        this.crates = new HashMap<>();
    }

    public void loadAll() {
        crates.clear();
        File cratesFolder = new File(plugin.getDataFolder(), "crates");
        if (!cratesFolder.exists()) {
            cratesFolder.mkdirs();
        }

        File[] files = cratesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            String crateId = file.getName().replace(".yml", "");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            
            String crateName = config.getString("name", crateId);
            List<String> acceptedKeys = config.getStringList("accepted_keys");
            
            ConfigurationSection rewardsSection = config.getConfigurationSection("rewards");
            List<IReward> rewardList = new ArrayList<>();

            if (rewardsSection != null) {
                for (String rewardId : rewardsSection.getKeys(false)) {
                    ConfigurationSection rSec = rewardsSection.getConfigurationSection(rewardId);
                    if (rSec == null) continue;
                    
                    double weight = rSec.getDouble("weight", 10.0);
                    int winLimit = rSec.getInt("limit", -1);
                    List<String> commands = rSec.getStringList("commands");
                    
                    // Display item (the one shown in the roulette)
                    ItemStack displayItem = ConfigItemParser.parse(rSec.getConfigurationSection("display_item"));
                    
                    // Items to give to the player (can be multiple)
                    List<ItemStack> winItems = new ArrayList<>();
                    ConfigurationSection winItemsSec = rSec.getConfigurationSection("items");
                    if (winItemsSec != null) {
                        for (String itemKey : winItemsSec.getKeys(false)) {
                            ItemStack winItem = ConfigItemParser.parse(winItemsSec.getConfigurationSection(itemKey));
                            if (winItem != null) {
                                winItems.add(winItem);
                            }
                        }
                    }

                    // For now we do not load fallback recursively to prevent infinite loops,
                    // can be expanded if needed
                    IReward reward = new UnifiedReward(rewardId, weight, displayItem, winLimit, null, winItems, commands);
                    
                    String rarityId = rSec.getString("rarity");
                    if (rarityId != null && !rarityId.isEmpty()) {
                        reward.setRarityId(rarityId);
                    }
                    if (rSec.contains("broadcast")) {
                        reward.setBroadcastEnabled(rSec.getBoolean("broadcast"));
                    }
                    
                    rewardList.add(reward);
                }
            }

            HologramConfig hologramConfig = null;
            ConfigurationSection holoSec = config.getConfigurationSection("hologram");
            if (holoSec != null) {
                List<String> content = holoSec.getStringList("content");
                Billboard billboard = Billboard.CENTER;
                try {
                    billboard = Billboard.valueOf(holoSec.getString("billboard", "CENTER").toUpperCase());
                } catch (IllegalArgumentException ignored) {}
                
                String bgColor = holoSec.getString("background-color", "none");
                boolean shadowText = holoSec.getBoolean("shadowtext", true);
                float scale = (float) holoSec.getDouble("scale", 1.0);
                
                hologramConfig = new HologramConfig(content, billboard, bgColor, shadowText, scale);
            }
            
            String animationId = config.getString("animation", "ROULETTE");

            Crate crate = new Crate(crateId, crateName, rewardList, acceptedKeys, hologramConfig, animationId);
            crates.put(crateId, crate);
            plugin.getLogger().info("Loaded crate: " + crateId + " with " + rewardList.size() + " rewards.");
        }
    }

    public Crate getCrate(String id) {
        return crates.get(id);
    }

    public List<Crate> getAllCrates() {
        return new ArrayList<>(crates.values());
    }

    public void createCrate(String id) {
        if (crates.containsKey(id)) return;
        
        HologramConfig defaultHologram = new HologramConfig(
                List.of("<bold><gradient:#4287f5:#42d4f5>" + id.toUpperCase() + "</gradient></bold>", "<gray>Use a key to open</gray>"),
                org.bukkit.entity.Display.Billboard.CENTER,
                "none",
                true,
                1.0f
        );
        Crate newCrate = new Crate(id, "&a" + id, new ArrayList<>(), new ArrayList<>(), defaultHologram, "ROULETTE");
        crates.put(id, newCrate);
        saveCrate(newCrate);
    }

    public void deleteCrate(String id) {
        crates.remove(id);
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            File file = new File(plugin.getDataFolder() + File.separator + "crates", id + ".yml");
            if (file.exists()) {
                file.delete();
            }
        });
    }

    public void saveCrate(Crate crate) {
        // We execute writing asynchronously to avoid causing lag (Async File Worker)
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            File file = new File(plugin.getDataFolder() + File.separator + "crates", crate.getId() + ".yml");
            YamlConfiguration config = new YamlConfiguration();
            
            config.set("name", crate.getName());
            config.set("accepted_keys", crate.getAcceptedKeys());
            config.set("animation", crate.getAnimationId());
            
            if (crate.getHologramConfig() != null) {
                ConfigurationSection holoSec = config.createSection("hologram");
                holoSec.set("content", crate.getHologramConfig().content());
                holoSec.set("billboard", crate.getHologramConfig().billboard().name());
                holoSec.set("background-color", crate.getHologramConfig().backgroundColor());
                holoSec.set("shadowtext", crate.getHologramConfig().shadowText());
                holoSec.set("scale", crate.getHologramConfig().scale());
            }
            
            ConfigurationSection rewardsSec = config.createSection("rewards");
            for (IReward reward : crate.getRewards()) {
                if (reward instanceof com.pumpkings.pkcrates.core.model.reward.UnifiedReward unReward) {
                    ConfigurationSection rSec = rewardsSec.createSection(unReward.getId());
                    rSec.set("weight", unReward.getWeight());
                    
                    if (unReward.getRarityId() != null) {
                        rSec.set("rarity", unReward.getRarityId());
                    }
                    
                    if (unReward.isBroadcastEnabled()) {
                        rSec.set("broadcast", true);
                    }
                    
                    if (unReward.getWinLimit() > 0) {
                        rSec.set("limit", unReward.getWinLimit());
                    }
                    
                    if (unReward.getDisplayItem() != null) {
                        com.pumpkings.pkcrates.infrastructure.item.ConfigItemSerializer.serialize(unReward.getDisplayItem(), rSec.createSection("display_item"));
                    }
                    
                    if (unReward.getCommands() != null && !unReward.getCommands().isEmpty()) {
                        rSec.set("commands", unReward.getCommands());
                    }
                    
                    if (unReward.getItems() != null && !unReward.getItems().isEmpty()) {
                        ConfigurationSection itemsSec = rSec.createSection("items");
                        int i = 1;
                        for (org.bukkit.inventory.ItemStack item : unReward.getItems()) {
                            com.pumpkings.pkcrates.infrastructure.item.ConfigItemSerializer.serialize(item, itemsSec.createSection("item_" + i));
                            i++;
                        }
                    }
                }
            }
            try {
                config.save(file);
            } catch (Exception e) {
                plugin.getLogger().severe("Error saving crate " + crate.getId() + ": " + e.getMessage());
            }
        });
    }
}
