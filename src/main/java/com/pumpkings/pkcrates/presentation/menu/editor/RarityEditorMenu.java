package com.pumpkings.pkcrates.presentation.menu.editor;

import com.pumpkings.pkcrates.core.model.rarity.Rarity;
import com.pumpkings.pkcrates.core.model.rarity.RarityChanceMode;
import com.pumpkings.pkcrates.api.rarity.RarityService;
import com.pumpkings.pkcrates.presentation.menu.Button;
import com.pumpkings.pkcrates.presentation.menu.MenuManager;
import com.pumpkings.pkcrates.presentation.menu.PkMenu;
import com.pumpkings.pkcrates.presentation.utils.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class RarityEditorMenu extends PkMenu {

    private final Plugin plugin;
    private final Rarity rarity;
    private final RarityService rarityService;

    public RarityEditorMenu(Plugin plugin, MenuManager menuManager, Player player, Rarity rarity, RarityService rarityService) {
        super(menuManager, player);
        this.plugin = plugin;
        this.rarity = rarity;
        this.rarityService = rarityService;
    }

    @Override
    public net.kyori.adventure.text.Component getTitle() {
        return TextUtil.parse("<dark_gray>Editing: <white>" + rarity.getId());
    }

    @Override
    public int getSize() {
        return 45;
    }

    @Override
    public void decorate() {
        // Background
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.empty());
            filler.setItemMeta(meta);
        }
        Button bg = Button.visual(filler);
        for (int i = 0; i < getSize(); i++) {
            setButton(i, bg);
        }

        // Display Name
        setButton(10, createEditButton(Material.NAME_TAG, "Display Name", rarity.getDisplayName(), "<gray>Left-Click to edit", event -> {
            prompt("Enter new display name (MiniMessage format):", input -> {
                rarity.setDisplayName(input);
                saveAndRefresh();
            });
        }));

        // Description
        setButton(11, createEditButton(Material.WRITABLE_BOOK, "Description", rarity.getDescription(), "<gray>Left-Click to edit", event -> {
            prompt("Enter new description:", input -> {
                rarity.setDescription(input);
                saveAndRefresh();
            });
        }));

        // Priority
        setButton(12, createEditButton(Material.COMPARATOR, "Priority", String.valueOf(rarity.getPriority()), "<gray>Left-Click to +1, Right-Click to -1", event -> {
            if (event.isLeftClick()) rarity.setPriority(rarity.getPriority() + 1);
            else rarity.setPriority(Math.max(1, rarity.getPriority() - 1));
            saveAndRefresh();
        }));

        // Chance Mode
        setButton(13, createEditButton(Material.REPEATER, "Chance Mode", rarity.getChanceMode().name(), "<gray>Click to toggle", event -> {
            if (rarity.getChanceMode() == RarityChanceMode.INDEPENDENT) rarity.setChanceMode(RarityChanceMode.SYNCED);
            else rarity.setChanceMode(RarityChanceMode.INDEPENDENT);
            saveAndRefresh();
        }));

        // Weight
        setButton(14, createEditButton(Material.GOLD_INGOT, "Weight", String.valueOf(rarity.getWeight()), "<gray>Left-Click to edit", event -> {
            prompt("Enter new weight:", input -> {
                try {
                    rarity.setWeight(Double.parseDouble(input));
                    saveAndRefresh();
                } catch (NumberFormatException ignored) {
                    player.sendRichMessage("<red>Invalid number!");
                }
            });
        }));

        // Color
        setButton(19, createEditButton(Material.MAGMA_CREAM, "Color", rarity.getColor(), "<gray>Left-Click to edit (MiniMessage)", event -> {
            prompt("Enter new color:", input -> {
                rarity.setColor(input);
                saveAndRefresh();
            });
        }));

        // MiniMessage Format
        setButton(20, createEditButton(Material.PAPER, "MM Format", rarity.getMiniMessageFormat(), "<gray>Left-Click to edit", event -> {
            prompt("Enter new format ({color}, {text}):", input -> {
                rarity.setMiniMessageFormat(input);
                saveAndRefresh();
            });
        }));

        // Icon
        setButton(21, createEditButton(Material.ITEM_FRAME, "Icon", rarity.getIcon(), "<gray>Left-Click to edit", event -> {
            prompt("Enter new Material icon:", input -> {
                rarity.setIcon(input.toUpperCase());
                saveAndRefresh();
            });
        }));

        // Glow
        setButton(22, createEditButton(Material.GLOWSTONE_DUST, "Glow", String.valueOf(rarity.isGlow()), "<gray>Click to toggle", event -> {
            rarity.setGlow(!rarity.isGlow());
            saveAndRefresh();
        }));

        // Broadcast Enabled
        setButton(28, createEditButton(Material.OAK_SIGN, "Broadcast", String.valueOf(rarity.isBroadcastEnabled()), "<gray>Click to toggle", event -> {
            rarity.setBroadcastEnabled(!rarity.isBroadcastEnabled());
            saveAndRefresh();
        }));

        // Announcement Template
        setButton(29, createEditButton(Material.BELL, "Announcement Template", rarity.getAnnouncementTemplate(), "<gray>Left-Click to edit", event -> {
            prompt("Enter new template:", input -> {
                rarity.setAnnouncementTemplate(input);
                saveAndRefresh();
            });
        }));

        // Back
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(TextUtil.parse("<red><bold>Back"));
            back.setItemMeta(backMeta);
        }
        setButton(36, new Button(back, event -> {
            new RaritiesDashboardMenu(plugin, menuManager, player, rarityService).open();
        }));
    }

    private Button createEditButton(Material material, String name, String value, String instruction, java.util.function.Consumer<org.bukkit.event.inventory.InventoryClickEvent> action) {
        ItemStack item = new ItemStack(material);
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta != null) {
            itemMeta.displayName(TextUtil.parse("<yellow><bold>" + name));
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            lore.add(TextUtil.parse("<gray>Current: <white>" + value));
            lore.add(net.kyori.adventure.text.Component.empty());
            lore.add(TextUtil.parse(instruction));
            itemMeta.lore(lore);
            item.setItemMeta(itemMeta);
        }
        return new Button(item, action);
    }

    private void prompt(String text, java.util.function.Consumer<String> action) {
        player.closeInventory();
        menuManager.getPromptManager().prompt(player, text, input -> {
            if (input.equalsIgnoreCase("cancelar")) {
                org.bukkit.Bukkit.getScheduler().runTask(plugin, this::open);
                return;
            }
            action.accept(input);
        });
    }

    private void saveAndRefresh() {
        rarityService.update(rarity);
        decorate();
    }
}
