package com.pumpkings.pkcrates.presentation.menu.editor;

import com.pumpkings.pkcrates.core.model.rarity.Rarity;
import com.pumpkings.pkcrates.api.rarity.RarityService;
import com.pumpkings.pkcrates.presentation.menu.Button;
import com.pumpkings.pkcrates.presentation.menu.MenuManager;
import com.pumpkings.pkcrates.presentation.menu.PaginatedPkMenu;
import com.pumpkings.pkcrates.presentation.utils.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import java.util.ArrayList;
import java.util.List;

public class RaritiesDashboardMenu extends PaginatedPkMenu<Rarity> {

    private final Plugin plugin;
    private final RarityService rarityService;

    private final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    public RaritiesDashboardMenu(Plugin plugin, MenuManager menuManager, Player player, RarityService rarityService) {
        super(menuManager, player);
        this.plugin = plugin;
        this.rarityService = rarityService;
    }

    @Override
    public net.kyori.adventure.text.Component getTitle() {
        com.pumpkings.pkcrates.infrastructure.config.MenuConfig config = menuManager.getMenuConfigManager().getMenu("rarities_dashboard");
        return config != null ? config.getTitle(null) : TextUtil.parse("<dark_gray>Admin <gray>| <red>Rarities");
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public List<Rarity> getItems() {
        return new ArrayList<>(rarityService.getAll());
    }

    @Override
    public int[] getItemSlots() {
        return SLOTS;
    }

    @Override
    public Button createItemButton(Rarity rarity) {
        Material mat = Material.matchMaterial(rarity.getIcon() != null ? rarity.getIcon() : "NETHER_STAR");
        if (mat == null) mat = Material.NETHER_STAR;

        com.pumpkings.pkcrates.infrastructure.config.MenuConfig config = menuManager.getMenuConfigManager().getMenu("rarities_dashboard");
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.parse(rarity.getDisplayName())); // Keep custom display name
            java.util.Map<String, String> placeholders = new java.util.HashMap<>();
            placeholders.put("<rarity_id>", rarity.getId());
            placeholders.put("<weight>", String.valueOf(rarity.getWeight()));
            placeholders.put("<chance_mode>", rarity.getChanceMode().name());
            placeholders.put("<priority>", String.valueOf(rarity.getPriority()));
            
            if (config != null) {
                meta.lore(config.getItemLore("items.rarity_format", placeholders));
            }
            item.setItemMeta(meta);
        }

        return new Button(item, event -> {
            if (event.getClick() == ClickType.LEFT) {
                new RarityEditorMenu(plugin, menuManager, player, rarity, rarityService).open();
            } else if (event.isRightClick() && event.isShiftClick()) {
                rarityService.delete(rarity.getId());
                player.sendRichMessage("<red>Rarity removed.");
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

        for (int i = 0; i < 9; i++) setButton(i, bg);
        for (int i = 45; i < 54; i++) {
            if (i != 49) setButton(i, bg);
        }

        com.pumpkings.pkcrates.infrastructure.config.MenuConfig config = menuManager.getMenuConfigManager().getMenu("rarities_dashboard");

        // Create Rarity
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
            menuManager.getPromptManager().prompt(player, com.pumpkings.pkcrates.infrastructure.config.Messages.PROMPT_NEW_CRATE, input -> { // Using crate prompt message for now, can be updated later
                if (input.equalsIgnoreCase("cancelar")) {
                    return;
                }
                if (rarityService.exists(input)) {
                    player.sendRichMessage("<red>A rarity with that ID already exists.");
                    return;
                }
                rarityService.create(input);
                player.sendRichMessage("<green>Rarity '" + input + "' created successfully.");
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    new RaritiesDashboardMenu(plugin, menuManager, player, rarityService).open();
                });
            });
        }));

        // Back Button
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
            player.performCommand("crate editor");
        }));
    }
}
