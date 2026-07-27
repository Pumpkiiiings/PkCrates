package com.pumpkings.pkcrates.presentation.menu.editor;

import com.pumpkings.pkcrates.core.model.Crate;
import com.pumpkings.pkcrates.core.model.key.IKey;
import com.pumpkings.pkcrates.infrastructure.config.CrateRegistry;
import com.pumpkings.pkcrates.infrastructure.key.KeyRegistry;
import com.pumpkings.pkcrates.presentation.menu.Button;
import com.pumpkings.pkcrates.presentation.menu.MenuManager;
import com.pumpkings.pkcrates.presentation.menu.PaginatedPkMenu;
import com.pumpkings.pkcrates.presentation.menu.PkMenu;
import com.pumpkings.pkcrates.presentation.utils.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import com.pumpkings.pkcrates.infrastructure.config.MenuConfig;
import java.util.HashMap;
import java.util.Map;

import java.util.ArrayList;
import java.util.List;

public class CrateKeysMenu extends PaginatedPkMenu<IKey> {

    private final Crate crate;
    private final CrateRegistry crateRegistry;
    private final KeyRegistry keyRegistry;
    private final PkMenu parentMenu;

    private final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    public CrateKeysMenu(MenuManager menuManager, Player player, Crate crate, CrateRegistry crateRegistry, KeyRegistry keyRegistry, PkMenu parentMenu) {
        super(menuManager, player);
        this.crate = crate;
        this.crateRegistry = crateRegistry;
        this.keyRegistry = keyRegistry;
        this.parentMenu = parentMenu;
    }

    @Override
    public net.kyori.adventure.text.Component getTitle() {
        MenuConfig config = menuManager.getMenuConfigManager().getMenu("crate_keys");
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("<crate_id>", crate.getId());
        return config != null ? config.getTitle(placeholders) : TextUtil.parse("<gold>Assign Keys to: <white>" + crate.getId());
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public List<IKey> getItems() {
        return new ArrayList<>(keyRegistry.getAllKeys());
    }

    @Override
    public int[] getItemSlots() {
        return SLOTS;
    }

    @Override
    public Button createItemButton(IKey key) {
        MenuConfig config = menuManager.getMenuConfigManager().getMenu("crate_keys");
        boolean isAccepted = crate.getAcceptedKeys().contains(key.getId());

        ItemStack item = key.getBaseItem().clone();
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("<key_id>", key.getId());
            
            if (!meta.hasDisplayName()) {
                if (config != null) {
                    meta.displayName(config.getItemName("items.key_format", placeholders));
                } else {
                    meta.displayName(TextUtil.parse("<yellow><bold>Key: <white>" + key.getId()));
                }
            }
            List<net.kyori.adventure.text.Component> lore = meta.lore();
            if (lore == null) lore = new ArrayList<>();
            
            if (config != null) {
                String path = isAccepted ? "items.key_format.active_lore" : "items.key_format.inactive_lore";
                lore.addAll(config.getExactLore(path, placeholders));
                if (isAccepted) {
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                }
            } else {
                lore.add(net.kyori.adventure.text.Component.empty());
                if (isAccepted) {
                    lore.add(TextUtil.parse("<green><bold>▶ ACTIVE"));
                    lore.add(TextUtil.parse("<gray>This crate can be opened with this key."));
                    lore.add(TextUtil.parse("<red>Click to unlink."));
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                } else {
                    lore.add(TextUtil.parse("<red><bold>▶ INACTIVE"));
                    lore.add(TextUtil.parse("<gray>This crate DOES NOT accept this key."));
                    lore.add(TextUtil.parse("<green>Click to link."));
                }
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }

        return new Button(item, event -> {
            if (isAccepted) {
                crate.removeAcceptedKey(key.getId());
            } else {
                crate.addAcceptedKey(key.getId());
            }
            crateRegistry.saveCrate(crate);
            decorate(); // Refresh
        });
    }

    @Override
    protected void addDecorations() {
        ItemStack bgItem = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta bgMeta = bgItem.getItemMeta();
        if (bgMeta != null) {
            bgMeta.displayName(net.kyori.adventure.text.Component.empty());
            bgItem.setItemMeta(bgMeta);
        }
        for (int i = 45; i <= 53; i++) {
            if (i != 49) {
                setButton(i, Button.visual(bgItem));
            }
        }
        
        MenuConfig config = menuManager.getMenuConfigManager().getMenu("crate_keys");
        
        // Back button
        Material backMat = config != null ? config.getItemMaterial("items.back", Material.ARROW) : Material.ARROW;
        ItemStack back = new ItemStack(backMat);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            if (config != null) {
                backMeta.displayName(config.getItemName("items.back", null));
                backMeta.lore(config.getItemLore("items.back", null));
            } else {
                backMeta.displayName(TextUtil.parse("<red>Back"));
            }
            back.setItemMeta(backMeta);
        }
        setButton(45, new Button(back, event -> {
            parentMenu.open();
        }));
    }
}
