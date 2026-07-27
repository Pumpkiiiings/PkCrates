package com.pumpkings.pkcrates.infrastructure.audit.sink;

import com.pumpkings.pkcrates.infrastructure.audit.api.AuditRecord;
import com.pumpkings.pkcrates.infrastructure.audit.api.AuditSink;
import com.pumpkings.pkcrates.infrastructure.audit.config.LoggerConfig;
import org.bukkit.plugin.Plugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Outputs audit records to a daily rotated log file.
 */
public class FileAuditSink implements AuditSink {

    private final Plugin plugin;
    private final LoggerConfig config;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());
    
    private BufferedWriter writer;
    private LocalDate currentFileDate;
    private ScheduledExecutorService flushScheduler;

    public FileAuditSink(Plugin plugin, LoggerConfig config) {
        this.plugin = plugin;
        this.config = config;
        
        if (config.isFileEnabled()) {
            initFlushTask();
        }
    }

    private void initFlushTask() {
        int intervalMs = config.getFileFlushIntervalMs();
        if (intervalMs > 0) {
            flushScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "PkCrates-FileAuditFlush");
                t.setDaemon(true);
                return t;
            });
            flushScheduler.scheduleAtFixedRate(this::flush, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public String getId() {
        return "file";
    }

    @Override
    public boolean isEnabled() {
        return config.isFileEnabled();
    }

    @Override
    public boolean accepts(AuditRecord record) {
        return isEnabled() && record.getPriority().isAtLeast(config.getFileMinPriority());
    }

    @Override
    public synchronized void write(AuditRecord record) {
        try {
            rotateIfNecessary();

            if (writer == null) {
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("[").append(timeFormatter.format(record.getTimestamp())).append("] ")
              .append("[").append(record.getPriority().name()).append("] ")
              .append("[").append(record.getEvent().name()).append("] ")
              .append(record.getActor()).append(" -> ").append(record.getTarget());

            if (!record.getData().isEmpty()) {
                sb.append(" | ");
                boolean first = true;
                for (Map.Entry<String, String> entry : record.getData().entrySet()) {
                    if (!first) sb.append(", ");
                    sb.append(entry.getKey()).append("=").append(entry.getValue());
                    first = false;
                }
            }
            
            sb.append(System.lineSeparator());
            writer.write(sb.toString());

            if (config.getFileFlushIntervalMs() <= 0) {
                writer.flush();
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[Audit] Failed to write to file log", e);
        }
    }

    private void rotateIfNecessary() throws IOException {
        LocalDate today = LocalDate.now();
        
        if (writer == null || (config.isFileRotateDaily() && !today.equals(currentFileDate))) {
            closeWriter();
            
            String dateStr = config.isFileRotateDaily() ? dateFormatter.format(today) : "all";
            String filename = config.getFileFilenamePattern().replace("{date}", dateStr);
            
            File dir = new File(config.getFilePath().replace("/", File.separator));
            if (!dir.exists() && !dir.isAbsolute()) {
                dir = new File(plugin.getServer().getWorldContainer(), config.getFilePath().replace("/", File.separator));
            }
            
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            File logFile = new File(dir, filename);
            writer = new BufferedWriter(new FileWriter(logFile, true));
            currentFileDate = today;
        }
    }

    private synchronized void flush() {
        if (writer != null) {
            try {
                writer.flush();
            } catch (IOException ignored) {}
        }
    }

    private void closeWriter() {
        if (writer != null) {
            try {
                writer.flush();
                writer.close();
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "[Audit] Error closing log file", e);
            }
            writer = null;
        }
    }

    @Override
    public synchronized void close() {
        if (flushScheduler != null && !flushScheduler.isShutdown()) {
            flushScheduler.shutdown();
        }
        closeWriter();
    }
}
