package com.pumpkings.pkcrates.infrastructure.audit.config;

import com.pumpkings.pkcrates.infrastructure.audit.api.AuditCategory;
import com.pumpkings.pkcrates.infrastructure.audit.api.AuditPriority;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.EnumMap;
import java.util.Map;

/**
 * Typed wrapper around {@code logger.yml}.
 *
 * <p>Loaded once at startup and on {@code /crate reload}.
 * All accessors return pre-parsed values — no YAML parsing happens
 * at event time.</p>
 */
public class LoggerConfig {

    private final Plugin plugin;
    private YamlConfiguration config;

    // Cached values (parsed on load)
    private boolean enabled;
    private boolean async;
    private int queueCapacity;
    private AuditPriority globalMinPriority;
    private final Map<AuditCategory, Boolean> categoryEnabled = new EnumMap<>(AuditCategory.class);

    // Console
    private boolean consoleEnabled;
    private AuditPriority consoleMinPriority;
    private String consoleFormat;

    // Discord
    private boolean discordEnabled;
    private String discordUrl;
    private String discordUsername;
    private String discordAvatarUrl;
    private String discordServerName;
    private boolean discordRetryEnabled;
    private int discordRetryAttempts;
    private int discordRetryDelaySeconds;
    private final Map<AuditPriority, Integer> discordColors = new EnumMap<>(AuditPriority.class);
    private final Map<AuditCategory, String> discordEmojis = new EnumMap<>(AuditCategory.class);
    private AuditPriority discordMinPriority;

    // File
    private boolean fileEnabled;
    private String filePath;
    private String fileFilenamePattern;
    private boolean fileRotateDaily;
    private int fileFlushIntervalMs;
    private AuditPriority fileMinPriority;

    public LoggerConfig(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads (or reloads) {@code logger.yml} from disk, merging any missing
     * keys from the bundled default.
     */
    public void load() {
        File configFile = new File(plugin.getDataFolder(), "logger.yml");
        if (!configFile.exists()) {
            plugin.saveResource("logger.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        // Merge defaults from jar
        InputStream defaultStream = plugin.getResource("logger.yml");
        if (defaultStream != null) {
            YamlConfiguration def = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            boolean changed = false;
            for (String key : def.getKeys(true)) {
                if (!config.contains(key)) {
                    config.set(key, def.get(key));
                    changed = true;
                }
            }
            if (changed) {
                try { config.save(configFile); } catch (Exception ignored) {}
            }
        }

        parseValues();
    }

    // -------------------------------------------------------------------------
    // Parsing
    // -------------------------------------------------------------------------

    private void parseValues() {
        enabled = config.getBoolean("logger.enabled", true);
        async = config.getBoolean("logger.async", true);
        queueCapacity = config.getInt("logger.queue-capacity", 10_000);
        globalMinPriority = parsePriority("logger.min-priority", AuditPriority.INFO);

        // Categories
        for (AuditCategory cat : AuditCategory.values()) {
            String key = "logger.events." + cat.name().toLowerCase();
            categoryEnabled.put(cat, config.getBoolean(key, cat != AuditCategory.DEBUG));
        }

        // Console
        consoleEnabled = config.getBoolean("logger.console.enabled", true);
        consoleMinPriority = parsePriority("logger.console.min-priority", AuditPriority.INFO);
        consoleFormat = config.getString("logger.console.format",
                "[AUDIT] [{priority}] [{event}] {actor} -> {target}");

        // Discord
        discordEnabled = config.getBoolean("logger.discord.enabled", false);
        discordUrl = config.getString("logger.discord.url", "");
        discordUsername = config.getString("logger.discord.username", "PkCrates");
        discordAvatarUrl = config.getString("logger.discord.avatar-url", "");
        discordServerName = config.getString("logger.discord.server-name", "Survival");
        discordRetryEnabled = config.getBoolean("logger.discord.retry.enabled", true);
        discordRetryAttempts = config.getInt("logger.discord.retry.attempts", 3);
        discordRetryDelaySeconds = config.getInt("logger.discord.retry.delay-seconds", 2);
        discordMinPriority = parsePriority("logger.discord.min-priority", AuditPriority.SUCCESS);

        for (AuditPriority p : AuditPriority.values()) {
            String key = "logger.discord.colors." + p.name();
            discordColors.put(p, config.getInt(key, 3447003)); // blue default
        }
        for (AuditCategory cat : AuditCategory.values()) {
            String key = "logger.discord.emojis." + cat.name();
            discordEmojis.put(cat, config.getString(key, "[" + cat.name() + "]"));
        }

        // File
        fileEnabled = config.getBoolean("logger.file.enabled", false);
        filePath = config.getString("logger.file.path", "plugins/PkCrates/audit/");
        fileFilenamePattern = config.getString("logger.file.filename-pattern", "audit-{date}.log");
        fileRotateDaily = "daily".equalsIgnoreCase(config.getString("logger.file.rotate", "daily"));
        fileFlushIntervalMs = config.getInt("logger.file.flush-interval-ms", 500);
        fileMinPriority = parsePriority("logger.file.min-priority", AuditPriority.DEBUG);
    }

    private AuditPriority parsePriority(String path, AuditPriority defaultValue) {
        String raw = config.getString(path, defaultValue.name()).toUpperCase();
        try { return AuditPriority.valueOf(raw); } catch (IllegalArgumentException e) { return defaultValue; }
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public boolean isEnabled() { return enabled; }
    public boolean isAsync() { return async; }
    public int getQueueCapacity() { return queueCapacity; }
    public AuditPriority getGlobalMinPriority() { return globalMinPriority; }
    public boolean isCategoryEnabled(AuditCategory category) {
        return categoryEnabled.getOrDefault(category, true);
    }

    // Console
    public boolean isConsoleEnabled() { return consoleEnabled; }
    public AuditPriority getConsoleMinPriority() { return consoleMinPriority; }
    public String getConsoleFormat() { return consoleFormat; }

    // Discord
    public boolean isDiscordEnabled() { return discordEnabled && !discordUrl.isBlank(); }
    public String getDiscordUrl() { return discordUrl; }
    public String getDiscordUsername() { return discordUsername; }
    public String getDiscordAvatarUrl() { return discordAvatarUrl; }
    public String getDiscordServerName() { return discordServerName; }
    public boolean isDiscordRetryEnabled() { return discordRetryEnabled; }
    public int getDiscordRetryAttempts() { return discordRetryAttempts; }
    public int getDiscordRetryDelaySeconds() { return discordRetryDelaySeconds; }
    public int getDiscordColor(AuditPriority priority) { return discordColors.getOrDefault(priority, 3447003); }
    public String getDiscordEmoji(AuditCategory category) { return discordEmojis.getOrDefault(category, ""); }
    public AuditPriority getDiscordMinPriority() { return discordMinPriority; }

    // File
    public boolean isFileEnabled() { return fileEnabled; }
    public String getFilePath() { return filePath; }
    public String getFileFilenamePattern() { return fileFilenamePattern; }
    public boolean isFileRotateDaily() { return fileRotateDaily; }
    public int getFileFlushIntervalMs() { return fileFlushIntervalMs; }
    public AuditPriority getFileMinPriority() { return fileMinPriority; }
}
