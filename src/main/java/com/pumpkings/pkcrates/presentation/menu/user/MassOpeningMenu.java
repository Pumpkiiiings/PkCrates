package com.pumpkings.pkcrates.presentation.menu.user;

import com.pumpkings.pkcrates.core.model.Crate;
import com.pumpkings.pkcrates.core.model.key.IKey;
import com.pumpkings.pkcrates.core.model.massopening.MassOpeningOption;
import com.pumpkings.pkcrates.core.service.KeyService;
import com.pumpkings.pkcrates.core.service.MassOpeningService;
import com.pumpkings.pkcrates.infrastructure.config.MenuConfig;
import com.pumpkings.pkcrates.infrastructure.key.KeyRegistry;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MassOpeningMenu extends PkMenu {

    private final Plugin plugin;
    private final Crate crate;
    private final MassOpeningService massOpeningService;
    private final KeyService keyService;
    private final KeyRegistry keyRegistry;
    private int userKeys = 0;
    private int maxAllowed = 0;

    private static final int[] OPTION_SLOTS = { 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25 };

    public MassOpeningMenu(Plugin plugin, MenuManager menuManager, Player player, Crate crate,
                           MassOpeningService massOpeningService, KeyService keyService, KeyRegistry keyRegistry) {
        super(menuManager, player);
        this.plugin = plugin;
        this.crate = crate;
        this.massOpeningService = massOpeningService;
        this.keyService = keyService;
        this.keyRegistry = keyRegistry;
        this.maxAllowed = massOpeningService.resolveMaxAllowed(player, crate);
    }

    @Override
    public Component getTitle() {
        MenuConfig config = menuManager.getMenuConfigManager().getMenu("mass_opening_menu");
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("<crate_name>", crate.getName());
        return config != null ? config.getTitle(placeholders)
                : TextUtil.parse("<gradient:#4287f5:#42d4f5><bold>Mass Opening: </bold></gradient>" + crate.getName());
    }

    @Override
    public int getSize() {
        return 36;
    }

    @Override
    public void decorate() {
        MenuConfig config = menuManager.getMenuConfigManager().getMenu("mass_opening_menu");

        // Build filler background
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillMeta = filler.getItemMeta();
        if (fillMeta != null) {
            fillMeta.displayName(Component.empty());
            filler.setItemMeta(fillMeta);
        }
        for (int i = 0; i < getSize(); i++) {
            setButton(i, Button.visual(filler));
        }

        // Add info icon
        Material infoMat = config != null ? config.getItemMaterial("items.info", Material.CHEST) : Material.CHEST;
        ItemStack infoItem = new ItemStack(infoMat);
        ItemMeta infoMeta = infoItem.getItemMeta();
        if (infoMeta != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("<crate_name>", crate.getName());
            placeholders.put("<user_keys>", String.valueOf(userKeys));
            placeholders.put("<max_allowed>", maxAllowed == Integer.MAX_VALUE ? "Unlimited" : String.valueOf(maxAllowed));
            if (config != null && config.getItemName("items.info", placeholders) != null) {
                infoMeta.displayName(config.getItemName("items.info", placeholders));
                infoMeta.lore(config.getItemLore("items.info", placeholders));
            } else {
                infoMeta.displayName(TextUtil.parse("<aqua><bold>" + crate.getName()));
                List<Component> lore = new ArrayList<>();
                lore.add(TextUtil.parse("<gray>Available Keys: <white>" + userKeys));
                lore.add(TextUtil.parse("<gray>Max Permitted limit: <yellow>" + (maxAllowed == Integer.MAX_VALUE ? "Unlimited" : maxAllowed)));
                lore.add(TextUtil.parse(""));
                lore.add(TextUtil.parse("<dark_gray>Select an amount below to open!"));
                infoMeta.lore(lore);
            }
            infoItem.setItemMeta(infoMeta);
        }
        setButton(4, Button.visual(infoItem));

        // Add close button
        Material closeMat = config != null ? config.getItemMaterial("items.close", Material.BARRIER) : Material.BARRIER;
        ItemStack closeItem = new ItemStack(closeMat);
        ItemMeta closeMeta = closeItem.getItemMeta();
        if (closeMeta != null) {
            if (config != null && config.getItemName("items.close", null) != null) {
                closeMeta.displayName(config.getItemName("items.close", null));
                closeMeta.lore(config.getItemLore("items.close", null));
            } else {
                closeMeta.displayName(TextUtil.parse("<red><bold>Close Menu"));
            }
            closeItem.setItemMeta(closeMeta);
        }
        setButton(31, new Button(closeItem, event -> {
            player.closeInventory();
        }));

        // Populate options asynchronously or using current keys
        if (!crate.getAcceptedKeys().isEmpty()) {
            String keyId = crate.getAcceptedKeys().get(0);
            IKey key = keyRegistry.getKey(keyId);
            if (key != null) {
                keyService.countKeys(player, key).thenAccept(count -> {
                    this.userKeys = count;
                    plugin.getServer().getScheduler().runTask(plugin, this::populateOptionButtons);
                });
                return;
            }
        }
        populateOptionButtons();
    }

    private void populateOptionButtons() {
        MenuConfig config = menuManager.getMenuConfigManager().getMenu("mass_opening_menu");
        List<MassOpeningOption> options = massOpeningService.getAvailableOptions(player, crate, userKeys);
        for (int i = 0; i < options.size() && i < OPTION_SLOTS.length; i++) {
            MassOpeningOption opt = options.get(i);
            int slot = OPTION_SLOTS[i];

            Material defaultMat = opt.isAll() ? Material.NETHER_STAR : Material.TRIPWIRE_HOOK;
            String configPath = opt.isAll() ? "items.option_all" : "items.option_amount";
            Material mat = config != null ? config.getItemMaterial(configPath, defaultMat) : defaultMat;
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("<amount>", String.valueOf(opt.getAmount()));
                placeholders.put("<user_keys>", String.valueOf(userKeys));
                int effective = Math.min(userKeys, maxAllowed);
                placeholders.put("<effective>", String.valueOf(effective));

                if (config != null && config.getItemName(configPath, placeholders) != null) {
                    meta.displayName(config.getItemName(configPath, placeholders));
                    meta.lore(config.getItemLore(configPath, placeholders));
                } else {
                    if (opt.isAll()) {
                        meta.displayName(TextUtil.parse("<gradient:#f5af19:#f12711><bold>OPEN ALL</bold></gradient>"));
                    } else {
                        meta.displayName(TextUtil.parse("<green><bold>Open x" + opt.getAmount()));
                    }

                    List<Component> lore = new ArrayList<>();
                    lore.add(TextUtil.parse("<gray>Your Keys: <white>" + userKeys));
                    if (opt.isAll()) {
                        lore.add(TextUtil.parse("<gray>Will open: <yellow>" + effective + "x"));
                    } else {
                        lore.add(TextUtil.parse("<gray>Cost: <white>" + opt.getAmount() + " keys"));
                    }
                    lore.add(TextUtil.parse(""));
                    if (userKeys >= (opt.isAll() ? 1 : opt.getAmount())) {
                        lore.add(TextUtil.parse("<yellow>► Click to open!"));
                    } else {
                        lore.add(TextUtil.parse("<red>✖ Not enough keys!"));
                    }
                    meta.lore(lore);
                }
                item.setItemMeta(meta);
            }

            setButton(slot, new Button(item, event -> {
                player.closeInventory();
                if (userKeys >= (opt.isAll() ? 1 : opt.getAmount())) {
                    massOpeningService.startMassOpening(player, crate, opt.getAmount());
                }
            }));
        }
    }
}
