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
        return TextUtil.parse("<dark_gray>Admin <gray>| <white>Rarities");
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

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.parse(rarity.getDisplayName()));
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            lore.add(TextUtil.parse("<gray>ID: <white>" + rarity.getId()));
            lore.add(TextUtil.parse("<gray>Weight: <yellow>" + rarity.getWeight()));
            lore.add(TextUtil.parse("<gray>Chance Mode: <yellow>" + rarity.getChanceMode().name()));
            lore.add(TextUtil.parse("<gray>Priority: <yellow>" + rarity.getPriority()));
            lore.add(net.kyori.adventure.text.Component.empty());
            lore.add(TextUtil.parse("<green>▶ Left-Click to edit"));
            lore.add(TextUtil.parse("<red>▶ Shift-Right-Click to remove"));
            meta.lore(lore);
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

        // Create Rarity
        ItemStack create = new ItemStack(Material.EMERALD);
        ItemMeta createMeta = create.getItemMeta();
        if (createMeta != null) {
            createMeta.displayName(TextUtil.parse("<green><bold>+ Create New Rarity"));
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            lore.add(TextUtil.parse("<gray>Click to create a new rarity."));
            createMeta.lore(lore);
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
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(TextUtil.parse("<red><bold>Back to Crates"));
            back.setItemMeta(backMeta);
        }
        setButton(45, new Button(back, event -> {
            // Need to pass other services, I will pass null for now if not easily accessible, or better, we can open CratesDashboardMenu
            player.performCommand("crates");
        }));
    }
}
