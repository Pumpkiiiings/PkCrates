package com.pumpkings.pkcrates.api.rarity;

import com.pumpkings.pkcrates.core.model.rarity.Rarity;
import java.util.Collection;

/**
 * Public API for managing rarities in PkCrates.
 */
public interface RarityService {

    /**
     * Creates a new rarity and registers it.
     * Fires RarityCreateEvent.
     *
     * @param id The unique identifier of the rarity.
     * @return The created Rarity, or null if it already exists or was cancelled.
     */
    Rarity create(String id);

    /**
     * Deletes an existing rarity.
     * Fires RarityDeleteEvent.
     *
     * @param id The unique identifier of the rarity.
     * @return True if deleted successfully, false otherwise.
     */
    boolean delete(String id);

    /**
     * Updates an existing rarity.
     * Fires RarityUpdateEvent.
     *
     * @param rarity The modified rarity.
     * @return True if updated successfully, false otherwise.
     */
    boolean update(Rarity rarity);

    /**
     * Gets a rarity by its ID.
     *
     * @param id The identifier.
     * @return The Rarity, or null if not found.
     */
    Rarity get(String id);

    /**
     * Gets all registered rarities.
     *
     * @return A collection of all rarities.
     */
    Collection<Rarity> getAll();

    /**
     * Checks if a rarity exists.
     *
     * @param id The identifier.
     * @return True if it exists.
     */
    boolean exists(String id);
}
