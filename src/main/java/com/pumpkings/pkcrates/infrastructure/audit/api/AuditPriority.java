package com.pumpkings.pkcrates.infrastructure.audit.api;

/**
 * Severity levels for audit records, ordered from lowest to highest.
 *
 * <p>Both the global filter ({@code logger.min-priority}) and each individual
 * sink can set a minimum priority threshold. Records below that threshold are
 * silently skipped.</p>
 */
public enum AuditPriority {

    DEBUG(0),
    INFO(1),
    SUCCESS(2),
    WARNING(3),
    ERROR(4),
    CRITICAL(5);

    private final int level;

    AuditPriority(int level) {
        this.level = level;
    }

    /** @return Numeric level used for threshold comparisons. */
    public int getLevel() {
        return level;
    }

    /**
     * Returns {@code true} if this priority is at least as severe as
     * {@code other}.
     *
     * @param other The minimum required priority.
     * @return {@code true} when {@code this.level >= other.level}.
     */
    public boolean isAtLeast(AuditPriority other) {
        return this.level >= other.level;
    }
}
