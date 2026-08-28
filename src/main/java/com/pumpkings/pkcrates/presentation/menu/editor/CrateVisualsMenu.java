package com.pumpkings.pkcrates.presentation.menu.editor;

import com.pumpkings.pkcrates.core.model.Crate;
import com.pumpkings.pkcrates.infrastructure.config.CrateRegistry;
import com.pumpkings.pkcrates.presentation.menu.Button;
import com.pumpkings.pkcrates.presentation.menu.MenuManager;
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

public class CrateVisualsMenu extends PkMenu {

    private final Plugin plugin;
    private final Crate crate;
    private final CrateRegistry crateRegistry;
    private final PkMenu parentMenu;

    public CrateVisualsMenu(Plugin plugin, MenuManager menuManager, Player player, Crate crate, CrateRegistry crateRegistry, PkMenu parentMenu) {
        super(menuManager, player);
        this.plugin = plugin;
        this.crate = crate;
        this.crateRegistry = crateRegistry;
        this.parentMenu = parentMenu;
    }

    @Override
    public net.kyori.adventure.text.Component getTitle() {
        MenuConfig config = menuManager.getMenuConfigManager().getMenu("crate_visuals");
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("<crate_id>", crate.getId());
        return config != null ? config.getTitle(placeholders) : TextUtil.parse("<dark_gray>Vɪꜱᴜᴀʟꜱ <gray>| <white>" + crate.getId());
    }

    @Override
    public int getSize() {
        return 27;
    }

    @Override
    public void decorate() {
        // Decorate background
        ItemStack bgItem = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bgMeta = bgItem.getItemMeta();
        if (bgMeta != null) {
            bgMeta.displayName(net.kyori.adventure.text.Component.empty());
            bgItem.setItemMeta(bgMeta);
        }
        Button bg = Button.visual(bgItem);
        for (int i = 0; i < getSize(); i++) {
            setButton(i, bg);
        }

        MenuConfig config = menuManager.getMenuConfigManager().getMenu("crate_visuals");

        // Ambient Effects
        Material ambientMat = config != null ? config.getItemMaterial("items.ambient", Material.PRISMARINE_CRYSTALS) : Material.PRISMARINE_CRYSTALS;
        ItemStack ambientItem = new ItemStack(ambientMat);
        ItemMeta ambientMeta = ambientItem.getItemMeta();
        if (ambientMeta != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("<ambient_id>", crate.getAmbientEffect().name());
            if (config != null) {
                ambientMeta.displayName(config.getItemName("items.ambient", placeholders));
                ambientMeta.lore(config.getItemLore("items.ambient", placeholders));
            } else {
                ambientMeta.displayName(TextUtil.parse("<color:#00CFFF><bold>ᴀᴍʙɪᴇɴᴛ ᴘᴀʀᴛɪᴄʟᴇꜱ"));
                java.util.List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>();
                lore.add(TextUtil.parse("<gray>ᴄᴜʀʀᴇɴᴛ ᴇꜰꜰᴇᴄᴛ: <white>" + crate.getAmbientEffect().name()));
                lore.add(net.kyori.adventure.text.Component.empty());
                lore.add(TextUtil.parse("<color:#00FF45>▶ ᴄʟɪᴄᴋ ᴛᴏ ꜱᴇʟᴇᴄᴛ."));
                ambientMeta.lore(lore);
            }
            ambientItem.setItemMeta(ambientMeta);
        }
        setButton(11, new Button(ambientItem, event -> {
            new CrateAmbientMenu(plugin, menuManager, player, crate, crateRegistry, this).open();
        }));

        // Opening Animations
        Material animMat = config != null ? config.getItemMaterial("items.animation", Material.FIREWORK_ROCKET) : Material.FIREWORK_ROCKET;
        ItemStack animItem = new ItemStack(animMat);
        ItemMeta animMeta = animItem.getItemMeta();
        if (animMeta != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("<animation_id>", crate.getAnimationId() != null ? crate.getAnimationId() : "None");
            if (config != null) {
                animMeta.displayName(config.getItemName("items.animation", placeholders));
                animMeta.lore(config.getItemLore("items.animation", placeholders));
            } else {
                animMeta.displayName(TextUtil.parse("<color:#FF00FF><bold>ᴏᴘᴇɴɪɴɢ ᴀɴɪᴍᴀᴛɪᴏɴ"));
                java.util.List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>();
                lore.add(TextUtil.parse("<gray>ᴄᴜʀʀᴇɴᴛ ᴀɴɪᴍᴀᴛɪᴏɴ: <white>" + (crate.getAnimationId() != null ? crate.getAnimationId() : "None")));
                lore.add(net.kyori.adventure.text.Component.empty());
                lore.add(TextUtil.parse("<color:#00FF45>▶ ᴄʟɪᴄᴋ ᴛᴏ ꜱᴇʟᴇᴄᴛ."));
                animMeta.lore(lore);
            }
            animItem.setItemMeta(animMeta);
        }
        setButton(15, new Button(animItem, event -> {
            new CrateAnimationMenu(plugin, menuManager, player, crate, crateRegistry, this).open();
        }));

        // Back button
        Material backMat = config != null ? config.getItemMaterial("items.back", Material.ARROW) : Material.ARROW;
        ItemStack back = new ItemStack(backMat);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            if (config != null) {
                backMeta.displayName(config.getItemName("items.back", null));
                backMeta.lore(config.getItemLore("items.back", null));
            } else {
                backMeta.displayName(TextUtil.parse("<color:#FF2626><bold>ʙᴀᴄᴋ"));
            }
            back.setItemMeta(backMeta);
        }
        setButton(22, new Button(back, event -> {
            parentMenu.open();
        }));
    }
}
