package com.pumpkings.pkcrates.infrastructure.audit.api;

/**
 * A pluggable output destination for audit records.
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li>{@link #write(AuditRecord)} is always called from the {@code AuditWorker}
 *       thread — implementations may perform blocking I/O safely.</li>
 *   <li>Exceptions thrown by {@link #write} are caught by the worker and logged;
 *       they do NOT propagate to other sinks.</li>
 *   <li>{@link #close()} is called on plugin disable; implementations must
 *       release all held resources (files, HTTP clients, thread pools, etc.).</li>
 * </ul>
 *
 * <h3>Registering a custom sink</h3>
 * <pre>{@code
 * AuditService audit = ...; // from ServicesManager
 * audit.registerSink(new MySlackSink(webhookUrl));
 * }</pre>
 */
public interface AuditSink {

    /** Unique identifier for this sink (e.g. {@code "discord"}, {@code "file"}). */
    String getId();

    /** Whether this sink is currently active. When false, the worker skips it. */
    boolean isEnabled();

    /**
     * Writes the record to this sink's output.
     *
     * <p>Called from the {@code AuditWorker} thread — blocking I/O is acceptable.
     * Do not perform heavy computation here; use an internal async mechanism if needed.</p>
     *
     * @param record The record to write. Guaranteed non-null.
     */
    void write(AuditRecord record);

    /**
     * Returns {@code true} if this sink accepts the given record.
     *
     * <p>The worker calls this before {@link #write}. Sinks can apply their own
     * priority threshold or category filter here independently of the global filter.</p>
     *
     * @param record The candidate record.
     * @return {@code true} to write, {@code false} to skip.
     */
    default boolean accepts(AuditRecord record) {
        return isEnabled();
    }

    /**
     * Called once when the plugin is disabled.
     * Flush buffers, close file handles, shut down executors, etc.
     */
    default void close() {}
}
