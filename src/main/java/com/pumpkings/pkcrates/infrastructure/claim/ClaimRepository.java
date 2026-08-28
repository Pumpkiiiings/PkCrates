package com.pumpkings.pkcrates.infrastructure.claim;

import com.pumpkings.pkcrates.core.model.claim.ClaimedReward;

import java.util.List;
import java.util.UUID;

/**
 * Persistence port for the Crate Claim system.
 *
 * <p>All implementations must be interchangeable — the service layer depends
 * only on this interface, never on a concrete class (YAML, SQLite, MySQL).</p>
 *
 * <p>Implementations are responsible for thread-safety and data integrity.</p>
 */
public interface ClaimRepository {

    /**
     * Persists a new or updated {@link ClaimedReward}.
     *
     * @param claim The claim to save.
     */
    void save(ClaimedReward claim);

    /**
     * Removes a specific claim entry from storage.
     *
     * @param playerUuid UUID of the owning player.
     * @param claimId    UUID of the claim to remove.
     */
    void delete(UUID playerUuid, UUID claimId);

    /**
     * Returns all pending claims for a player, sorted newest-first.
     *
     * @param playerUuid UUID of the player to query.
     * @return Immutable list; never {@code null}.
     */
    List<ClaimedReward> findByPlayer(UUID playerUuid);

    /**
     * Removes all claims for a specific player.
     *
     * @param playerUuid UUID of the player whose claims to purge.
     */
    void deleteAll(UUID playerUuid);

    /**
     * Removes all claims for every player.
     * Intended for administrative reset operations only.
     */
    void deleteAllPlayers();

    /**
     * Writes any buffered state to the underlying store and blocks until done.
     *
     * <p>Called from {@code onDisable}. Implementations that write through on every
     * mutation may leave this as a no-op.</p>
     */
    default void flush() {
        // No-op by default: write-through implementations have nothing buffered.
    }
}
