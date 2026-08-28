package com.pumpkings.pkcrates.infrastructure.claim;

/**
 * Exposes the {@code claim:} section of {@code config.yml}.
 *
 * <p>Populated fully in Etapa 2 ({@code YamlClaimConfig}). This interface
 * is declared here so {@link com.pumpkings.pkcrates.core.service.ClaimServiceImpl}
 * can compile without depending on Bukkit configuration classes.</p>
 */
public interface ClaimConfig {

    /** @return {@code true} if the claim module is active. */
    boolean isEnabled();

    /**
     * @return {@code true} if rewards should be stored when the player's
     *         inventory is full at delivery time.
     */
    boolean storeIfInventoryFull();

    /**
     * @return {@code true} if rewards should be stored when the player
     *         is offline at the time of reward generation.
     */
    boolean storeIfPlayerOffline();

    /**
     * @return {@code true} if rewards should be stored when any delivery
     *         error occurs.
     */
    boolean storeIfDeliveryFailed();

    /**
     * @return Maximum number of claims a player may have stored at once.
     *         {@code -1} means unlimited.
     */
    int getMaxStored();

    /**
     * @return {@code true} if players should be notified on login
     *         when they have pending claims.
     */
    boolean notifyOnLogin();

    /**
     * @return {@code true} if players should be notified immediately
     *         when a reward is stored for them.
     */
    boolean notifyOnStore();
}
