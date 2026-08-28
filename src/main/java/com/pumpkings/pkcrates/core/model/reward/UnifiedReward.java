package com.pumpkings.pkcrates.core.model.reward;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class UnifiedReward extends AbstractReward {

    private List<ItemStack> winItems;
    private List<String> winCommands;

    public UnifiedReward(String id, double weight, ItemStack displayItem, int limit, @Nullable IReward fallback, List<ItemStack> winItems, List<String> winCommands) {
        super(id, weight, displayItem, limit, fallback);
        this.winItems = winItems == null ? new ArrayList<>() : winItems;
        this.winCommands = winCommands == null ? new ArrayList<>() : winCommands;
    }

    @Override
    protected void executeGive(Player player) {
        // 1. Give items
        for (ItemStack item : winItems) {
            // The item is given to the player. If the inventory is full, it drops to the ground.
            player.getInventory().addItem(item.clone()).values().forEach(
                    leftover -> player.getWorld().dropItem(player.getLocation(), leftover)
            );
        }

        // 2. Execute commands (replacing %player% with real name)
        for (String command : winCommands) {
            String parsedCommand = command.replace("%player%", player.getName());
            // Commands are executed as console to guarantee permissions
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCommand);
        }
    }

    @Override
    protected void recordWin(Player player) {
        com.pumpkings.pkcrates.infrastructure.cache.LimitManager.getInstance().addWin(player.getUniqueId(), id);
    }
    
    public List<String> getCommands() {
        return new ArrayList<>(winCommands);
    }
    
    public void setCommands(List<String> winCommands) {
        this.winCommands = new ArrayList<>(winCommands);
    }

    public List<ItemStack> getItems() {
        return new ArrayList<>(winItems);
    }
    
    public void setItems(List<ItemStack> winItems) {
        this.winItems = new ArrayList<>(winItems);
    }
    
    public ItemStack getDisplayItem() {
        return displayItem;
    }
}
