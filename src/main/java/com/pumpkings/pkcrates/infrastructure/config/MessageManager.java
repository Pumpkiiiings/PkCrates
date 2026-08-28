package com.pumpkings.pkcrates.infrastructure.config;

import com.pumpkings.pkcrates.presentation.utils.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageManager {

    private final Plugin plugin;
    private final File configFile;
    private YamlConfiguration config;

    public MessageManager(Plugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "messages.yml");
    }

    public void loadMessages() {
        if (!configFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        // Update file if any key is missing
        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            boolean changed = false;
            for (String key : defaultConfig.getKeys(true)) {
                if (!config.contains(key)) {
                    config.set(key, defaultConfig.get(key));
                    changed = true;
                }
            }
            if (changed) {
                try {
                    config.save(configFile);
                } catch (Exception e) {
                    plugin.getLogger().severe("Could not save messages.yml after updating missing keys.");
                }
            }
        }
    }

    public String getRawMessage(String path) {
        return config.getString(path, "<red>Message not found: " + path);
    }

    public Component getComponent(String path, Map<String, String> placeholders) {
        String raw = getRawMessage(path);
        
        if (!path.equals(Messages.PREFIX)) {
            String prefix = config.getString(Messages.PREFIX, "");
            if (raw.contains("<prefix>")) {
                raw = raw.replace("<prefix>", prefix);
            }
        }

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                raw = raw.replace(entry.getKey(), entry.getValue());
            }
        }
        return TextUtil.parse(raw);
    }
    
    public Component getComponent(String path) {
        return getComponent(path, null);
    }

    public void sendMessage(CommandSender sender, String path) {
        sender.sendMessage(getComponent(path));
    }

    /**
     * Shows a title/subtitle pair from the message file.
     *
     * <p>Either half may be blank in {@code messages.yml} to hide that line; a title whose
     * two halves are both blank is skipped entirely, which is how a server disables the
     * crate-opening title without editing code.</p>
     *
     * @param player       Recipient.
     * @param titlePath    Message path for the title line.
     * @param subtitlePath Message path for the subtitle line.
     * @param placeholders Replacements applied to both lines; may be {@code null}.
     */
    public void showTitle(org.bukkit.entity.Player player, String titlePath, String subtitlePath,
                          Map<String, String> placeholders) {

        String rawTitle = getRawMessage(titlePath);
        String rawSubtitle = getRawMessage(subtitlePath);
        if ((rawTitle == null || rawTitle.isBlank()) && (rawSubtitle == null || rawSubtitle.isBlank())) {
            return;
        }

        net.kyori.adventure.title.Title.Times times = net.kyori.adventure.title.Title.Times.times(
                java.time.Duration.ofMillis(500),
                java.time.Duration.ofMillis(2000),
                java.time.Duration.ofMillis(500));

        player.showTitle(net.kyori.adventure.title.Title.title(
                getComponent(titlePath, placeholders),
                getComponent(subtitlePath, placeholders),
                times));
    }

    public void sendMessage(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(getComponent(path, placeholders));
    }
    
    public void sendMessage(CommandSender sender, String path, String placeholder1, String value1) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put(placeholder1, value1);
        sendMessage(sender, path, placeholders);
    }
    
    public void sendMessage(CommandSender sender, String path, String placeholder1, String value1, String placeholder2, String value2) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put(placeholder1, value1);
        placeholders.put(placeholder2, value2);
        sendMessage(sender, path, placeholders);
    }
    
    public void sendMessage(CommandSender sender, String path, String placeholder1, String value1, String placeholder2, String value2, String placeholder3, String value3) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put(placeholder1, value1);
        placeholders.put(placeholder2, value2);
        placeholders.put(placeholder3, value3);
        sendMessage(sender, path, placeholders);
    }
    
    // For multiline messages
    public void sendList(CommandSender sender, String path, Map<String, String> placeholders) {
        List<String> list = config.getStringList(path);
        if (list.isEmpty()) {
            sendMessage(sender, path, placeholders);
            return;
        }
        for (String line : list) {
            if (placeholders != null) {
                for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                    line = line.replace(entry.getKey(), entry.getValue());
                }
            }
            sender.sendMessage(TextUtil.parse(line));
        }
    }
}
