package com.pumpkings.pkcrates.infrastructure.audit.impl;

import com.pumpkings.pkcrates.infrastructure.audit.api.AuditRecord;
import com.pumpkings.pkcrates.infrastructure.audit.api.AuditSink;
import org.bukkit.plugin.Plugin;

import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Background daemon that retries failed sink writes with exponential backoff.
 */
public class AuditRetryWorker extends Thread {

    private final ConcurrentLinkedQueue<RetryRecord> queue = new ConcurrentLinkedQueue<>();
    private final List<AuditSink> sinks;
    private final Logger logger;

    public AuditRetryWorker(List<AuditSink> sinks, Plugin plugin) {
        super("PkCrates-AuditRetryWorker");
        setDaemon(true);
        this.sinks = sinks;
        this.logger = plugin.getLogger();
    }

    /**
     * Enqueues a failed record for later retry.
     */
    public void enqueue(AuditRecord record, AuditSink sink, int maxAttempts, int baseDelaySeconds) {
        queue.offer(new RetryRecord(record, sink.getId(), maxAttempts, baseDelaySeconds));
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(1000); // Check every second
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            Instant now = Instant.now();
            Iterator<RetryRecord> it = queue.iterator();

            while (it.hasNext()) {
                RetryRecord rr = it.next();
                if (now.isAfter(rr.nextAttempt)) {
                    it.remove(); // Remove to process
                    processRetry(rr);
                }
            }
        }
    }

    private void processRetry(RetryRecord rr) {
        AuditSink sink = findSink(rr.sinkId);
        if (sink == null || !sink.isEnabled()) {
            return; // Sink no longer exists or is disabled, drop it.
        }

        try {
            sink.write(rr.record);
            // Success! No need to re-enqueue
        } catch (Exception e) {
            rr.attemptsLeft--;
            if (rr.attemptsLeft > 0) {
                // Calculate next exponential delay
                int delay = rr.baseDelaySeconds * (int) Math.pow(2, rr.maxAttempts - rr.attemptsLeft);
                rr.nextAttempt = Instant.now().plusSeconds(delay);
                queue.offer(rr); // Re-enqueue for later
            } else {
                logger.log(Level.SEVERE,
                        "[Audit] Exhausted all retries for sink '" + rr.sinkId + "'. Record dropped: "
                                + rr.record.getEvent() + " / " + rr.record.getTarget());
            }
        }
    }

    private AuditSink findSink(String id) {
        for (AuditSink s : sinks) {
            if (s.getId().equals(id)) return s;
        }
        return null;
    }

    private static class RetryRecord {
        final AuditRecord record;
        final String sinkId;
        final int maxAttempts;
        final int baseDelaySeconds;
        int attemptsLeft;
        Instant nextAttempt;

        RetryRecord(AuditRecord record, String sinkId, int maxAttempts, int baseDelaySeconds) {
            this.record = record;
            this.sinkId = sinkId;
            this.maxAttempts = maxAttempts;
            this.baseDelaySeconds = baseDelaySeconds;
            this.attemptsLeft = maxAttempts;
            // First retry delay is just baseDelaySeconds
            this.nextAttempt = Instant.now().plusSeconds(baseDelaySeconds);
        }
    }
}
