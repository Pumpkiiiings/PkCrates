package com.pumpkings.pkcrates.presentation.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Reusable paginated menu.
 * @param <T> The type of object to iterate in the paginated list.
 */
public abstract class PaginatedPkMenu<T> extends PkMenu {

    protected int page = 0;
    protected int maxItemsPerPage = 21; // 3x7 grid

    public PaginatedPkMenu(MenuManager menuManager, Player player) {
        super(menuManager, player);
    }

    /**
     * @return The full list of objects to paginate.
     */
    public abstract List<T> getItems();

    /**
     * @return The slots where the iterated objects will be placed.
     */
    public abstract int[] getItemSlots();

    /**
     * Converts the object into a functional button for the corresponding slot.
     */
    public abstract Button createItemButton(T item);

    @Override
    public void decorate() {
        // Render base decoration (borders, background)
        addDecorations();

        // Calculate pagination
        List<T> allItems = getItems();
        int[] slots = getItemSlots();
        this.maxItemsPerPage = slots.length;

        int totalPages = (int) Math.ceil((double) allItems.size() / maxItemsPerPage);
        if (totalPages == 0) totalPages = 1;

        // Limits
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        // Render items
        int startIndex = page * maxItemsPerPage;
        for (int i = 0; i < maxItemsPerPage; i++) {
            int itemIndex = startIndex + i;
            int slot = slots[i];
            
            if (itemIndex < allItems.size()) {
                setButton(slot, createItemButton(allItems.get(itemIndex)));
            } else {
                setButton(slot, null);
                if (inventory != null) inventory.setItem(slot, new ItemStack(Material.AIR));
            }
        }

        // Render control buttons
        renderControls(totalPages);
    }

    protected void addDecorations() {
        // Optionally overridable
    }

    protected void renderControls(int totalPages) {
        // Previous
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta meta = prev.getItemMeta();
            meta.displayName(com.pumpkings.pkcrates.presentation.utils.TextUtil.parse("<red>Previous Page"));
            prev.setItemMeta(meta);
            
            setButton(18, new Button(prev, e -> {
                page--;
                decorate();
            }));
        } else {
            setButton(18, null);
            if (inventory != null) inventory.setItem(18, new ItemStack(Material.AIR));
        }

        // Next
        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta meta = next.getItemMeta();
            meta.displayName(com.pumpkings.pkcrates.presentation.utils.TextUtil.parse("<green>Next Page"));
            next.setItemMeta(meta);
            
            setButton(26, new Button(next, e -> {
                page++;
                decorate();
            }));
        } else {
            setButton(26, null);
            if (inventory != null) inventory.setItem(26, new ItemStack(Material.AIR));
        }

        if (buttons.get(49) == null) {
            ItemStack close = new ItemStack(Material.BARRIER);
            ItemMeta closeMeta = close.getItemMeta();
            closeMeta.displayName(com.pumpkings.pkcrates.presentation.utils.TextUtil.parse("<red>Close Menu"));
            close.setItemMeta(closeMeta);
            
            setButton(49, new Button(close, e -> {
                player.closeInventory();
            }));
        }
    }
}
