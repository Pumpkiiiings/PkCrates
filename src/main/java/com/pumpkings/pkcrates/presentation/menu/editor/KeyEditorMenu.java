package com.pumpkings.pkcrates.presentation.menu.editor;

import com.pumpkings.pkcrates.core.model.key.IKey;
import com.pumpkings.pkcrates.core.model.key.KeyRecord;
import com.pumpkings.pkcrates.infrastructure.key.KeyRegistry;
import com.pumpkings.pkcrates.presentation.menu.Button;
import com.pumpkings.pkcrates.presentation.menu.MenuManager;
import com.pumpkings.pkcrates.presentation.menu.PkMenu;
import com.pumpkings.pkcrates.presentation.utils.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.pumpkings.pkcrates.infrastructure.config.MenuConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class KeyEditorMenu extends PkMenu {

    private final KeyRegistry keyRegistry;
    private final IKey key;

    public KeyEditorMenu(MenuManager menuManager, Player player, KeyRegistry keyRegistry, IKey key) {
        super(menuManager, player);
        this.keyRegistry = keyRegistry;
        this.key = key;
    }

    @Override
    public Component getTitle() {
        MenuConfig config = menuManager.getMenuConfigManager().getMenu("key_editor");
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("<key_id>", key.getId());
        return config != null ? config.getTitle(placeholders) : TextUtil.parse("<dark_gray>Editing Key: <green>" + key.getId());
    }

    @Override
    public int getSize() {
        return 36;
    }

    @Override
    public void decorate() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            filler.setItemMeta(meta);
        }
        Button bg = Button.visual(filler);
        for (int i = 0; i < getSize(); i++) setButton(i, bg);

        MenuConfig config = menuManager.getMenuConfigManager().getMenu("key_editor");

        // Base Item
        ItemStack baseItem = key.getBaseItem();
        if (baseItem == null) baseItem = new ItemStack(Material.TRIPWIRE_HOOK);
        else baseItem = baseItem.clone();

        ItemMeta baseMeta = baseItem.getItemMeta();
        if (baseMeta != null) {
            List<Component> lore = baseMeta.lore();
            if (lore == null) lore = new ArrayList<>();
            if (config != null) {
                lore.addAll(config.getExactLore("items.base_item.lore_append", null));
            } else {
                lore.add(Component.empty());
                lore.add(TextUtil.parse("<yellow>↑ Current base item of the key ↑"));
                lore.add(TextUtil.parse("<gray>Click here with an item from your inventory"));
                lore.add(TextUtil.parse("<gray>to change its material, name, and texture."));
            }
            baseMeta.lore(lore);
            baseItem.setItemMeta(baseMeta);
        }
        setButton(11, new Button(baseItem, event -> {
            ItemStack cursor = event.getCursor();
            if (cursor != null && !cursor.getType().isAir()) {
                key.setBaseItem(cursor.clone());
                keyRegistry.updateKey(key);
                player.sendRichMessage("<green>Key material updated.");
                new KeyEditorMenu(menuManager, player, keyRegistry, key).open();
            } else {
                player.sendRichMessage("<red>Hold an item in your cursor and click here.");
            }
        }));

        // Rename Visible Name
        Material renameMat = config != null ? config.getItemMaterial("items.rename", Material.NAME_TAG) : Material.NAME_TAG;
        ItemStack nameIcon = new ItemStack(renameMat);
        ItemMeta nameMeta = nameIcon.getItemMeta();
        if (nameMeta != null) {
            if (config != null) {
                nameMeta.displayName(config.getItemName("items.rename", null));
                nameMeta.lore(config.getItemLore("items.rename", null));
            } else {
                nameMeta.displayName(TextUtil.parse("<yellow><bold>Rename Key"));
                List<Component> lore = new ArrayList<>();
                lore.add(TextUtil.parse("<gray>Change the visual name of the key."));
                lore.add(Component.empty());
                lore.add(TextUtil.parse("<green>▶ Click to rename."));
                nameMeta.lore(lore);
            }
            nameIcon.setItemMeta(nameMeta);
        }
        setButton(13, new Button(nameIcon, event -> {
            player.closeInventory();
            menuManager.getPromptManager().prompt(player, com.pumpkings.pkcrates.infrastructure.config.Messages.PROMPT_RENAME_KEY, input -> {
                ItemStack item = key.getBaseItem() != null ? key.getBaseItem().clone() : new ItemStack(Material.TRIPWIRE_HOOK);
                ItemMeta iMeta = item.getItemMeta();
                if (iMeta != null) {
                    iMeta.displayName(TextUtil.parse(input.replace("&", "§")));
                    item.setItemMeta(iMeta);
                }
                key.setBaseItem(item);
                keyRegistry.updateKey(key);
                player.sendRichMessage("<green>Key name updated.");
                org.bukkit.Bukkit.getScheduler().runTask(org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass()), () -> {
                    new KeyEditorMenu(menuManager, player, keyRegistry, key).open();
                });
            });
        }));

        // Virtual / Physical Type
        Material typeMat = config != null ? config.getItemMaterial("items.type", Material.ENDER_CHEST) : Material.ENDER_CHEST;
        ItemStack typeIcon = new ItemStack(typeMat);
        ItemMeta typeMeta = typeIcon.getItemMeta();
        if (typeMeta != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("<key_type>", key.isVirtual() ? "<green>Virtual" : "<yellow>Physical");
            if (config != null) {
                typeMeta.displayName(config.getItemName("items.type", placeholders));
                typeMeta.lore(config.getItemLore("items.type", placeholders));
            } else {
                typeMeta.displayName(TextUtil.parse("<aqua><bold>Key Type"));
                List<Component> lore = new ArrayList<>();
                lore.add(TextUtil.parse("<gray>Current: " + (key.isVirtual() ? "<green>Virtual" : "<yellow>Physical")));
                lore.add(Component.empty());
                lore.add(TextUtil.parse("<green>▶ Click to toggle."));
                typeMeta.lore(lore);
            }
            typeIcon.setItemMeta(typeMeta);
        }
        setButton(15, new Button(typeIcon, event -> {
            key.setVirtual(!key.isVirtual());
            keyRegistry.updateKey(key);
            new KeyEditorMenu(menuManager, player, keyRegistry, key).open();
        }));

        // Rarity Restriction Type
        ItemStack restrictionIcon = new ItemStack(Material.REPEATER);
        ItemMeta restMeta = restrictionIcon.getItemMeta();
        if (restMeta != null) {
            restMeta.displayName(TextUtil.parse("<light_purple><bold>Rarity Restriction Type"));
            List<Component> lore = new ArrayList<>();
            lore.add(TextUtil.parse("<gray>Current: <white>" + key.getRarityRestriction().name()));
            lore.add(Component.empty());
            lore.add(TextUtil.parse("<green>▶ Click to toggle."));
            restMeta.lore(lore);
            restrictionIcon.setItemMeta(restMeta);
        }
        setButton(19, new Button(restrictionIcon, event -> {
            IKey.RarityRestriction[] vals = IKey.RarityRestriction.values();
            int next = (key.getRarityRestriction().ordinal() + 1) % vals.length;
            key.setRarityRestriction(vals[next]);
            keyRegistry.updateKey(key);
            this.open();
        }));

        // Rarity Target
        ItemStack targetIcon = new ItemStack(Material.PAPER);
        ItemMeta targetMeta = targetIcon.getItemMeta();
        if (targetMeta != null) {
            String target = key.getRarityTarget() != null && !key.getRarityTarget().isEmpty() ? key.getRarityTarget() : "NONE";
            targetMeta.displayName(TextUtil.parse("<yellow><bold>Rarity Target"));
            List<Component> lore = new ArrayList<>();
            lore.add(TextUtil.parse("<gray>Target: <white>" + target));
            lore.add(Component.empty());
            lore.add(TextUtil.parse("<green>▶ Left-Click to edit"));
            lore.add(TextUtil.parse("<red>▶ Right-Click to clear"));
            targetMeta.lore(lore);
            targetIcon.setItemMeta(targetMeta);
        }
        setButton(20, new Button(targetIcon, event -> {
            if (event.isRightClick()) {
                key.setRarityTarget("");
                keyRegistry.updateKey(key);
                this.open();
            } else {
                player.closeInventory();
                menuManager.getPromptManager().prompt(player, "Enter the target (ID or list of IDs or Priority Number):", input -> {
                    if (input.equalsIgnoreCase("cancelar")) {
                        org.bukkit.Bukkit.getScheduler().runTask(org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass()), this::open);
                        return;
                    }
                    key.setRarityTarget(input);
                    keyRegistry.updateKey(key);
                    org.bukkit.Bukkit.getScheduler().runTask(org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass()), this::open);
                });
            }
        }));

        // Back
        Material backMat = config != null ? config.getItemMaterial("items.back", Material.ARROW) : Material.ARROW;
        ItemStack back = new ItemStack(backMat);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            if (config != null) {
                backMeta.displayName(config.getItemName("items.back", null));
                backMeta.lore(config.getItemLore("items.back", null));
            } else {
                backMeta.displayName(TextUtil.parse("<red>Return to List"));
            }
            back.setItemMeta(backMeta);
        }
        setButton(27, new Button(back, event -> {
            new KeyListMenu(menuManager, player, keyRegistry).open();
        }));
    }
}
