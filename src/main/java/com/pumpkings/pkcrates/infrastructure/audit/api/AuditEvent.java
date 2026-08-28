package com.pumpkings.pkcrates.infrastructure.audit.api;

/**
 * Catalogue of every auditable event in PkCrates.
 *
 * <p>Each constant knows its {@link AuditCategory} and the default
 * {@link AuditPriority} to use when the caller does not specify one.
 * The caller may always override the priority via
 * {@link AuditRecord.Builder#priority(AuditPriority)}.</p>
 *
 * <h3>Adding new events</h3>
 * <p>Just add a new constant here. No other class needs to change — the
 * {@link AuditService} and all sinks handle {@link AuditEvent} generically.</p>
 */
public enum AuditEvent {

    // ── Crates ──────────────────────────────────────────────────────────────
    CRATE_CREATED(AuditCategory.CRATES,  AuditPriority.SUCCESS),
    CRATE_DELETED(AuditCategory.CRATES,  AuditPriority.WARNING),
    CRATE_EDITED(AuditCategory.CRATES,   AuditPriority.INFO),
    CRATE_OPENED(AuditCategory.CRATES,   AuditPriority.INFO),

    // ── Rewards ─────────────────────────────────────────────────────────────
    REWARD_ADDED(AuditCategory.REWARDS,   AuditPriority.SUCCESS),
    REWARD_REMOVED(AuditCategory.REWARDS, AuditPriority.WARNING),
    REWARD_WON(AuditCategory.REWARDS,     AuditPriority.SUCCESS),
    REWARD_REROLLED(AuditCategory.REWARDS, AuditPriority.INFO),

    // ── Keys ────────────────────────────────────────────────────────────────
    KEY_CREATED(AuditCategory.KEYS,  AuditPriority.SUCCESS),
    KEY_DELETED(AuditCategory.KEYS,  AuditPriority.WARNING),
    KEY_GIVEN(AuditCategory.KEYS,    AuditPriority.SUCCESS),
    KEY_REMOVED(AuditCategory.KEYS,  AuditPriority.WARNING),
    KEY_USED(AuditCategory.KEYS,     AuditPriority.INFO),

    // ── Claims ──────────────────────────────────────────────────────────────
    CLAIM_STORED(AuditCategory.CLAIMS,   AuditPriority.WARNING),
    CLAIM_CLAIMED(AuditCategory.CLAIMS,  AuditPriority.SUCCESS),
    CLAIM_CLEARED(AuditCategory.CLAIMS,  AuditPriority.WARNING),

    // ── Admin ───────────────────────────────────────────────────────────────
    CONFIG_RELOADED(AuditCategory.ADMIN, AuditPriority.INFO),
    PLUGIN_ENABLED(AuditCategory.ADMIN,  AuditPriority.INFO),
    PLUGIN_DISABLED(AuditCategory.ADMIN, AuditPriority.INFO),

    // ── Debug ────────────────────────────────────────────────────────────────
    DEBUG(AuditCategory.DEBUG, AuditPriority.DEBUG);

    // -------------------------------------------------------------------------

    private final AuditCategory category;
    private final AuditPriority defaultPriority;

    AuditEvent(AuditCategory category, AuditPriority defaultPriority) {
        this.category = category;
        this.defaultPriority = defaultPriority;
    }

    /** @return The broad category this event belongs to. */
    public AuditCategory getCategory() {
        return category;
    }

    /** @return The priority used when the caller does not specify one. */
    public AuditPriority getDefaultPriority() {
        return defaultPriority;
    }
}
