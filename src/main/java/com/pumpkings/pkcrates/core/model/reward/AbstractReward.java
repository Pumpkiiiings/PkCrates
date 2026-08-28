package com.pumpkings.pkcrates.core.model.reward;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractReward implements IReward {

    protected final String id;
    protected double weight;
    protected ItemStack displayItem;
    protected int limit;
    protected IReward fallback;
    protected String rarityId;
    protected boolean broadcastEnabled;

    public AbstractReward(String id, double weight, ItemStack displayItem, int limit, @Nullable IReward fallback) {
        this.id = id;
        this.weight = weight;
        this.displayItem = displayItem;
        this.limit = limit;
        this.fallback = fallback;
        this.broadcastEnabled = false;
    }

    @Override
    public String getRarityId() {
        return rarityId;
    }

    @Override
    public void setRarityId(@Nullable String rarityId) {
        this.rarityId = rarityId;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public double getWeight() {
        return weight;
    }

    @Override
    public ItemStack getPreviewItem() {
        if (displayItem == null) return null;
        ItemStack clone = displayItem.clone();
        
        if (rarityId != null) {
            org.bukkit.plugin.RegisteredServiceProvider<com.pumpkings.pkcrates.api.rarity.RarityService> rsp = 
                org.bukkit.Bukkit.getServer().getServicesManager().getRegistration(com.pumpkings.pkcrates.api.rarity.RarityService.class);
            if (rsp != null) {
                com.pumpkings.pkcrates.api.rarity.RarityService rarityService = rsp.getProvider();
                com.pumpkings.pkcrates.core.model.rarity.Rarity rarity = rarityService.get(rarityId);
                
                if (rarity != null) {
                    org.bukkit.inventory.meta.ItemMeta meta = clone.getItemMeta();
                    if (meta != null) {
                        if (rarity.isGlow()) {
                            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                        }
                        
                        if (meta.hasDisplayName()) {
                            String originalName = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().serialize(meta.displayName());
                            String format = rarity.getMiniMessageFormat() != null ? rarity.getMiniMessageFormat() : "{color}{text}";
                            String color = rarity.getColor() != null ? rarity.getColor() : "";
                            
                            String newName = format.replace("{color}", color).replace("{text}", originalName);
                            meta.displayName(com.pumpkings.pkcrates.presentation.utils.TextUtil.parse(newName));
                        }
                        
                        clone.setItemMeta(meta);
                    }
                }
            }
        }
        
        return clone;
    }

    @Override
    public @Nullable IReward getFallbackReward() {
        return fallback;
    }

    @Override
    public boolean canWin(Player player) {
        if (limit > 0) {
            int currentWins = com.pumpkings.pkcrates.infrastructure.cache.LimitManager.getInstance().getWins(player.getUniqueId(), id);
            if (currentWins >= limit) {
                return false;
            }
        }
        return true; 
    }
    
    public void setWeight(double weight) {
        this.weight = weight;
    }
    
    public void setLimit(int limit) {
        this.limit = limit;
    }
    
    public void setDisplayItem(ItemStack item) {
        this.displayItem = item;
    }
    
    @Override
    public int getWinLimit() {
        return limit;
    }

    @Override
    public void give(Player player) {
        if (!canWin(player) && fallback != null) {
            fallback.give(player);
            return;
        }
        
        // Register in cache that the player won this reward
        recordWin(player);
        
        // Execute concrete logic (give items/commands)
        executeGive(player);
    }

    /**
     * Specific delivery logic implemented by the child class (e.g. UnifiedReward).
     */
    protected abstract void executeGive(Player player);

    /**
     * Logic to register in the cache that the player won the reward.
     */
    protected abstract void recordWin(Player player);
    
    @Override
    public boolean isBroadcastEnabled() {
        return broadcastEnabled;
    }
    
    @Override
    public void setBroadcastEnabled(boolean broadcastEnabled) {
        this.broadcastEnabled = broadcastEnabled;
    }
}
