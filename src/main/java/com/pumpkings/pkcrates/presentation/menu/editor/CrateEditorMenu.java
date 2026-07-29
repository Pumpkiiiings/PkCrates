package com.pumpkings.pkcrates.presentation.menu.editor;

import com.pumpkings.pkcrates.core.model.Crate;
import com.pumpkings.pkcrates.infrastructure.config.CrateRegistry;
import com.pumpkings.pkcrates.infrastructure.key.KeyRegistry;
import com.pumpkings.pkcrates.infrastructure.location.CrateLocationManager;
import com.pumpkings.pkcrates.presentation.menu.Button;
import com.pumpkings.pkcrates.presentation.menu.MenuManager;
import com.pumpkings.pkcrates.presentation.menu.PkMenu;
import com.pumpkings.pkcrates.presentation.utils.TextUtil;
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

public class CrateEditorMenu extends PkMenu {

    private final Plugin plugin;
    private final Crate crate;
    private final CrateRegistry crateRegistry;
    private final KeyRegistry keyRegistry;
    private final CrateLocationManager locationMgr;
    private final com.pumpkings.pkcrates.infrastructure.display.HologramManager hologramManager;

    public CrateEditorMenu(Plugin plugin, MenuManager menuManager, Player player, Crate crate, CrateRegistry crateRegistry, KeyRegistry keyRegistry, CrateLocationManager locationMgr, com.pumpkings.pkcrates.infrastructure.display.HologramManager hologramManager) {
        super(menuManager, player);
        this.plugin = plugin;
        this.crate = crate;
        this.crateRegistry = crateRegistry;
        this.keyRegistry = keyRegistry;
        this.locationMgr = locationMgr;
        this.hologramManager = hologramManager;
    }

    @Override
    public net.kyori.adventure.text.Component getTitle() {
        MenuConfig config = menuManager.getMenuConfigManager().getMenu("crate_editor");
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("<crate_id>", crate.getId());
        return config != null ? config.getTitle(placeholders) : TextUtil.parse("<dark_gray>Dash > " + crate.getId() + " > Edit");
    }

    @Override
    public int getSize() {
        return 45;
    }

    @Override
    public void decorate() {
        // Decorate background
        ItemStack bgItem = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bgMeta = bgItem.getItemMeta();
        if (bgMeta != null) {
            bgMeta.displayName(net.kyori.adventure.text.Component.empty());
            bgItem.setItemMeta(bgMeta);
        }
        Button bg = Button.visual(bgItem);
        for (int i = 0; i < getSize(); i++) {
            setButton(i, bg);
        }

        MenuConfig config = menuManager.getMenuConfigManager().getMenu("crate_editor");

        // Edit Name (Name Tag)
        Material nameMat = config != null ? config.getItemMaterial("items.name", Material.NAME_TAG) : Material.NAME_TAG;
        ItemStack nameItem = new ItemStack(nameMat);
        ItemMeta nameMeta = nameItem.getItemMeta();
        if (nameMeta != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("<crate_name>", crate.getName());
            if (config != null) {
                nameMeta.displayName(config.getItemName("items.name", placeholders));
                nameMeta.lore(config.getItemLore("items.name", placeholders));
            } else {
                nameMeta.displayName(TextUtil.parse("<yellow><bold>Edit Name"));
                List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                lore.add(TextUtil.parse("<gray>Current name: <white>" + crate.getName()));
                lore.add(net.kyori.adventure.text.Component.empty());
                lore.add(TextUtil.parse("<green>▶ Click to change."));
                nameMeta.lore(lore);
            }
            nameItem.setItemMeta(nameMeta);
        }
        setButton(11, new Button(nameItem, event -> {
            player.closeInventory();
            menuManager.getPromptManager().prompt(player, com.pumpkings.pkcrates.infrastructure.config.Messages.PROMPT_RENAME_CRATE, input -> {
                if (input.equalsIgnoreCase("cancelar")) return;
                crate.setName(input.replace("&", "§"));
                // Auto-Save required in phase 2
                crateRegistry.saveCrate(crate);
                player.sendRichMessage("<green>Name updated.");
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> new CrateEditorMenu(plugin, menuManager, player, crate, crateRegistry, keyRegistry, locationMgr, hologramManager).open());
            });
        }));

        // Edit Rewards (Diamond)
        Material rewardsMat = config != null ? config.getItemMaterial("items.rewards", Material.DIAMOND) : Material.DIAMOND;
        ItemStack rewardsItem = new ItemStack(rewardsMat);
        ItemMeta rewardsMeta = rewardsItem.getItemMeta();
        if (rewardsMeta != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("<rewards_count>", String.valueOf(crate.getRewards().size()));
            if (config != null) {
                rewardsMeta.displayName(config.getItemName("items.rewards", placeholders));
                rewardsMeta.lore(config.getItemLore("items.rewards", placeholders));
            } else {
                rewardsMeta.displayName(TextUtil.parse("<aqua><bold>Edit Rewards"));
                List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                lore.add(TextUtil.parse("<gray>Configured rewards: <white>" + crate.getRewards().size()));
                lore.add(net.kyori.adventure.text.Component.empty());
                lore.add(TextUtil.parse("<green>▶ Click to manage rewards."));
                rewardsMeta.lore(lore);
            }
            rewardsItem.setItemMeta(rewardsMeta);
        }
        setButton(13, new Button(rewardsItem, event -> {
            new CrateRewardsMenu(plugin, menuManager, player, crate, crateRegistry, this).open();
        }));

        // Edit Required Keys (Tripwire Hook)
        Material keysMat = config != null ? config.getItemMaterial("items.keys", Material.TRIPWIRE_HOOK) : Material.TRIPWIRE_HOOK;
        ItemStack keysItem = new ItemStack(keysMat);
        ItemMeta keysMeta = keysItem.getItemMeta();
        if (keysMeta != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("<keys_count>", String.valueOf(crate.getAcceptedKeys().size()));
            if (config != null) {
                keysMeta.displayName(config.getItemName("items.keys", placeholders));
                keysMeta.lore(config.getItemLore("items.keys", placeholders));
            } else {
                keysMeta.displayName(TextUtil.parse("<gold><bold>Required Keys"));
                List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                lore.add(TextUtil.parse("<gray>Currently linked keys: <white>" + crate.getAcceptedKeys().size()));
                lore.add(net.kyori.adventure.text.Component.empty());
                lore.add(TextUtil.parse("<green>▶ Click to assign keys."));
                keysMeta.lore(lore);
            }
            keysItem.setItemMeta(keysMeta);
        }
        setButton(15, new Button(keysItem, event -> {
            new CrateKeysMenu(menuManager, player, crate, crateRegistry, keyRegistry, this).open();
        }));

        // Edit Animations (Firework Rocket)
        Material animMat = config != null ? config.getItemMaterial("items.animation", Material.FIREWORK_ROCKET) : Material.FIREWORK_ROCKET;
        ItemStack animItem = new ItemStack(animMat);
        ItemMeta animMeta = animItem.getItemMeta();
        if (animMeta != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("<animation_id>", crate.getAnimationId() != null ? crate.getAnimationId() : "None");
            if (config != null) {
                animMeta.displayName(config.getItemName("items.animation", placeholders));
                animMeta.lore(config.getItemLore("items.animation", placeholders));
            } else {
                animMeta.displayName(TextUtil.parse("<light_purple><bold>Opening Animation"));
                List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                lore.add(TextUtil.parse("<gray>Current animation: <white>" + (crate.getAnimationId() != null ? crate.getAnimationId() : "None")));
                lore.add(net.kyori.adventure.text.Component.empty());
                lore.add(TextUtil.parse("<green>▶ Click to select."));
                animMeta.lore(lore);
            }
            animItem.setItemMeta(animMeta);
        }
        setButton(29, new Button(animItem, event -> {
            new CrateAnimationMenu(plugin, menuManager, player, crate, crateRegistry, this).open();
        }));

        // Edit Holograms / Physical Locations (Bedrock)
        Material locMat = config != null ? config.getItemMaterial("items.locations", Material.BEDROCK) : Material.BEDROCK;
        ItemStack locItem = new ItemStack(locMat);
        ItemMeta locMeta = locItem.getItemMeta();
        if (locMeta != null) {
            if (config != null) {
                locMeta.displayName(config.getItemName("items.locations", null));
                locMeta.lore(config.getItemLore("items.locations", null));
            } else {
                locMeta.displayName(TextUtil.parse("<gray><bold>Locations and Hologram"));
                List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                lore.add(TextUtil.parse("<gray>Configure the visual hologram"));
                lore.add(TextUtil.parse("<gray>and place crates in the world."));
                lore.add(net.kyori.adventure.text.Component.empty());
                lore.add(TextUtil.parse("<green>▶ Click to manage."));
                locMeta.lore(lore);
            }
            locItem.setItemMeta(locMeta);
        }
        setButton(33, new Button(locItem, event -> {
            new CrateLocationMenu(plugin, menuManager, player, crate, locationMgr, hologramManager, this).open();
        }));

        // Edit Mass Opening (Slot 31)
        Material moMat = config != null ? config.getItemMaterial("items.mass_opening", Material.ENDER_CHEST) : Material.ENDER_CHEST;
        ItemStack moItem = new ItemStack(moMat);
        ItemMeta moMeta = moItem.getItemMeta();
        if (moMeta != null) {
            Map<String, String> placeholders = new HashMap<>();
            boolean moEnabled = crate.getMassOpeningConfig() != null && crate.getMassOpeningConfig().isEnabled();
            placeholders.put("<status>", moEnabled ? "<green>Enabled" : "<red>Disabled");
            if (config != null) {
                moMeta.displayName(config.getItemName("items.mass_opening", placeholders));
                moMeta.lore(config.getItemLore("items.mass_opening", placeholders));
            } else {
                moMeta.displayName(TextUtil.parse("<gradient:#4287f5:#42d4f5><bold>Mass Opening</bold></gradient>"));
                List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                lore.add(TextUtil.parse("<gray>Status: " + (moEnabled ? "<green>Enabled" : "<red>Disabled")));
                lore.add(net.kyori.adventure.text.Component.empty());
                lore.add(TextUtil.parse("<green>▶ Click to configure Mass Opening."));
                moMeta.lore(lore);
            }
            moItem.setItemMeta(moMeta);
        }
        setButton(31, new Button(moItem, event -> {
            new MassOpeningEditorMenu(plugin, menuManager, player, crate, crateRegistry, this).open();
        }));

        // Back button (To return to Dashboard)
        Material backMat = config != null ? config.getItemMaterial("items.back", Material.ARROW) : Material.ARROW;
        ItemStack back = new ItemStack(backMat);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            if (config != null) {
                backMeta.displayName(config.getItemName("items.back", null));
                backMeta.lore(config.getItemLore("items.back", null));
            } else {
                backMeta.displayName(TextUtil.parse("<red>Return to Dashboard"));
            }
            back.setItemMeta(backMeta);
        }
        setButton(36, new Button(back, event -> {
            new CratesDashboardMenu(plugin, menuManager, player, crateRegistry, keyRegistry, locationMgr, hologramManager).open();
        }));
    }
}
