package com.pumpkings.pkcrates.infrastructure.audit.impl;

import com.pumpkings.pkcrates.infrastructure.audit.api.*;
import com.pumpkings.pkcrates.infrastructure.audit.config.LoggerConfig;
import com.pumpkings.pkcrates.infrastructure.audit.filter.EventFilter;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Production implementation of {@link AuditService}.
 *
 * <h3>Threading model</h3>
 * <ul>
 *   <li>All emit methods ({@code info}, {@code success}, etc.) enqueue to a
 *       {@link LinkedBlockingQueue} — O(1), thread-safe, non-blocking.</li>
 *   <li>A single {@link AuditWorker} daemon thread drains the queue and calls
 *       {@link AuditSink#write} on each registered sink.</li>
 *   <li>{@link #sinks} uses {@link CopyOnWriteArrayList} so sinks can be
 *       added/removed while the worker iterates safely.</li>
 * </ul>
 *
 * <h3>Backpressure</h3>
 * <p>If the queue reaches {@code queueCapacity}, new records are dropped.
 * A warning is emitted at most once per second to avoid log spam.</p>
 */
public class AuditServiceImpl implements AuditService {

    private final Plugin plugin;
    private final LoggerConfig loggerConfig;
    private final Logger logger;

    private final LinkedBlockingQueue<AuditRecord> queue;
    private final CopyOnWriteArrayList<AuditSink> sinks = new CopyOnWriteArrayList<>();
    private final AuditWorker worker;
    private final EventFilter filter;

    // Backpressure: track last drop-warning timestamp (ms)
    private final AtomicLong lastDropWarningMs = new AtomicLong(0L);

    /**
     * @param plugin        Owning plugin (for server name and logging).
     * @param loggerConfig  Parsed configuration from {@code logger.yml}.
     */
    public AuditServiceImpl(Plugin plugin, LoggerConfig loggerConfig) {
        this.plugin = plugin;
        this.loggerConfig = loggerConfig;
        this.logger = plugin.getLogger();
        this.filter = new EventFilter(loggerConfig);
        this.queue = new LinkedBlockingQueue<>(loggerConfig.getQueueCapacity());
        this.worker = new AuditWorker(queue, sinks, filter, plugin);
    }

    /**
     * Starts the background worker thread.
     * Must be called once after all sinks have been registered.
     */
    public void start() {
        if (loggerConfig.isEnabled()) {
            worker.start();
        }
    }

    // ── AuditService — emit methods ──────────────────────────────────────────

    @Override
    public void info(AuditEvent event, String actor, String target, Map<String, String> data) {
        enqueue(buildRecord(event, AuditPriority.INFO, actor, target, data));
    }

    @Override
    public void success(AuditEvent event, String actor, String target, Map<String, String> data) {
        enqueue(buildRecord(event, AuditPriority.SUCCESS, actor, target, data));
    }

    @Override
    public void warning(AuditEvent event, String actor, String target, Map<String, String> data) {
        enqueue(buildRecord(event, AuditPriority.WARNING, actor, target, data));
    }

    @Override
    public void error(AuditEvent event, String actor, String target, Map<String, String> data) {
        enqueue(buildRecord(event, AuditPriority.ERROR, actor, target, data));
    }

    @Override
    public void critical(AuditEvent event, String actor, String target, Map<String, String> data) {
        enqueue(buildRecord(event, AuditPriority.CRITICAL, actor, target, data));
    }

    @Override
    public void log(AuditRecord record) {
        enqueue(record);
    }

    // ── AuditService — sink management ──────────────────────────────────────

    @Override
    public void registerSink(AuditSink sink) {
        // Replace if a sink with the same ID already exists
        sinks.removeIf(s -> s.getId().equals(sink.getId()));
        sinks.add(sink);
        logger.info("[Audit] Registered sink: " + sink.getId());
    }

    @Override
    public void unregisterSink(String sinkId) {
        sinks.removeIf(s -> s.getId().equals(sinkId));
        logger.info("[Audit] Unregistered sink: " + sinkId);
    }

    @Override
    public List<AuditSink> getRegisteredSinks() {
        return Collections.unmodifiableList(sinks);
    }

    // ── AuditService — lifecycle ─────────────────────────────────────────────

    @Override
    public void shutdown() {
        // Send the poison pill — the worker will drain remaining records then exit
        queue.offer(AuditWorker.POISON);
        try {
            worker.join(5_000); // wait up to 5 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // Close all sinks
        for (AuditSink sink : sinks) {
            try { sink.close(); } catch (Exception e) {
                logger.warning("[Audit] Error closing sink '" + sink.getId() + "': " + e.getMessage());
            }
        }
        logger.info("[Audit] System shut down cleanly.");
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    private void enqueue(AuditRecord record) {
        if (!loggerConfig.isEnabled()) return;

        boolean offered = queue.offer(record);
        if (!offered) {
            // Throttled warning — at most 1 per second
            long now = System.currentTimeMillis();
            if (now - lastDropWarningMs.get() > 1_000 && lastDropWarningMs.compareAndSet(lastDropWarningMs.get(), now)) {
                logger.warning("[Audit] Queue is full (" + loggerConfig.getQueueCapacity()
                        + " records). Records are being dropped!");
            }
        }
    }

    private AuditRecord buildRecord(AuditEvent event, AuditPriority priority,
                                     String actor, String target, Map<String, String> data) {
        return AuditRecord.builder(event)
                .priority(priority)
                .actor(actor)
                .target(target)
                .data(data)
                .serverName(loggerConfig.getDiscordServerName())
                .build();
    }
}
