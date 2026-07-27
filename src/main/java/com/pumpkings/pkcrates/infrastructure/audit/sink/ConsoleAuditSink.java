package com.pumpkings.pkcrates.infrastructure.audit.sink;

import com.pumpkings.pkcrates.infrastructure.audit.api.AuditRecord;
import com.pumpkings.pkcrates.infrastructure.audit.api.AuditSink;
import com.pumpkings.pkcrates.infrastructure.audit.config.LoggerConfig;
import org.bukkit.plugin.Plugin;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * Outputs audit records to the server console using the plugin's native logger.
 */
public class ConsoleAuditSink implements AuditSink {

    private final LoggerConfig config;
    private final Logger logger;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public ConsoleAuditSink(Plugin plugin, LoggerConfig config) {
        this.logger = plugin.getLogger();
        this.config = config;
    }

    @Override
    public String getId() {
        return "console";
    }

    @Override
    public boolean isEnabled() {
        return config.isConsoleEnabled();
    }

    @Override
    public boolean accepts(AuditRecord record) {
        // Sink-specific threshold filter
        return isEnabled() && record.getPriority().isAtLeast(config.getConsoleMinPriority());
    }

    @Override
    public void write(AuditRecord record) {
        String message = config.getConsoleFormat()
                .replace("{timestamp}", timeFormatter.format(record.getTimestamp()))
                .replace("{priority}", record.getPriority().name())
                .replace("{event}", record.getEvent().name())
                .replace("{actor}", record.getActor())
                .replace("{target}", record.getTarget());

        // Bukkit loggers don't inherently support ANSI via info(), but Paper often
        // translates it if we use standard info. For a clean, structured log,
        // standard INFO is usually best for audit trails.
        logger.info(message);
    }
}
