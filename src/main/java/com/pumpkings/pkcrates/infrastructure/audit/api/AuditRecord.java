package com.pumpkings.pkcrates.infrastructure.audit.api;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable snapshot of a single auditable action.
 *
 * <p>Create instances via the {@link Builder}:</p>
 * <pre>{@code
 * AuditRecord record = AuditRecord.builder(AuditEvent.CRATE_OPENED)
 *         .actor(player.getName())
 *         .target(crateId)
 *         .data(Map.of("world", world.getName()))
 *         .serverName("Survival")
 *         .build();
 * }</pre>
 *
 * <p>Once built, no field can be modified — every getter returns the
 * original value captured at build time.</p>
 */
public final class AuditRecord {

    private final UUID id;
    private final Instant timestamp;
    private final AuditEvent event;
    private final AuditPriority priority;
    private final AuditCategory category;
    private final String actor;
    private final String target;
    private final Map<String, String> data;
    private final String serverName;
    private final String worldName;

    private AuditRecord(Builder b) {
        this.id         = UUID.randomUUID();
        this.timestamp  = Instant.now();
        this.event      = b.event;
        this.priority   = b.priority != null ? b.priority : b.event.getDefaultPriority();
        this.category   = b.event.getCategory();
        this.actor      = b.actor != null ? b.actor : "SYSTEM";
        this.target     = b.target != null ? b.target : "";
        this.data       = Collections.unmodifiableMap(b.data);
        this.serverName = b.serverName != null ? b.serverName : "server";
        this.worldName  = b.worldName != null ? b.worldName : "";
    }

    // ── Getters ─────────────────────────────────────────────────────────────

    /** Unique ID for this record (not persisted by default). */
    public UUID getId() { return id; }

    /** Exact instant when this record was created. */
    public Instant getTimestamp() { return timestamp; }

    /** The type of event that occurred. */
    public AuditEvent getEvent() { return event; }

    /** Effective priority (defaulted from the event if not overridden). */
    public AuditPriority getPriority() { return priority; }

    /** The broad category of this event, derived from {@link AuditEvent}. */
    public AuditCategory getCategory() { return category; }

    /**
     * The human-readable name of whoever triggered the event.
     * May be a player name, {@code "CONSOLE"}, or {@code "SYSTEM"}.
     */
    public String getActor() { return actor; }

    /** What the action was performed on (crate ID, key ID, player name, etc.). */
    public String getTarget() { return target; }

    /**
     * Arbitrary structured key-value pairs for sink-specific rendering.
     * Examples: {@code world}, {@code amount}, {@code reason}, {@code reward}.
     */
    public Map<String, String> getData() { return data; }

    /** Server name for multi-server setups (from {@code logger.yml}). */
    public String getServerName() { return serverName; }

    /** World name where the event occurred, or empty if not applicable. */
    public String getWorldName() { return worldName; }

    // ── Factory ─────────────────────────────────────────────────────────────

    /**
     * Creates a new {@link Builder} for the given event type.
     *
     * @param event The event to record.
     * @return A new builder.
     */
    public static Builder builder(AuditEvent event) {
        return new Builder(event);
    }

    // ── Builder ─────────────────────────────────────────────────────────────

    /** Fluent builder for {@link AuditRecord}. */
    public static final class Builder {

        private final AuditEvent event;
        private AuditPriority priority;
        private String actor;
        private String target;
        private final Map<String, String> data = new HashMap<>();
        private String serverName;
        private String worldName;

        private Builder(AuditEvent event) {
            this.event = event;
        }

        /**
         * Overrides the default priority for this event.
         * Use only when the severity differs from the event's canonical level.
         */
        public Builder priority(AuditPriority priority) {
            this.priority = priority;
            return this;
        }

        /** Name of the player, or {@code "CONSOLE"} / {@code "SYSTEM"}. */
        public Builder actor(String actor) {
            this.actor = actor;
            return this;
        }

        /** ID of the crate, key, player, or other entity being acted upon. */
        public Builder target(String target) {
            this.target = target;
            return this;
        }

        /** Adds all entries from {@code map} to the record's data. */
        public Builder data(Map<String, String> map) {
            if (map != null) this.data.putAll(map);
            return this;
        }

        /** Adds a single key-value pair to the record's data. */
        public Builder data(String key, String value) {
            this.data.put(key, value);
            return this;
        }

        /** Server name to embed in the record (shown in Discord footer, etc.). */
        public Builder serverName(String serverName) {
            this.serverName = serverName;
            return this;
        }

        /** World name where the event occurred. */
        public Builder worldName(String worldName) {
            this.worldName = worldName;
            return this;
        }

        /** Builds the immutable {@link AuditRecord}. */
        public AuditRecord build() {
            return new AuditRecord(this);
        }
    }

    @Override
    public String toString() {
        return "AuditRecord{event=" + event + ", priority=" + priority
                + ", actor='" + actor + "', target='" + target + "', ts=" + timestamp + '}';
    }
}
