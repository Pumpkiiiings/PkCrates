package com.pumpkings.pkcrates.infrastructure.permission;

import org.bukkit.permissions.Permissible;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic utility for resolving a numeric limit from a player's permissions.
 *
 * <h3>Design</h3>
 * <p>The resolver scans the {@link Permissible}'s effective permissions for any
 * that match the configured prefix followed by a positive integer.  The
 * <em>highest</em> matched value is returned.  Special "unlimited" and
 * "admin override" permissions always win regardless of numeric values.</p>
 *
 * <h3>Example usage</h3>
 * <pre>{@code
 * PermissionLimitResolver claimLimits = PermissionLimitResolver.builder("pkcrates.claim.limit.")
 *         .unlimitedPermission("pkcrates.claim.limit.unlimited")
 *         .adminOverride("pkcrates.admin")
 *         .defaultLimit(10)
 *         .build();
 *
 * int max = claimLimits.resolve(player);   // -1 means unlimited
 * boolean ok = claimLimits.isUnlimited(player);
 * }</pre>
 *
 * <h3>Reusability</h3>
 * <p>Any feature that needs permission-based numeric limits (keys, openings,
 * crate slots, etc.) can create its own {@code PermissionLimitResolver} with
 * a different prefix and admin override list.</p>
 */
public final class PermissionLimitResolver {

    /** Sentinel value meaning "no upper limit". */
    public static final int UNLIMITED = -1;

    private final String prefix;
    private final List<String> unlimitedPermissions;
    private final int defaultLimit;

    private PermissionLimitResolver(Builder builder) {
        this.prefix = builder.prefix;
        this.unlimitedPermissions = List.copyOf(builder.unlimitedPermissions);
        this.defaultLimit = builder.defaultLimit;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Resolves the effective limit for the given permissible.
     *
     * <ol>
     *   <li>If any "unlimited" permission is held → returns {@link #UNLIMITED} ({@code -1}).</li>
     *   <li>Scans all effective permissions matching {@code prefix + <integer>} and
     *       returns the highest found value.</li>
     *   <li>If no matching permission is found → returns {@link #defaultLimit}.</li>
     * </ol>
     *
     * @param permissible The player (or any {@link Permissible}) to evaluate.
     * @return The resolved limit, or {@link #UNLIMITED} ({@code -1}) for no limit.
     */
    public int resolve(Permissible permissible) {
        // Check unlimited overrides first (fast path)
        for (String perm : unlimitedPermissions) {
            if (permissible.hasPermission(perm)) {
                return UNLIMITED;
            }
        }

        // Scan all effective permissions for the numeric suffix
        int highest = Integer.MIN_VALUE;

        for (var entry : permissible.getEffectivePermissions()) {
            if (!entry.getValue()) continue; // skip negated permissions

            String name = entry.getPermission().toLowerCase();
            if (!name.startsWith(prefix.toLowerCase())) continue;

            String suffix = name.substring(prefix.length());
            try {
                int value = Integer.parseInt(suffix);
                if (value > highest) {
                    highest = value;
                }
            } catch (NumberFormatException ignored) {
                // Not a numeric permission — skip silently
            }
        }

        return highest > Integer.MIN_VALUE ? highest : defaultLimit;
    }

    /**
     * Convenience method: returns {@code true} if {@link #resolve} would
     * return {@link #UNLIMITED}.
     *
     * @param permissible The player to evaluate.
     * @return {@code true} if the player has no effective upper limit.
     */
    public boolean isUnlimited(Permissible permissible) {
        return resolve(permissible) == UNLIMITED;
    }

    /**
     * Returns {@code true} if the player has NOT yet reached their limit.
     *
     * @param permissible  The player to evaluate.
     * @param currentCount The player's current item count.
     * @return {@code true} if {@code currentCount < limit} (or limit is unlimited).
     */
    public boolean hasCapacity(Permissible permissible, int currentCount) {
        int limit = resolve(permissible);
        return limit == UNLIMITED || currentCount < limit;
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    /**
     * Creates a new builder for a {@link PermissionLimitResolver}.
     *
     * @param prefix The permission prefix to scan for (e.g. {@code "pkcrates.claim.limit."}).
     *               Must end with a dot.
     * @return A new {@link Builder} instance.
     */
    public static Builder builder(String prefix) {
        return new Builder(prefix);
    }

    /** Fluent builder for {@link PermissionLimitResolver}. */
    public static final class Builder {

        private final String prefix;
        private final List<String> unlimitedPermissions = new ArrayList<>();
        private int defaultLimit = UNLIMITED;

        private Builder(String prefix) {
            if (prefix == null || prefix.isEmpty()) {
                throw new IllegalArgumentException("Prefix must not be null or empty.");
            }
            this.prefix = prefix.endsWith(".") ? prefix : prefix + ".";
        }

        /**
         * Adds a permission that, when held, grants unlimited access regardless
         * of any numeric permissions.
         *
         * <p>May be called multiple times to add multiple override permissions.</p>
         *
         * @param permission The full permission node (e.g. {@code "pkcrates.admin"}).
         * @return This builder.
         */
        public Builder unlimitedPermission(String permission) {
            if (permission != null && !permission.isBlank()) {
                unlimitedPermissions.add(permission);
            }
            return this;
        }

        /**
         * Sets the fallback limit returned when the player has no matching
         * permission.  Use {@link PermissionLimitResolver#UNLIMITED} ({@code -1})
         * for no default cap.
         *
         * @param limit The default limit value.
         * @return This builder.
         */
        public Builder defaultLimit(int limit) {
            this.defaultLimit = limit;
            return this;
        }

        /**
         * Builds an immutable {@link PermissionLimitResolver}.
         *
         * @return The configured resolver.
         */
        public PermissionLimitResolver build() {
            return new PermissionLimitResolver(this);
        }
    }
}
