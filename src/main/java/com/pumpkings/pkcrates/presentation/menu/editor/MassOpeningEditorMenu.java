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
        return TextUtil.parse("<dark_gray>Edit > Mass Opening > " + crate.getId());
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

        // Toggle Enabled/Disabled Button (Slot 11)
        ItemStack statusItem = new ItemStack(enabled ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK);
        ItemMeta statusMeta = statusItem.getItemMeta();
        if (statusMeta != null) {
            statusMeta.displayName(TextUtil.parse(enabled ? "<green><bold>Mass Opening: ENABLED" : "<red><bold>Mass Opening: DISABLED"));
            List<Component> lore = new ArrayList<>();
            lore.add(TextUtil.parse("<gray>Status: " + (enabled ? "<green>Active" : "<red>Inactive")));
            lore.add(Component.empty());
            lore.add(TextUtil.parse("<yellow>▶ Click to toggle status"));
            statusMeta.lore(lore);
            statusItem.setItemMeta(statusMeta);
        }

        final MassOpeningConfig finalConfig = config;
        setButton(11, new Button(statusItem, event -> {
            finalConfig.setEnabled(!enabled);
            crateRegistry.saveCrate(crate);
            open();
        }));

        // Add Option Button (Slot 15)
        ItemStack addItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta addMeta = addItem.getItemMeta();
        if (addMeta != null) {
            addMeta.displayName(TextUtil.parse("<gold><bold>Add Opening Amount"));
            List<Component> lore = new ArrayList<>();
            lore.add(TextUtil.parse("<gray>Add a new numeric option"));
            lore.add(TextUtil.parse("<gray>(e.g. 10, 25, 50, or ALL)"));
            lore.add(Component.empty());
            lore.add(TextUtil.parse("<yellow>▶ Click to add"));
            addMeta.lore(lore);
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
                meta.displayName(TextUtil.parse(opt.isAll() ? "<yellow><bold>Option: ALL" : "<yellow><bold>Option: x" + opt.getAmount()));
                List<Component> lore = new ArrayList<>();
                lore.add(TextUtil.parse("<gray>Allows opening " + (opt.isAll() ? "all keys" : "up to " + opt.getAmount() + " keys")));
                lore.add(Component.empty());
                lore.add(TextUtil.parse("<red>▶ Click to remove this option"));
                meta.lore(lore);
                item.setItemMeta(meta);
            }

            setButton(slot, new Button(item, event -> {
                finalConfig.getOptions().remove(opt);
                crateRegistry.saveCrate(crate);
                open();
            }));
        }

        // Back button (Slot 36)
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(TextUtil.parse("<red>Return to Crate Editor"));
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
