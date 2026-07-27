package com.pumpkings.pkcrates.core.model.claim;

/**
 * Represents the reason why a reward was stored in the claim system
 * instead of being delivered directly to the player.
 *
 * <p>Designed to be extensible — future reasons (e.g. WORLD_GUARD_DENIED)
 * can be added without breaking existing logic.</p>
 */
public enum ClaimReason {

    /** The player's inventory had no available slots at delivery time. */
    INVENTORY_FULL,

    /** The player was offline when the reward was generated. */
    PLAYER_OFFLINE,

    /** An administrator or plugin forced the reward into the claim system. */
    FORCED,

    /** An unexpected error occurred during the normal delivery attempt. */
    DELIVERY_ERROR
}
