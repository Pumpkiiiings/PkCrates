package com.pumpkings.pkcrates.infrastructure.audit.api;

import java.util.List;
import java.util.Map;

/**
 * Central entry point for the PkCrates audit system.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // Simple convenience call — uses the event's default priority
 * auditService.success(AuditEvent.REWARD_WON, player.getName(), rewardId,
 *         Map.of("crate", crateId, "world", world.getName()));
 *
 * // Full control
 * AuditRecord record = AuditRecord.builder(AuditEvent.CRATE_DELETED)
 *         .actor("CONSOLE")
 *         .target(crateId)
 *         .priority(AuditPriority.CRITICAL)
 *         .build();
 * auditService.log(record);
 * }</pre>
 *
 * <h3>Threading</h3>
 * <p>All emit methods ({@code info}, {@code success}, etc.) are thread-safe
 * and non-blocking — they enqueue the record and return immediately.</p>
 *
 * <h3>Obtaining an instance</h3>
 * <p>Internal PkCrates components receive this via constructor injection.
 * External plugins use Bukkit's {@code ServicesManager}:</p>
 * <pre>{@code
 * RegisteredServiceProvider<AuditService> rsp =
 *     Bukkit.getServicesManager().getRegistration(AuditService.class);
 * if (rsp != null) { AuditService audit = rsp.getProvider(); }
 * }</pre>
 */
public interface AuditService {

    // ── Convenience emitters ─────────────────────────────────────────────────

    /**
     * Emits a record with {@link AuditPriority#INFO}.
     *
     * @param event  The type of event.
     * @param actor  Who triggered it (player name, "CONSOLE", "SYSTEM").
     * @param target What the action was performed on.
     * @param data   Additional structured data (may be null or empty).
     */
    void info(AuditEvent event, String actor, String target, Map<String, String> data);

    /** Emits a record with {@link AuditPriority#SUCCESS}. */
    void success(AuditEvent event, String actor, String target, Map<String, String> data);

    /** Emits a record with {@link AuditPriority#WARNING}. */
    void warning(AuditEvent event, String actor, String target, Map<String, String> data);

    /** Emits a record with {@link AuditPriority#ERROR}. */
    void error(AuditEvent event, String actor, String target, Map<String, String> data);

    /** Emits a record with {@link AuditPriority#CRITICAL}. */
    void critical(AuditEvent event, String actor, String target, Map<String, String> data);

    /**
     * Enqueues a pre-built {@link AuditRecord} for processing.
     * Use when you need to specify fields not covered by the convenience methods.
     *
     * @param record The record to emit.
     */
    void log(AuditRecord record);

    // ── Sink management ──────────────────────────────────────────────────────

    /**
     * Registers a new sink. The sink will start receiving records immediately.
     * If a sink with the same {@link AuditSink#getId()} is already registered,
     * it is replaced.
     *
     * @param sink The sink to register.
     */
    void registerSink(AuditSink sink);

    /**
     * Removes the sink with the given ID. No-op if not found.
     *
     * @param sinkId The ID returned by {@link AuditSink#getId()}.
     */
    void unregisterSink(String sinkId);

    /**
     * Returns an unmodifiable snapshot of currently registered sinks.
     *
     * @return Live (but unmodifiable) list of sinks.
     */
    List<AuditSink> getRegisteredSinks();

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Shuts down the worker thread, drains remaining queued records (best-effort),
     * and calls {@link AuditSink#close()} on every registered sink.
     *
     * <p>Called automatically from {@code PkCratesPlugin.onDisable()}.</p>
     */
    void shutdown();
}
