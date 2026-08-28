package com.pumpkings.pkcrates.infrastructure.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

public class SchedulerEngine {

    private final Plugin plugin;
    private final boolean isFolia;

    public SchedulerEngine(Plugin plugin) {
        this.plugin = plugin;
        this.isFolia = isFoliaAvailable();
    }

    private boolean isFoliaAvailable() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Executes an asynchronous task globally.
     * On Folia uses GlobalRegionScheduler. On Paper uses the async BukkitScheduler.
     */
    public void runAsync(Runnable task) {
        if (isFolia) {
            Bukkit.getGlobalRegionScheduler().execute(plugin, task);
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    /**
     * Executes a task synchronized with the thread of a specific region (Chunk/Block).
     * On Folia uses RegionScheduler. On Paper uses the main Bukkit thread.
     */
    public void runAtLocation(Location location, Runnable task) {
        if (isFolia) {
            Bukkit.getRegionScheduler().execute(plugin, location, task);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
}
