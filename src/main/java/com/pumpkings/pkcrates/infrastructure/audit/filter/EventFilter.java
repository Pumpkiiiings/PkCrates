package com.pumpkings.pkcrates.infrastructure.audit.filter;

import com.pumpkings.pkcrates.infrastructure.audit.api.AuditRecord;
import com.pumpkings.pkcrates.infrastructure.audit.config.LoggerConfig;

/**
 * Global pre-filter applied by the {@code AuditWorker} before dispatching
 * a record to any sink.
 *
 * <p>A record passes the filter when:</p>
 * <ol>
 *   <li>Its category is enabled in {@code logger.events.*}.</li>
 *   <li>Its priority is at or above {@code logger.min-priority}.</li>
 * </ol>
 *
 * <p>Sink-level filtering ({@link com.pumpkings.pkcrates.infrastructure.audit.api.AuditSink#accepts})
 * is a second, independent pass that runs per-sink after this one.</p>
 */
public class EventFilter {

    private final LoggerConfig config;

    public EventFilter(LoggerConfig config) {
        this.config = config;
    }

    /**
     * Returns {@code true} if the record should be forwarded to sinks.
     *
     * @param record The candidate record.
     * @return {@code true} when the record passes all global filters.
     */
    public boolean accepts(AuditRecord record) {
        // Category filter
        if (!config.isCategoryEnabled(record.getCategory())) return false;
        // Global priority threshold
        return record.getPriority().isAtLeast(config.getGlobalMinPriority());
    }
}
