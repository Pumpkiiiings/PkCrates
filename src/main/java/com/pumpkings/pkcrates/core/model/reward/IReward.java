package com.pumpkings.pkcrates.core.model.reward;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface IReward {

    /**
     * @return The unique identifier of this reward in the crate.
     */
    String getId();

    /**
     * @return The relative weight to calculate chance.
     */
    double getWeight();

    /**
     * @return The item to be displayed in the roulette/crate interface.
     */
    ItemStack getPreviewItem();

    /**
     * Checks if the player meets the requirements and limits to win this.
     * @param player Player to evaluate.
     * @return true if they can win it.
     */
    boolean canWin(Player player);

    /**
     * Executes the delivery of this reward (gives items, runs commands).
     * @param player The winning player.
     */
    void give(Player player);

    /**
     * @return true if a global broadcast should be sent when this reward is won.
     */
    boolean isBroadcastEnabled();

    void setBroadcastEnabled(boolean broadcastEnabled);

    /**
     * @return Alternative reward in case canWin() is false.
     */
    @Nullable IReward getFallbackReward();

    /**
     * @return The win limit for this reward.
     */
    int getWinLimit();

    /**
     * @return The Rarity ID linked to this reward, or null if independent.
     */
    @Nullable String getRarityId();

    /**
     * Assigns a Rarity to this reward.
     * @param rarityId The Rarity ID.
     */
    void setRarityId(@Nullable String rarityId);
}
