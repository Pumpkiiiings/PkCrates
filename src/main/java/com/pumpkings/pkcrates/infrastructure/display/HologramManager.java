package com.pumpkings.pkcrates.infrastructure.display;

import com.pumpkings.pkcrates.core.model.Crate;
import com.pumpkings.pkcrates.core.model.HologramConfig;
import com.pumpkings.pkcrates.infrastructure.config.CrateRegistry;
import com.pumpkings.pkcrates.infrastructure.location.CrateLocationManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BlockVector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HologramManager implements Listener {

    private final Plugin plugin;
    private final CrateLocationManager locationMgr;
    private final CrateRegistry crateRegistry;
    
    // We store the entities in memory so we can delete them: WorldName -> ChunkKey -> List<TextDisplay>
    private final Map<String, Map<Long, List<TextDisplay>>> activeDisplays;

    public HologramManager(Plugin plugin, CrateLocationManager locationMgr, CrateRegistry crateRegistry) {
        this.plugin = plugin;
        this.locationMgr = locationMgr;
        this.crateRegistry = crateRegistry;
        this.activeDisplays = new HashMap<>();
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        World world = chunk.getWorld();
        
        Map<BlockVector, String> cratesInChunk = locationMgr.getCratesInChunk(world.getName(), chunk.getX(), chunk.getZ());
        
        if (cratesInChunk.isEmpty()) return;

        long chunkKey = getChunkKey(chunk.getX(), chunk.getZ());
        List<TextDisplay> chunkDisplays = activeDisplays
                .computeIfAbsent(world.getName(), k -> new HashMap<>())
                .computeIfAbsent(chunkKey, k -> new ArrayList<>());

        for (Map.Entry<BlockVector, String> entry : cratesInChunk.entrySet()) {
            BlockVector vec = entry.getKey();
            String crateId = entry.getValue();
            
            Crate crate = crateRegistry.getCrate(crateId);
            if (crate == null || crate.getHologramConfig() == null || !crate.getHologramConfig().hasContent()) continue;
            
            Location spawnLoc = new Location(world, vec.getBlockX() + 0.5, vec.getBlockY() + 1.3, vec.getBlockZ() + 0.5);
            TextDisplay display = spawnHologram(spawnLoc, crate.getHologramConfig());
            if (display != null) {
                chunkDisplays.add(display);
            }
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        long chunkKey = getChunkKey(chunk.getX(), chunk.getZ());
        
        Map<Long, List<TextDisplay>> worldMap = activeDisplays.get(chunk.getWorld().getName());
        if (worldMap != null) {
            List<TextDisplay> chunkDisplays = worldMap.remove(chunkKey);
            if (chunkDisplays != null) {
                for (TextDisplay display : chunkDisplays) {
                    if (display.isValid()) {
                        display.remove();
                    }
                }
            }
        }
    }

    public void removeAll() {
        for (Map<Long, List<TextDisplay>> worldMap : activeDisplays.values()) {
            for (List<TextDisplay> displays : worldMap.values()) {
                for (TextDisplay display : displays) {
                    if (display.isValid()) {
                        display.remove();
                    }
                }
            }
        }
        activeDisplays.clear();
    }
    
    public void spawnFor(Location loc, Crate crate) {
        if (loc.getWorld() == null) return;
        if (crate == null || crate.getHologramConfig() == null || !crate.getHologramConfig().hasContent()) return;
        
        long chunkKey = getChunkKey(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
        List<TextDisplay> chunkDisplays = activeDisplays
                .computeIfAbsent(loc.getWorld().getName(), k -> new HashMap<>())
                .computeIfAbsent(chunkKey, k -> new ArrayList<>());

        Location spawnLoc = loc.clone().add(0.5, 1.3, 0.5);
        TextDisplay display = spawnHologram(spawnLoc, crate.getHologramConfig());
        if (display != null) {
            chunkDisplays.add(display);
        }
    }
    
    public void removeFor(Location loc) {
        if (loc.getWorld() == null) return;
        long chunkKey = getChunkKey(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
        
        Map<Long, List<TextDisplay>> worldMap = activeDisplays.get(loc.getWorld().getName());
        if (worldMap != null) {
            List<TextDisplay> chunkDisplays = worldMap.get(chunkKey);
            if (chunkDisplays != null) {
                // Find and remove the exact text display at this block
                chunkDisplays.removeIf(display -> {
                    if (display.getLocation().getBlockX() == loc.getBlockX() && 
                        display.getLocation().getBlockY() == loc.getBlockY() + 1 && 
                        display.getLocation().getBlockZ() == loc.getBlockZ()) {
                        
                        if (display.isValid()) {
                            display.remove();
                        }
                        return true;
                    }
                    return false;
                });
            }
        }
    }

    private TextDisplay spawnHologram(Location location, HologramConfig config) {
        TextDisplay display = (TextDisplay) location.getWorld().spawnEntity(location, EntityType.TEXT_DISPLAY);
        
        Component fullText = Component.empty();
        MiniMessage mm = MiniMessage.miniMessage();
        
        com.pumpkings.pkcrates.api.rarity.RarityService rarityService = 
            ((com.pumpkings.pkcrates.PkCratesPlugin) plugin).getRarityService();
            
        for (int i = 0; i < config.content().size(); i++) {
            String line = config.content().get(i);
            
            // Replace Rarity placeholders e.g. <rarity_epic_display>
            for (com.pumpkings.pkcrates.core.model.rarity.Rarity r : rarityService.getAll()) {
                String rId = r.getId().toLowerCase();
                String mmFormat = r.getMiniMessageFormat() != null ? r.getMiniMessageFormat() : "{color}{text}";
                String displayStr = mmFormat
                        .replace("{color}", r.getColor() != null ? r.getColor() : "")
                        .replace("{text}", r.getDisplayName() != null ? r.getDisplayName() : r.getId());
                        
                line = line.replace("<rarity_" + rId + "_display>", displayStr);
                line = line.replace("<rarity_" + rId + "_color>", r.getColor() != null ? r.getColor() : "");
            }
            
            fullText = fullText.append(mm.deserialize(line));
            if (i < config.content().size() - 1) {
                fullText = fullText.append(Component.newline());
            }
        }
        
        display.text(fullText);
        display.setBillboard(config.billboard());
        display.setShadowed(config.shadowText());
        
        // Handle background color ("none" = transparent)
        if (config.backgroundColor().equalsIgnoreCase("none")) {
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        } else {
            // A default dark color if it parses incorrectly
            display.setBackgroundColor(Color.fromARGB(100, 0, 0, 0));
        }

        // Prevent it from being saved to disk
        display.setPersistent(false);
        
        return display;
    }

    private long getChunkKey(int chunkX, int chunkZ) {
        return (long) chunkX & 0xffffffffL | ((long) chunkZ & 0xffffffffL) << 32;
    }
}
