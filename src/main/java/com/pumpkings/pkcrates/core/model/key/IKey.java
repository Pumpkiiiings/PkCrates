package com.pumpkings.pkcrates.core.model.key;

import org.bukkit.inventory.ItemStack;

public interface IKey {

    /**
     * @return The unique identifier for this key.
     */
    String getId();

    /**
     * @return true if the key is purely virtual (database), false if physical.
     */
    boolean isVirtual();
    void setVirtual(boolean virtual);

    /**
     * @return The visual ItemStack of this key (without PDC injection, which is done by KeyService).
     */
    ItemStack getBaseItem();
    void setBaseItem(ItemStack baseItem);
    
    /**
     * @return The type of rarity restriction applied to this key.
     */
    RarityRestriction getRarityRestriction();

    void setRarityRestriction(RarityRestriction restriction);

    /**
     * @return The target rarity ID or priority threshold, depending on the restriction.
     */
    String getRarityTarget();

    void setRarityTarget(String target);

    enum RarityRestriction {
        ANY,
        SPECIFIC_LIST,
        MINIMUM_PRIORITY,
        EXACT
    }
}
