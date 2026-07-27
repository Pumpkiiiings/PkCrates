package com.pumpkings.pkcrates.presentation.listener;

import com.pumpkings.pkcrates.presentation.menu.Button;
import com.pumpkings.pkcrates.presentation.menu.PkMenu;
import com.pumpkings.pkcrates.presentation.menu.MenuManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class MenuListener implements Listener {

    private final MenuManager menuManager;

    public MenuListener(MenuManager menuManager) {
        this.menuManager = menuManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        PkMenu menu = menuManager.getOpenMenu(player.getUniqueId());
        if (menu == null) return;

        // If the clicked inventory is ours or theirs, we cancel if the menu requires it
        // to prevent shift-clicks that insert or extract items, except for specific logic.
        if (menu.isCancelClicks()) {
            event.setCancelled(true);
        }

        if (event.getClickedInventory() == null) return;
        
        // If they clicked in THEIR inventory, we call onBottomClick
        if (event.getClickedInventory().equals(event.getView().getBottomInventory())) {
            menu.onBottomClick(event);
            return;
        }

        int slot = event.getSlot();
        Button button = menu.getButton(slot);
        
        if (button != null) {
            button.onClick(event);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        PkMenu menu = menuManager.getOpenMenu(player.getUniqueId());
        if (menu != null) {
            menu.onClose();
            menuManager.unregisterMenu(player.getUniqueId());
        }
    }
}
