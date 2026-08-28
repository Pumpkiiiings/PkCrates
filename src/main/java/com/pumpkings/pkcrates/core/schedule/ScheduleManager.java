package com.pumpkings.pkcrates.core.schedule;

import com.pumpkings.pkcrates.PkCratesPlugin;
import com.pumpkings.pkcrates.core.model.key.IKey;
import com.pumpkings.pkcrates.core.service.KeyService;
import com.pumpkings.pkcrates.infrastructure.key.KeyRegistry;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ScheduleManager {

    private final PkCratesPlugin plugin;
    private final KeyService keyService;
    private final KeyRegistry keyRegistry;
    
    private final List<Schedule> schedules = new ArrayList<>();
    private BukkitTask task;
    private LocalDateTime lastCheckTime;

    public ScheduleManager(PkCratesPlugin plugin, KeyService keyService, KeyRegistry keyRegistry) {
        this.plugin = plugin;
        this.keyService = keyService;
        this.keyRegistry = keyRegistry;
        this.lastCheckTime = LocalDateTime.now().minusMinutes(1);
    }

    public void load() {
        schedules.clear();
        File file = new File(plugin.getDataFolder(), "schedules.yml");
        if (!file.exists()) {
            plugin.saveResource("schedules.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("schedules");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            ConfigurationSection schedConfig = section.getConfigurationSection(id);
            if (schedConfig == null) continue;

            String keyId = schedConfig.getString("key_id");
            int amount = schedConfig.getInt("amount", 1);
            
            List<String> messages;
            if (schedConfig.isList("message")) {
                messages = schedConfig.getStringList("message");
            } else {
                String singleMsg = schedConfig.getString("message", "");
                messages = new ArrayList<>();
                if (!singleMsg.isEmpty()) {
                    messages.add(singleMsg);
                }
            }

            Integer month = schedConfig.contains("month") ? schedConfig.getInt("month") : null;
            Integer dayOfMonth = schedConfig.contains("day_of_month") ? schedConfig.getInt("day_of_month") : null;
            
            DayOfWeek dayOfWeek = null;
            if (schedConfig.contains("day_of_week")) {
                try {
                    dayOfWeek = DayOfWeek.valueOf(schedConfig.getString("day_of_week").toUpperCase());
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid day_of_week in schedule '" + id + "'");
                }
            }
            
            Integer hour = schedConfig.contains("hour") ? schedConfig.getInt("hour") : null;
            Integer minute = schedConfig.contains("minute") ? schedConfig.getInt("minute") : null;

            schedules.add(new Schedule(id, keyId, amount, messages, month, dayOfMonth, dayOfWeek, hour, minute));
        }

        startTask();
    }

    public void startTask() {
        if (task != null) {
            task.cancel();
        }

        // Run every minute (1200 ticks = 60 seconds)
        // We run it every 20 ticks (1 sec) to precisely catch the minute boundary, 
        // or we could run every 1200 ticks. Better to run every 20 ticks and check if minute changed.
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            LocalDateTime now = LocalDateTime.now();
            
            // Only process if the minute has changed
            if (now.getMinute() == lastCheckTime.getMinute() && now.getHour() == lastCheckTime.getHour() && now.getDayOfYear() == lastCheckTime.getDayOfYear()) {
                return;
            }
            
            lastCheckTime = now;
            
            for (Schedule schedule : schedules) {
                if (matches(schedule, now)) {
                    executeSchedule(schedule);
                }
            }
        }, 20L, 20L);
    }

    private boolean matches(Schedule schedule, LocalDateTime time) {
        if (schedule.getMonth() != null && schedule.getMonth() != time.getMonthValue()) return false;
        if (schedule.getDayOfMonth() != null && schedule.getDayOfMonth() != time.getDayOfMonth()) return false;
        if (schedule.getDayOfWeek() != null && schedule.getDayOfWeek() != time.getDayOfWeek()) return false;
        if (schedule.getHour() != null && schedule.getHour() != time.getHour()) return false;
        if (schedule.getMinute() != null && schedule.getMinute() != time.getMinute()) return false;
        return true;
    }

    private void executeSchedule(Schedule schedule) {
        IKey key = keyRegistry.getKey(schedule.getKeyId());
        if (key == null) {
            plugin.getLogger().warning("Schedule '" + schedule.getId() + "' tried to give unknown key '" + schedule.getKeyId() + "'.");
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            keyService.giveKey(player, key, schedule.getAmount());
            if (schedule.getMessages() != null && !schedule.getMessages().isEmpty()) {
                for (String msg : schedule.getMessages()) {
                    player.sendMessage(com.pumpkings.pkcrates.presentation.utils.TextUtil.parse(msg));
                }
            }
        }
    }
}
