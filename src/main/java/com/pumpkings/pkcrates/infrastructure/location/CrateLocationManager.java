package com.pumpkings.pkcrates.infrastructure.location;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BlockVector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CrateLocationManager {

    private final Plugin plugin;
    private final File file;
    private YamlConfiguration config;

    // Spatial Map: WorldName -> ChunkKey(Long) -> Map<BlockVector, CrateId>
    // This makes looking up a block on click ultra fast O(1), without iterating over huge lists.
    private final Map<String, Map<Long, Map<BlockVector, String>>> spatialMap;

    public CrateLocationManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "locations.yml");
        this.spatialMap = new HashMap<>();
    }

    public void load() {
        spatialMap.clear();
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create locations.yml");
                return;
            }
        }

        config = YamlConfiguration.loadConfiguration(file);
        List<String> locStrings = config.getStringList("locations");

        for (String locString : locStrings) {
            // Expected format: world,x,y,z,crate_id
            String[] parts = locString.split(",");
            if (parts.length == 5) {
                String worldName = parts[0];
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                int z = Integer.parseInt(parts[3]);
                String crateId = parts[4];

                addLocationToMemory(worldName, x, y, z, crateId);
            }
        }
        plugin.getLogger().info("Loaded " + locStrings.size() + " crate locations.");
    }

    private void addLocationToMemory(String worldName, int x, int y, int z, String crateId) {
        long chunkKey = getChunkKey(x >> 4, z >> 4);
        BlockVector vector = new BlockVector(x, y, z);

        spatialMap.computeIfAbsent(worldName, k -> new HashMap<>())
                  .computeIfAbsent(chunkKey, k -> new HashMap<>())
                  .put(vector, crateId);
    }

    public void addLocation(Location location, String crateId) {
        if (location.getWorld() == null) return;
        
        String worldName = location.getWorld().getName();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        addLocationToMemory(worldName, x, y, z, crateId);
        saveToDisk();
    }

    public void removeLocation(Location location) {
        if (location.getWorld() == null) return;
        
        String worldName = location.getWorld().getName();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        long chunkKey = getChunkKey(x >> 4, z >> 4);
        BlockVector vector = new BlockVector(x, y, z);

        Map<Long, Map<BlockVector, String>> worldMap = spatialMap.get(worldName);
        if (worldMap != null) {
            Map<BlockVector, String> chunkMap = worldMap.get(chunkKey);
            if (chunkMap != null) {
                chunkMap.remove(vector);
                saveToDisk();
            }
        }
    }

    /**
     * Returns the crate ID if the clicked block is a physical crate, or null if it's not.
     */
    public String getCrateAt(Location location) {
        if (location.getWorld() == null) return null;
        
        String worldName = location.getWorld().getName();
        Map<Long, Map<BlockVector, String>> worldMap = spatialMap.get(worldName);
        if (worldMap == null) return null;

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        
        long chunkKey = getChunkKey(x >> 4, z >> 4);
        Map<BlockVector, String> chunkMap = worldMap.get(chunkKey);
        
        if (chunkMap == null) return null;

        return chunkMap.get(new BlockVector(x, y, z));
    }

    /**
     * Returns all crate locations within a specific chunk.
     * Useful for the HologramManager.
     */
    public Map<BlockVector, String> getCratesInChunk(String worldName, int chunkX, int chunkZ) {
        Map<Long, Map<BlockVector, String>> worldMap = spatialMap.get(worldName);
        if (worldMap == null) return new HashMap<>();

        long chunkKey = getChunkKey(chunkX, chunkZ);
        Map<BlockVector, String> chunkMap = worldMap.get(chunkKey);
        
        return chunkMap == null ? new HashMap<>() : new HashMap<>(chunkMap);
    }
    
    public List<Location> getAllLocations(String crateId) {
        List<Location> locations = new ArrayList<>();
        for (Map.Entry<String, Map<Long, Map<BlockVector, String>>> worldEntry : spatialMap.entrySet()) {
            World world = Bukkit.getWorld(worldEntry.getKey());
            if (world == null) continue;
            for (Map<BlockVector, String> chunkMap : worldEntry.getValue().values()) {
                for (Map.Entry<BlockVector, String> entry : chunkMap.entrySet()) {
                    if (entry.getValue().equals(crateId)) {
                        BlockVector vec = entry.getKey();
                        locations.add(new Location(world, vec.getBlockX(), vec.getBlockY(), vec.getBlockZ()));
                    }
                }
            }
        }
        return locations;
    }

    private void saveToDisk() {
        List<String> locStrings = new ArrayList<>();
        
        for (Map.Entry<String, Map<Long, Map<BlockVector, String>>> worldEntry : spatialMap.entrySet()) {
            String worldName = worldEntry.getKey();
            for (Map<BlockVector, String> chunkMap : worldEntry.getValue().values()) {
                for (Map.Entry<BlockVector, String> blockEntry : chunkMap.entrySet()) {
                    BlockVector vec = blockEntry.getKey();
                    locStrings.add(worldName + "," + vec.getBlockX() + "," + vec.getBlockY() + "," + vec.getBlockZ() + "," + blockEntry.getValue());
                }
            }
        }
        
        config.set("locations", locStrings);
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Error saving locations.yml");
        }
    }

    /**
     * Utility to generate a unique key (long) from the X and Z coordinates of a chunk.
     */
    private long getChunkKey(int chunkX, int chunkZ) {
        return (long) chunkX & 0xffffffffL | ((long) chunkZ & 0xffffffffL) << 32;
    }
}
