package com.pumpkings.pkcrates.presentation.menu;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public class Button {

    private final ItemStack item;
    private final Consumer<InventoryClickEvent> clickAction;

    public Button(ItemStack item, Consumer<InventoryClickEvent> clickAction) {
        this.item = item;
        this.clickAction = clickAction;
    }

    public ItemStack getItem() {
        return item != null ? item.clone() : null;
    }

    public void onClick(InventoryClickEvent event) {
        if (clickAction != null) {
            clickAction.accept(event);
        }
    }
    
    /**
     * Creates a static visual button (no action on click).
     */
    public static Button visual(ItemStack item) {
        return new Button(item, event -> {});
    }
}
