package com.pumpkings.pkcrates.presentation.menu.editor;

import com.pumpkings.pkcrates.core.model.Crate;
import com.pumpkings.pkcrates.core.model.massopening.MassOpeningOption;
import com.pumpkings.pkcrates.infrastructure.config.CrateRegistry;
import com.pumpkings.pkcrates.infrastructure.config.MassOpeningConfig;
import com.pumpkings.pkcrates.presentation.menu.Button;
import com.pumpkings.pkcrates.presentation.menu.MenuManager;
import com.pumpkings.pkcrates.presentation.menu.PkMenu;
import com.pumpkings.pkcrates.presentation.utils.TextUtil;
import net.kyori.adventure.text.Component;
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

public class MassOpeningEditorMenu extends PkMenu {

    private final Plugin plugin;
    private final Crate crate;
    private final CrateRegistry crateRegistry;
    private final PkMenu parentMenu;

    private static final int[] OPTION_SLOTS = { 19, 20, 21, 22, 23, 24, 25 };

    public MassOpeningEditorMenu(Plugin plugin, MenuManager menuManager, Player player,
                                 Crate crate, CrateRegistry crateRegistry, PkMenu parentMenu) {
        super(menuManager, player);
        this.plugin = plugin;
        this.crate = crate;
        this.crateRegistry = crateRegistry;
        this.parentMenu = parentMenu;
    }

    @Override
    public Component getTitle() {
        MenuConfig config = menuManager.getMenuConfigManager().getMenu("mass_opening_editor");
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("<crate_id>", crate.getId());
        return config != null ? config.getTitle(placeholders) : TextUtil.parse("<dark_gray>Edit Mass Opening <gray>| <white>" + crate.getId());
    }

    @Override
    public int getSize() {
        return 45;
    }

    @Override
    public void decorate() {
        // Filler background
        ItemStack bgItem = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bgMeta = bgItem.getItemMeta();
        if (bgMeta != null) {
            bgMeta.displayName(Component.empty());
            bgItem.setItemMeta(bgMeta);
        }
        Button bg = Button.visual(bgItem);
        for (int i = 0; i < getSize(); i++) {
            setButton(i, bg);
        }

        MassOpeningConfig config = crate.getMassOpeningConfig();
        if (config == null) {
            config = MassOpeningConfig.createDefault(false);
            crate.setMassOpeningConfig(config);
        }

        boolean enabled = config.isEnabled();

        MenuConfig menuConfig = menuManager.getMenuConfigManager().getMenu("mass_opening_editor");

        // Toggle Enabled/Disabled Button (Slot 11)
        ItemStack statusItem = new ItemStack(enabled ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK);
        ItemMeta statusMeta = statusItem.getItemMeta();
        if (statusMeta != null) {
            if (menuConfig != null) {
                String namePath = enabled ? "items.status.enabled_name" : "items.status.disabled_name";
                String lorePath = enabled ? "items.status.enabled_lore" : "items.status.disabled_lore";
                statusMeta.displayName(menuConfig.getItemName(namePath.replace(".name", ""), null));
                statusMeta.lore(menuConfig.getExactLore(lorePath, null));
            }
            statusItem.setItemMeta(statusMeta);
        }

        final MassOpeningConfig finalConfig = config;
        setButton(11, new Button(statusItem, event -> {
            finalConfig.setEnabled(!enabled);
            crateRegistry.saveCrate(crate);
            open();
        }));

        // Add Option Button (Slot 15)
        Material addMat = menuConfig != null ? menuConfig.getItemMaterial("items.add_option", Material.NETHER_STAR) : Material.NETHER_STAR;
        ItemStack addItem = new ItemStack(addMat);
        ItemMeta addMeta = addItem.getItemMeta();
        if (addMeta != null) {
            if (menuConfig != null) {
                addMeta.displayName(menuConfig.getItemName("items.add_option", null));
                addMeta.lore(menuConfig.getItemLore("items.add_option", null));
            }
            addItem.setItemMeta(addMeta);
        }
        setButton(15, new Button(addItem, event -> {
            player.closeInventory();
            menuManager.getPromptManager().prompt(player, "<green>Type the amount to add (number or 'all'):", input -> {
                if (input.equalsIgnoreCase("cancelar") || input.equalsIgnoreCase("cancel")) return;
                MassOpeningOption newOption;
                if (input.equalsIgnoreCase("all") || input.equalsIgnoreCase("todo")) {
                    newOption = MassOpeningOption.all();
                } else {
                    try {
                        int amount = Integer.parseInt(input);
                        if (amount <= 0) {
                            player.sendRichMessage("<red>Amount must be positive!");
                            open();
                            return;
                        }
                        newOption = MassOpeningOption.of(amount);
                    } catch (NumberFormatException e) {
                        player.sendRichMessage("<red>Invalid number!");
                        open();
                        return;
                    }
                }
                finalConfig.getOptions().add(newOption);
                crateRegistry.saveCrate(crate);
                player.sendRichMessage("<green>Option added!");
                open();
            });
        }));

        // Render current options
        List<MassOpeningOption> options = config.getOptions();
        for (int i = 0; i < options.size() && i < OPTION_SLOTS.length; i++) {
            MassOpeningOption opt = options.get(i);
            int slot = OPTION_SLOTS[i];

            ItemStack item = new ItemStack(opt.isAll() ? Material.NETHER_STAR : Material.GOLD_NUGGET);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (menuConfig != null) {
                    Map<String, String> phs = new HashMap<>();
                    phs.put("<amount>", String.valueOf(opt.getAmount()));
                    phs.put("<amount_text>", opt.isAll() ? "ᴀʟʟ ᴋᴇʏꜱ" : "ᴜᴘ ᴛᴏ " + opt.getAmount() + " ᴋᴇʏꜱ");
                    
                    String namePath = opt.isAll() ? "items.option_format.all_name" : "items.option_format.amount_name";
                    meta.displayName(menuConfig.getItemName(namePath.replace(".name", ""), phs));
                    meta.lore(menuConfig.getItemLore("items.option_format", phs));
                }
                item.setItemMeta(meta);
            }

            setButton(slot, new Button(item, event -> {
                finalConfig.getOptions().remove(opt);
                crateRegistry.saveCrate(crate);
                open();
            }));
        }

        // Back button (Slot 36)
        Material backMat = menuConfig != null ? menuConfig.getItemMaterial("items.back", Material.ARROW) : Material.ARROW;
        ItemStack back = new ItemStack(backMat);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            if (menuConfig != null) {
                backMeta.displayName(menuConfig.getItemName("items.back", null));
                backMeta.lore(menuConfig.getItemLore("items.back", null));
            }
            back.setItemMeta(backMeta);
        }
        setButton(36, new Button(back, event -> {
            if (parentMenu != null) {
                parentMenu.open();
            } else {
                player.closeInventory();
            }
        }));
    }
}
