package com.pumpkings.pkcrates.presentation.menu.editor;

import com.pumpkings.pkcrates.core.model.key.IKey;
import com.pumpkings.pkcrates.infrastructure.key.KeyRegistry;
import com.pumpkings.pkcrates.presentation.menu.Button;
import com.pumpkings.pkcrates.presentation.menu.MenuManager;
import com.pumpkings.pkcrates.presentation.menu.PaginatedPkMenu;
import com.pumpkings.pkcrates.presentation.utils.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.pumpkings.pkcrates.infrastructure.config.MenuConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class KeyListMenu extends PaginatedPkMenu<IKey> {

    private final KeyRegistry keyRegistry;

    private final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    public KeyListMenu(MenuManager menuManager, Player player, KeyRegistry keyRegistry) {
        super(menuManager, player);
        this.keyRegistry = keyRegistry;
    }

    @Override
    public Component getTitle() {
        MenuConfig config = menuManager.getMenuConfigManager().getMenu("key_list");
        return config != null ? config.getTitle(null) : TextUtil.parse("<dark_green>Registered Keys");
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public List<IKey> getItems() {
        return keyRegistry.getAllKeys();
    }

    @Override
    public int[] getItemSlots() {
        return SLOTS;
    }

    @Override
    public Button createItemButton(IKey key) {
        MenuConfig config = menuManager.getMenuConfigManager().getMenu("key_list");
        ItemStack display = key.getBaseItem();
        if (display == null) display = new ItemStack(Material.TRIPWIRE_HOOK);
        else display = display.clone();

        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("<key_id>", key.getId());
            placeholders.put("<key_type>", key.isVirtual() ? "<aqua>Virtual" : "<yellow>Physical");
            
            if (!meta.hasDisplayName()) {
                if (config != null) {
                    meta.displayName(config.getItemName("items.key_format", placeholders));
                } else {
                    meta.displayName(TextUtil.parse("<yellow><bold>Key: <white>" + key.getId()));
                }
            }
            List<Component> lore = meta.lore();
            if (lore == null) lore = new ArrayList<>();
            
            if (config != null) {
                lore.addAll(config.getItemLore("items.key_format", placeholders));
            } else {
                lore.add(Component.empty());
                lore.add(TextUtil.parse("<gray>ID: <white>" + key.getId()));
                lore.add(TextUtil.parse("<gray>Type: " + (key.isVirtual() ? "<aqua>Virtual" : "<yellow>Physical")));
                lore.add(Component.empty());
                lore.add(TextUtil.parse("<green>▶ Left-Click to edit"));
                lore.add(TextUtil.parse("<red>▶ Shift-Right-Click to remove"));
            }
            meta.lore(lore);
            display.setItemMeta(meta);
        }

        return new Button(display, event -> {
            if (event.getClick() == ClickType.SHIFT_RIGHT) {
                keyRegistry.deleteKey(key.getId());
                player.sendRichMessage("<red>Key removed.");
                decorate();
            } else if (event.getClick() == ClickType.LEFT) {
                new KeyEditorMenu(menuManager, player, keyRegistry, key).open();
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

        MenuConfig config = menuManager.getMenuConfigManager().getMenu("key_list");

        // Create Key Button
        Material createMat = config != null ? config.getItemMaterial("items.create", Material.EMERALD_BLOCK) : Material.EMERALD_BLOCK;
        ItemStack create = new ItemStack(createMat);
        ItemMeta createMeta = create.getItemMeta();
        if (createMeta != null) {
            if (config != null) {
                createMeta.displayName(config.getItemName("items.create", null));
                createMeta.lore(config.getItemLore("items.create", null));
            } else {
                createMeta.displayName(TextUtil.parse("<green><bold>+ Create New Key"));
                List<Component> lore = new ArrayList<>();
                lore.add(TextUtil.parse("<gray>Click to create a new key."));
                createMeta.lore(lore);
            }
            create.setItemMeta(createMeta);
        }
        setButton(53, new Button(create, event -> {
            player.closeInventory();
            menuManager.getPromptManager().prompt(player, com.pumpkings.pkcrates.infrastructure.config.Messages.PROMPT_NEW_KEY, input -> {
                if (input.equalsIgnoreCase("cancelar")) {
                    org.bukkit.Bukkit.getScheduler().runTask(org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass()), () -> this.open());
                    return;
                }
                if (keyRegistry.getKey(input) != null) {
                    player.sendRichMessage("<red>A key with that ID already exists.");
                    return;
                }
                keyRegistry.createKey(input, false);
                player.sendRichMessage("<green>Key '" + input + "' created successfully.");
                org.bukkit.Bukkit.getScheduler().runTask(org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass()), () -> {
                    new KeyListMenu(menuManager, player, keyRegistry).open();
                });
            });
        }));
    }
}
