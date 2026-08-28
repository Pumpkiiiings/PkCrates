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
        com.pumpkings.pkcrates.infrastructure.config.MenuConfig config = menuManager.getMenuConfigManager().getMenu("rarity_editor");
        java.util.Map<String, String> placeholders = new java.util.HashMap<>();
        placeholders.put("<rarity_id>", rarity.getId());
        return config != null ? config.getTitle(placeholders) : TextUtil.parse("<dark_gray>Edit Rarity: <red>" + rarity.getId());
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
        setButton(10, createEditButton("display_name", Material.NAME_TAG, "Display Name", rarity.getDisplayName(), "<color:#00FF45>▶ ʟᴇꜰᴛ-ᴄʟɪᴄᴋ ᴛᴏ ᴇᴅɪᴛ", event -> {
            prompt("Enter new display name (MiniMessage format):", input -> {
                rarity.setDisplayName(input);
                saveAndRefresh();
            });
        }));

        // Description
        setButton(11, createEditButton("description", Material.WRITABLE_BOOK, "Description", rarity.getDescription(), "<color:#00FF45>▶ ʟᴇꜰᴛ-ᴄʟɪᴄᴋ ᴛᴏ ᴇᴅɪᴛ", event -> {
            prompt("Enter new description:", input -> {
                rarity.setDescription(input);
                saveAndRefresh();
            });
        }));

        // Priority
        setButton(12, createEditButton("priority", Material.COMPARATOR, "Priority", String.valueOf(rarity.getPriority()), "<color:#00FF45>▶ ʟᴇꜰᴛ-ᴄʟɪᴄᴋ (+1)\n<color:#FF2626>▶ ʀɪɢʜᴛ-ᴄʟɪᴄᴋ (-1)", event -> {
            if (event.isLeftClick()) rarity.setPriority(rarity.getPriority() + 1);
            else rarity.setPriority(Math.max(1, rarity.getPriority() - 1));
            saveAndRefresh();
        }));

        // Chance Mode
        setButton(13, createEditButton("chance_mode", Material.REPEATER, "Chance Mode", rarity.getChanceMode().name(), "<color:#00FF45>▶ ᴄʟɪᴄᴋ ᴛᴏ ᴛᴏɢɢʟᴇ", event -> {
            if (rarity.getChanceMode() == RarityChanceMode.INDEPENDENT) rarity.setChanceMode(RarityChanceMode.SYNCED);
            else rarity.setChanceMode(RarityChanceMode.INDEPENDENT);
            saveAndRefresh();
        }));

        // Weight
        setButton(14, createEditButton("weight", Material.GOLD_INGOT, "Weight", String.valueOf(rarity.getWeight()), "<color:#00FF45>▶ ʟᴇꜰᴛ-ᴄʟɪᴄᴋ ᴛᴏ ᴇᴅɪᴛ", event -> {
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
        setButton(19, createEditButton("color", Material.MAGMA_CREAM, "Color", rarity.getColor(), "<color:#00FF45>▶ ʟᴇꜰᴛ-ᴄʟɪᴄᴋ ᴛᴏ ᴇᴅɪᴛ", event -> {
            prompt("Enter new color:", input -> {
                rarity.setColor(input);
                saveAndRefresh();
            });
        }));

        // MiniMessage Format
        setButton(20, createEditButton("mm_format", Material.PAPER, "MM Format", rarity.getMiniMessageFormat(), "<color:#00FF45>▶ ʟᴇꜰᴛ-ᴄʟɪᴄᴋ ᴛᴏ ᴇᴅɪᴛ", event -> {
            prompt("Enter new format ({color}, {text}):", input -> {
                rarity.setMiniMessageFormat(input);
                saveAndRefresh();
            });
        }));

        // Icon
        setButton(21, createEditButton("icon", Material.ITEM_FRAME, "Icon", rarity.getIcon(), "<color:#00FF45>▶ ʟᴇꜰᴛ-ᴄʟɪᴄᴋ ᴛᴏ ᴇᴅɪᴛ", event -> {
            prompt("Enter new Material icon:", input -> {
                rarity.setIcon(input.toUpperCase());
                saveAndRefresh();
            });
        }));

        // Glow
        setButton(22, createEditButton("glow", Material.GLOWSTONE_DUST, "Glow", String.valueOf(rarity.isGlow()), "<color:#00FF45>▶ ᴄʟɪᴄᴋ ᴛᴏ ᴛᴏɢɢʟᴇ", event -> {
            rarity.setGlow(!rarity.isGlow());
            saveAndRefresh();
        }));

        // Broadcast Enabled
        setButton(28, createEditButton("broadcast", Material.OAK_SIGN, "Broadcast", String.valueOf(rarity.isBroadcastEnabled()), "<color:#00FF45>▶ ᴄʟɪᴄᴋ ᴛᴏ ᴛᴏɢɢʟᴇ", event -> {
            rarity.setBroadcastEnabled(!rarity.isBroadcastEnabled());
            saveAndRefresh();
        }));

        // Announcement Template
        setButton(29, createEditButton("announcement_template", Material.BELL, "Announcement Template", rarity.getAnnouncementTemplate(), "<color:#00FF45>▶ ʟᴇꜰᴛ-ᴄʟɪᴄᴋ ᴛᴏ ᴇᴅɪᴛ", event -> {
            prompt("Enter new template:", input -> {
                rarity.setAnnouncementTemplate(input);
                saveAndRefresh();
            });
        }));

        // Back
        com.pumpkings.pkcrates.infrastructure.config.MenuConfig config = menuManager.getMenuConfigManager().getMenu("rarity_editor");
        Material backMat = config != null ? config.getItemMaterial("items.back", Material.ARROW) : Material.ARROW;
        ItemStack back = new ItemStack(backMat);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            if (config != null) {
                backMeta.displayName(config.getItemName("items.back", null));
                backMeta.lore(config.getItemLore("items.back", null));
            } else {
                backMeta.displayName(TextUtil.parse("<color:#FF2626><bold>ʙᴀᴄᴋ ᴛᴏ ʀᴀʀɪᴛɪᴇꜱ"));
            }
            back.setItemMeta(backMeta);
        }
        setButton(36, new Button(back, event -> {
            new RaritiesDashboardMenu(plugin, menuManager, player, rarityService).open();
        }));
    }

    private Button createEditButton(String configKey, Material fallbackMaterial, String defaultName, String value, String instruction, java.util.function.Consumer<org.bukkit.event.inventory.InventoryClickEvent> action) {
        com.pumpkings.pkcrates.infrastructure.config.MenuConfig config = menuManager.getMenuConfigManager().getMenu("rarity_editor");
        Material mat = config != null ? config.getItemMaterial("items." + configKey, fallbackMaterial) : fallbackMaterial;
        ItemStack item = new ItemStack(mat);
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta != null) {
            java.util.Map<String, String> placeholders = new java.util.HashMap<>();
            placeholders.put("<value>", value != null ? value : "none");
            
            if (config != null) {
                itemMeta.displayName(config.getItemName("items." + configKey, placeholders));
                itemMeta.lore(config.getItemLore("items." + configKey, placeholders));
            } else {
                itemMeta.displayName(TextUtil.parse("<color:#FCFF00><bold>" + defaultName.toUpperCase()));
                List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                lore.add(TextUtil.parse("<gray>ᴄᴜʀʀᴇɴᴛ: <white>" + value));
                lore.add(net.kyori.adventure.text.Component.empty());
                String[] split = instruction.split("\n");
                for (String s : split) {
                    lore.add(TextUtil.parse(s));
                }
                itemMeta.lore(lore);
            }
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
