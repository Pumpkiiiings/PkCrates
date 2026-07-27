package com.pumpkings.pkcrates.infrastructure.audit.impl;

import com.pumpkings.pkcrates.infrastructure.audit.api.AuditRecord;
import com.pumpkings.pkcrates.infrastructure.audit.api.AuditSink;
import com.pumpkings.pkcrates.infrastructure.audit.filter.EventFilter;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Daemon thread that consumes {@link AuditRecord}s from the shared queue and
 * dispatches them to every registered {@link AuditSink}.
 *
 * <h3>Error isolation</h3>
 * <p>Each sink is wrapped in a try-catch. A failing sink (even an unchecked
 * exception) cannot affect other sinks or the worker loop.</p>
 *
 * <h3>Shutdown</h3>
 * <p>Send a {@code null} poison pill to the queue, or call
 * {@link Thread#interrupt()} on this thread. After receiving the signal,
 * the worker drains any remaining records and exits cleanly.</p>
 */
public class AuditWorker extends Thread {

    private final BlockingQueue<AuditRecord> queue;
    private final List<AuditSink> sinks;
    private final EventFilter filter;
    private final Logger logger;

    /** Sentinel value that signals the worker to shut down. */
    static final AuditRecord POISON = AuditRecord.builder(
            com.pumpkings.pkcrates.infrastructure.audit.api.AuditEvent.DEBUG)
            .actor("SYSTEM").target("SHUTDOWN").build();

    public AuditWorker(BlockingQueue<AuditRecord> queue,
                       List<AuditSink> sinks,
                       EventFilter filter,
                       Plugin plugin) {
        super("PkCrates-AuditWorker");
        setDaemon(true);
        this.queue = queue;
        this.sinks = sinks;
        this.filter = filter;
        this.logger = plugin.getLogger();
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                AuditRecord record = queue.take(); // blocks until record available

                if (record == POISON) {            // shutdown signal received
                    drainRemaining();
                    break;
                }

                dispatch(record);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // restore interrupt flag
                drainRemaining();
                break;
            }
        }
    }

    /** Dispatches a single record to all sinks that accept it. */
    private void dispatch(AuditRecord record) {
        if (!filter.accepts(record)) return;

        for (AuditSink sink : sinks) {
            if (!sink.accepts(record)) continue;
            try {
                sink.write(record);
            } catch (Exception e) {
                // Log but do NOT propagate — one bad sink must not affect others
                logger.log(Level.WARNING,
                        "[AuditWorker] Sink '" + sink.getId() + "' threw an exception: " + e.getMessage(), e);
            }
        }
    }

    /** Processes remaining records after the shutdown signal. */
    private void drainRemaining() {
        AuditRecord record;
        while ((record = queue.poll()) != null) {
            if (record != POISON) dispatch(record);
        }
    }
}
