package com.pumpkings.pkcrates.presentation.menu.editor;

import com.pumpkings.pkcrates.core.model.Crate;
import com.pumpkings.pkcrates.infrastructure.config.CrateRegistry;
import com.pumpkings.pkcrates.presentation.menu.Button;
import com.pumpkings.pkcrates.presentation.menu.MenuManager;
import com.pumpkings.pkcrates.presentation.menu.PaginatedPkMenu;
import com.pumpkings.pkcrates.presentation.menu.PkMenu;
import com.pumpkings.pkcrates.presentation.utils.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.pumpkings.pkcrates.PkCratesPlugin;
import org.bukkit.plugin.Plugin;
import com.pumpkings.pkcrates.infrastructure.config.MenuConfig;
import java.util.HashMap;
import java.util.Map;

import java.util.ArrayList;
import java.util.List;

public class CrateAnimationMenu extends PaginatedPkMenu<String> {

    private final PkCratesPlugin plugin;
    private final Crate crate;
    private final CrateRegistry crateRegistry;
    private final PkMenu parentMenu;

    private final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    public CrateAnimationMenu(Plugin plugin, MenuManager menuManager, Player player, Crate crate, CrateRegistry crateRegistry, PkMenu parentMenu) {
        super(menuManager, player);
        this.plugin = (PkCratesPlugin) plugin;
        this.crate = crate;
        this.crateRegistry = crateRegistry;
        this.parentMenu = parentMenu;
    }

    @Override
    public net.kyori.adventure.text.Component getTitle() {
        MenuConfig config = menuManager.getMenuConfigManager().getMenu("crate_animation");
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("<crate_id>", crate.getId());
        return config != null ? config.getTitle(placeholders) : TextUtil.parse("<dark_gray>Animations for: <white>" + crate.getId());
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public List<String> getItems() {
        // We get all animations, and add "NONE" to be able to remove it
        List<String> list = new ArrayList<>();
        list.add("NONE");
        list.addAll(plugin.getAnimationRegistry().getRegisteredAnimations());
        return list;
    }

    @Override
    public int[] getItemSlots() {
        return SLOTS;
    }

    @Override
    public Button createItemButton(String animationId) {
        MenuConfig config = menuManager.getMenuConfigManager().getMenu("crate_animation");
        boolean isSelected = (crate.getAnimationId() == null && animationId.equals("NONE")) || 
                             (crate.getAnimationId() != null && crate.getAnimationId().equalsIgnoreCase(animationId));

        ItemStack item = new ItemStack(isSelected ? Material.FIREWORK_STAR : Material.GUNPOWDER);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("<animation_id>", animationId);
            if (config != null) {
                meta.displayName(config.getItemName("items.animation_format", placeholders));
                String path = isSelected ? "items.animation_format.active_lore" : "items.animation_format.inactive_lore";
                List<net.kyori.adventure.text.Component> lore = config.getExactLore(path, placeholders);
                meta.lore(lore);
                if (isSelected) {
                    meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                }
            }
            item.setItemMeta(meta);
        }

        return new Button(item, event -> {
            if (!isSelected) {
                if (animationId.equals("NONE")) {
                    crate.setAnimationId(null);
                } else {
                    crate.setAnimationId(animationId);
                }
                crateRegistry.saveCrate(crate);
                player.sendRichMessage("<green>Animation updated to: " + animationId);
                decorate(); // Refresh
            }
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
        
        MenuConfig config = menuManager.getMenuConfigManager().getMenu("crate_animation");

        // Back button
        Material backMat = config != null ? config.getItemMaterial("items.back", Material.ARROW) : Material.ARROW;
        ItemStack back = new ItemStack(backMat);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            if (config != null) {
                backMeta.displayName(config.getItemName("items.back", null));
                backMeta.lore(config.getItemLore("items.back", null));
            }
            back.setItemMeta(backMeta);
        }
        setButton(45, new Button(back, event -> {
            parentMenu.open();
        }));
    }
}
