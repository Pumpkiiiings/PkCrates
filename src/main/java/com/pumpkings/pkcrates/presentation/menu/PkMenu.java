package com.pumpkings.pkcrates.presentation.menu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract mother class to create robust menus (GUI).
 */
public abstract class PkMenu implements InventoryHolder {

    protected final MenuManager menuManager;
    protected final Player player;
    protected Inventory inventory;
    protected final Map<Integer, Button> buttons;

    public PkMenu(MenuManager menuManager, Player player) {
        this.menuManager = menuManager;
        this.player = player;
        this.buttons = new HashMap<>();
    }

    /**
     * Defines the inventory title (preferably using parsed MiniMessage).
     */
    public abstract net.kyori.adventure.text.Component getTitle();

    /**
     * Defines the inventory size (multiple of 9).
     */
    public abstract int getSize();

    /**
     * Logic where buttons are configured before opening the menu.
     */
    public abstract void decorate();

    /**
     * Logic to execute if the inventory is closed (Optional).
     */
    public void onClose() {
        // Can be overridden
    }
    
    /**
     * If it returns true, all clicks are canceled automatically.
     */
    public boolean isCancelClicks() {
        return true;
    }

    /**
     * Logic to execute if clicked on their own inventory.
     */
    public void onBottomClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        // Can be overridden
    }

    public void open() {
        inventory = Bukkit.createInventory(this, getSize(), getTitle());
        buttons.clear();
        decorate();

        for (Map.Entry<Integer, Button> entry : buttons.entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue().getItem());
        }

        player.openInventory(inventory);
        menuManager.registerOpenMenu(player.getUniqueId(), this);
    }

    protected void setButton(int slot, Button button) {
        if (button == null) {
            buttons.remove(slot);
            if (inventory != null) {
                inventory.setItem(slot, null);
            }
        } else {
            buttons.put(slot, button);
            if (inventory != null) {
                inventory.setItem(slot, button.getItem());
            }
        }
    }

    public Button getButton(int slot) {
        return buttons.get(slot);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
