package com.pumpkings.pkcrates.presentation.menu.editor;

import com.pumpkings.pkcrates.core.model.Crate;
import com.pumpkings.pkcrates.infrastructure.location.CrateLocationManager;
import com.pumpkings.pkcrates.presentation.menu.Button;
import com.pumpkings.pkcrates.presentation.menu.MenuManager;
import com.pumpkings.pkcrates.presentation.menu.PaginatedPkMenu;
import com.pumpkings.pkcrates.presentation.utils.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import com.pumpkings.pkcrates.infrastructure.config.MenuConfig;
import java.util.HashMap;
import java.util.Map;

import java.util.ArrayList;
import java.util.List;

public class CrateLocationMenu extends PaginatedPkMenu<Location> {

    private final Plugin plugin;
    private final Crate crate;
    private final CrateLocationManager locationMgr;
    private final com.pumpkings.pkcrates.infrastructure.display.HologramManager hologramManager;
    private final com.pumpkings.pkcrates.presentation.menu.PkMenu parentMenu;

    private final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    public CrateLocationMenu(Plugin plugin, MenuManager menuManager, Player player, Crate crate, CrateLocationManager locationMgr, com.pumpkings.pkcrates.infrastructure.display.HologramManager hologramManager, com.pumpkings.pkcrates.presentation.menu.PkMenu parentMenu) {
        super(menuManager, player);
        this.plugin = plugin;
        this.crate = crate;
        this.locationMgr = locationMgr;
        this.hologramManager = hologramManager;
        this.parentMenu = parentMenu;
    }

    @Override
    public Component getTitle() {
        MenuConfig config = menuManager.getMenuConfigManager().getMenu("crate_location");
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("<crate_id>", crate.getId());
        return config != null ? config.getTitle(placeholders) : TextUtil.parse("<dark_gray>Editor > <yellow>" + crate.getId() + " <dark_gray>> Blocks");
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public List<Location> getItems() {
        return locationMgr.getAllLocations(crate.getId());
    }

    @Override
    public int[] getItemSlots() {
        return SLOTS;
    }

    @Override
    public Button createItemButton(Location loc) {
        MenuConfig config = menuManager.getMenuConfigManager().getMenu("crate_location");
        Material locMat = config != null ? config.getItemMaterial("items.location_format", Material.BEDROCK) : Material.BEDROCK;
        ItemStack display = new ItemStack(locMat);
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("<world>", loc.getWorld() != null ? loc.getWorld().getName() : "Unknown");
            placeholders.put("<x>", String.valueOf(loc.getBlockX()));
            placeholders.put("<y>", String.valueOf(loc.getBlockY()));
            placeholders.put("<z>", String.valueOf(loc.getBlockZ()));
            
            if (config != null) {
                meta.displayName(config.getItemName("items.location_format", placeholders));
                meta.lore(config.getItemLore("items.location_format", placeholders));
            } else {
                meta.displayName(TextUtil.parse("<yellow><bold>Physical Block"));
                List<Component> lore = new ArrayList<>();
                lore.add(TextUtil.parse("<gray>World: <white>" + (loc.getWorld() != null ? loc.getWorld().getName() : "Unknown")));
                lore.add(TextUtil.parse("<gray>X: <white>" + loc.getBlockX()));
                lore.add(TextUtil.parse("<gray>Y: <white>" + loc.getBlockY()));
                lore.add(TextUtil.parse("<gray>Z: <white>" + loc.getBlockZ()));
                lore.add(Component.empty());
                lore.add(TextUtil.parse("<green>▶ Left-Click to teleport"));
                lore.add(TextUtil.parse("<red>▶ Shift-Right-Click to remove block"));
                meta.lore(lore);
            }
            display.setItemMeta(meta);
        }

        return new Button(display, event -> {
            if (event.getClick() == ClickType.SHIFT_RIGHT) {
                locationMgr.removeLocation(loc);
                hologramManager.removeFor(loc);
                player.sendRichMessage("<red>Physical block removed.");
                decorate();
            } else if (event.getClick() == ClickType.LEFT) {
                player.closeInventory();
                player.teleportAsync(loc.clone().add(0.5, 1.0, 0.5)).thenAccept(success -> {
                    if (success) {
                        player.sendRichMessage("<green>Teleported to the crate.");
                    } else {
                        player.sendRichMessage("<red>Error while teleporting.");
                    }
                });
            }
        });
    }

    @Override
    protected void addDecorations() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            filler.setItemMeta(meta);
        }
        Button bg = Button.visual(filler);

        for (int i = 0; i < 9; i++) setButton(i, bg);
        for (int i = 45; i < 54; i++) {
            if (i != 49) setButton(i, bg);
        }

        MenuConfig config = menuManager.getMenuConfigManager().getMenu("crate_location");
        
        // Back button
        Material backMat = config != null ? config.getItemMaterial("items.back", Material.ARROW) : Material.ARROW;
        ItemStack back = new ItemStack(backMat);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            if (config != null) {
                backMeta.displayName(config.getItemName("items.back", null));
                backMeta.lore(config.getItemLore("items.back", null));
            } else {
                backMeta.displayName(TextUtil.parse("<red>Return to Crate Editor"));
            }
            back.setItemMeta(backMeta);
        }
        setButton(45, new Button(back, event -> parentMenu.open()));
    }
}
