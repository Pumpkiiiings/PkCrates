package com.pumpkings.pkcrates.infrastructure.audit.api;

/**
 * Classifies audit events into broad groups for filtering purposes.
 *
 * <p>Each {@link AuditEvent} belongs to exactly one category.
 * Category-level filtering is configured in {@code logger.yml} under
 * {@code logger.events.*}.</p>
 */
public enum AuditCategory {
    CRATES,
    REWARDS,
    KEYS,
    CLAIMS,
    ADMIN,
    DEBUG
}
