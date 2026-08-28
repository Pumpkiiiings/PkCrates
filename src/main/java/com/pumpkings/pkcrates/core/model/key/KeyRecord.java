package com.pumpkings.pkcrates.core.model.key;

import org.bukkit.inventory.ItemStack;

public class KeyRecord implements IKey {

    private final String id;
    private boolean isVirtual;
    private ItemStack baseItem;
    
    private RarityRestriction rarityRestriction;
    private String rarityTarget;

    public KeyRecord(String id, boolean isVirtual, ItemStack baseItem) {
        this.id = id;
        this.isVirtual = isVirtual;
        this.baseItem = baseItem;
        this.rarityRestriction = RarityRestriction.ANY;
        this.rarityTarget = "";
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isVirtual() {
        return isVirtual;
    }

    public void setVirtual(boolean virtual) {
        isVirtual = virtual;
    }

    @Override
    public ItemStack getBaseItem() {
        return baseItem != null ? baseItem.clone() : null;
    }

    public void setBaseItem(ItemStack baseItem) {
        this.baseItem = baseItem;
    }

    @Override
    public RarityRestriction getRarityRestriction() {
        return rarityRestriction;
    }

    @Override
    public void setRarityRestriction(RarityRestriction rarityRestriction) {
        this.rarityRestriction = rarityRestriction;
    }

    @Override
    public String getRarityTarget() {
        return rarityTarget;
    }

    @Override
    public void setRarityTarget(String rarityTarget) {
        this.rarityTarget = rarityTarget;
    }
}
