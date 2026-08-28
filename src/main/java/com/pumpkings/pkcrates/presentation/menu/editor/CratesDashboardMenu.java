package com.pumpkings.pkcrates.presentation.menu.editor;

import com.pumpkings.pkcrates.core.model.Crate;
import com.pumpkings.pkcrates.infrastructure.config.CrateRegistry;
import com.pumpkings.pkcrates.infrastructure.key.KeyRegistry;
import com.pumpkings.pkcrates.infrastructure.location.CrateLocationManager;
import com.pumpkings.pkcrates.presentation.menu.Button;
import com.pumpkings.pkcrates.presentation.menu.MenuManager;
import com.pumpkings.pkcrates.presentation.menu.PaginatedPkMenu;
import com.pumpkings.pkcrates.presentation.utils.TextUtil;
import net.kyori.adventure.text.Component;
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

public class CratesDashboardMenu extends PaginatedPkMenu<Crate> {

    private final Plugin plugin;
    private final CrateRegistry crateRegistry;
    private final KeyRegistry keyRegistry;
    private final CrateLocationManager locationMgr;
    private final com.pumpkings.pkcrates.infrastructure.display.HologramManager hologramManager;

    private final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    public CratesDashboardMenu(Plugin plugin, MenuManager menuManager, Player player, CrateRegistry crateRegistry, KeyRegistry keyRegistry, CrateLocationManager locationMgr, com.pumpkings.pkcrates.infrastructure.display.HologramManager hologramManager) {
        super(menuManager, player);
        this.plugin = plugin;
        this.crateRegistry = crateRegistry;
        this.keyRegistry = keyRegistry;
        this.locationMgr = locationMgr;
        this.hologramManager = hologramManager;
    }

    @Override
    public net.kyori.adventure.text.Component getTitle() {
        MenuConfig config = menuManager.getMenuConfigManager().getMenu("crates_dashboard");
        return config != null ? config.getTitle(null) : TextUtil.parse("<dark_gray>Admin <gray>| <white>Crates");
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public List<Crate> getItems() {
        return new ArrayList<>(crateRegistry.getAllCrates());
    }

    @Override
    public int[] getItemSlots() {
        return SLOTS;
    }

    @Override
    public Button createItemButton(Crate crate) {
        MenuConfig config = menuManager.getMenuConfigManager().getMenu("crates_dashboard");
        Material mat = config != null ? config.getItemMaterial("items.crate_format", Material.ENDER_CHEST) : Material.ENDER_CHEST;
        
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("<crate_name>", crate.getName());
            placeholders.put("<crate_id>", crate.getId());
            placeholders.put("<rewards_count>", String.valueOf(crate.getRewards().size()));
            placeholders.put("<keys_count>", String.valueOf(crate.getAcceptedKeys().size()));
            
            if (config != null) {
                meta.displayName(config.getItemName("items.crate_format", placeholders));
                meta.lore(config.getItemLore("items.crate_format", placeholders));
            }
            item.setItemMeta(meta);
        }

        return new Button(item, event -> {
            if (event.getClick() == ClickType.LEFT) {
                new CrateEditorMenu(plugin, menuManager, player, crate, crateRegistry, keyRegistry, locationMgr, hologramManager).open();
            } else if (event.isRightClick() && event.isShiftClick()) {
                // Delete confirmation logic (Phase 4, for now just delete)
                crateRegistry.deleteCrate(crate.getId());
                player.sendRichMessage("<red>Crate removed.");
                decorate();
            }
        });
    }

    @Override
    protected void addDecorations() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.empty());
            filler.setItemMeta(meta);
        }
        Button bg = Button.visual(filler);

        // Fill borders
        for (int i = 0; i < 9; i++) setButton(i, bg);
        for (int i = 45; i < 54; i++) {
            if (i != 49) setButton(i, bg); // 49 is reserved for Close button by PaginatedPkMenu
        }

        MenuConfig config = menuManager.getMenuConfigManager().getMenu("crates_dashboard");
        
        // Custom buttons
        // Create Crate
        Material createMat = config != null ? config.getItemMaterial("items.create", Material.EMERALD) : Material.EMERALD;
        ItemStack create = new ItemStack(createMat);
        ItemMeta createMeta = create.getItemMeta();
        if (createMeta != null) {
            if (config != null) {
                createMeta.displayName(config.getItemName("items.create", null));
                createMeta.lore(config.getItemLore("items.create", null));
            }
            create.setItemMeta(createMeta);
        }
        setButton(53, new Button(create, event -> {
            player.closeInventory();
            menuManager.getPromptManager().prompt(player, com.pumpkings.pkcrates.infrastructure.config.Messages.PROMPT_NEW_CRATE, input -> {
                if (input.equalsIgnoreCase("cancelar")) {
                    return;
                }
                if (crateRegistry.getCrate(input) != null) {
                    player.sendRichMessage("<red>A crate with that ID already exists.");
                    return;
                }
                crateRegistry.createCrate(input);
                player.sendRichMessage("<green>Crate '" + input + "' created successfully.");
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    new CratesDashboardMenu(plugin, menuManager, player, crateRegistry, keyRegistry, locationMgr, hologramManager).open();
                });
            });
        }));

        // Keys Manager
        Material keysMat = config != null ? config.getItemMaterial("items.keys", Material.TRIPWIRE_HOOK) : Material.TRIPWIRE_HOOK;
        ItemStack keys = new ItemStack(keysMat);
        ItemMeta keysMeta = keys.getItemMeta();
        if (keysMeta != null) {
            if (config != null) {
                keysMeta.displayName(config.getItemName("items.keys", null));
                keysMeta.lore(config.getItemLore("items.keys", null));
            }
            keys.setItemMeta(keysMeta);
        }
        setButton(45, new Button(keys, event -> {
            new KeyListMenu(menuManager, player, keyRegistry).open();
        }));

        // Rarities Manager
        Material raritiesMat = config != null ? config.getItemMaterial("items.rarities", Material.NETHER_STAR) : Material.NETHER_STAR;
        ItemStack rarities = new ItemStack(raritiesMat);
        ItemMeta raritiesMeta = rarities.getItemMeta();
        if (raritiesMeta != null) {
            if (config != null) {
                raritiesMeta.displayName(config.getItemName("items.rarities", null));
                raritiesMeta.lore(config.getItemLore("items.rarities", null));
            }
            rarities.setItemMeta(raritiesMeta);
        }
        setButton(52, new Button(rarities, event -> {
            com.pumpkings.pkcrates.api.rarity.RarityService rarityService = ((com.pumpkings.pkcrates.PkCratesPlugin) plugin).getRarityService();
            if (rarityService != null) {
                new RaritiesDashboardMenu(plugin, menuManager, player, rarityService).open();
            } else {
                player.sendRichMessage("<red>Rarity service is not available.");
            }
        }));
    }
}
