package com.pumpkings.pkcrates.presentation.menu.editor;

import com.pumpkings.pkcrates.core.model.Crate;
import com.pumpkings.pkcrates.core.model.reward.UnifiedReward;
import com.pumpkings.pkcrates.infrastructure.config.CrateRegistry;
import com.pumpkings.pkcrates.infrastructure.item.ItemBuilder;
import com.pumpkings.pkcrates.presentation.menu.Button;
import com.pumpkings.pkcrates.presentation.menu.MenuManager;
import com.pumpkings.pkcrates.presentation.menu.PkMenu;
import com.pumpkings.pkcrates.presentation.utils.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import com.pumpkings.pkcrates.infrastructure.config.MenuConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.List;

public class RewardEditorMenu extends PkMenu {

    private final Plugin plugin;
    private final Crate crate;
    private final UnifiedReward reward;
    private final CrateRegistry crateRegistry;
    private final PkMenu parentMenu;

    public RewardEditorMenu(Plugin plugin, MenuManager menuManager, Player player, Crate crate, UnifiedReward reward, CrateRegistry crateRegistry, PkMenu parentMenu) {
        super(menuManager, player);
        this.plugin = plugin;
        this.crate = crate;
        this.reward = reward;
        this.crateRegistry = crateRegistry;
        this.parentMenu = parentMenu;
    }

    @Override
    public Component getTitle() {
        MenuConfig config = menuManager.getMenuConfigManager().getMenu("reward_editor");
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("<reward_id>", reward.getId());
        return config != null ? config.getTitle(placeholders) : TextUtil.parse("<dark_gray>Editing Reward: <yellow>" + reward.getId());
    }

    @Override
    public int getSize() {
        return 27;
    }

    @Override
    public void decorate() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            filler.setItemMeta(meta);
        }
        Button bg = Button.visual(filler);
        for (int i = 0; i < getSize(); i++) setButton(i, bg);

        MenuConfig config = menuManager.getMenuConfigManager().getMenu("reward_editor");

        // Edit Weight
        Material weightMat = config != null ? config.getItemMaterial("items.weight", Material.GOLD_INGOT) : Material.GOLD_INGOT;
        ItemStack weightItem = new ItemStack(weightMat);
        ItemMeta weightMeta = weightItem.getItemMeta();
        if (weightMeta != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("<weight>", String.valueOf(reward.getWeight()));
            if (config != null) {
                weightMeta.displayName(config.getItemName("items.weight", placeholders));
                weightMeta.lore(config.getItemLore("items.weight", placeholders));
            }
            weightItem.setItemMeta(weightMeta);
        }
        setButton(10, new Button(weightItem, event -> {
            double change = 0;
            if (event.getClick() == ClickType.LEFT) change = 1.0;
            else if (event.getClick() == ClickType.RIGHT) change = -1.0;
            else if (event.getClick() == ClickType.SHIFT_LEFT) change = 10.0;
            else if (event.getClick() == ClickType.SHIFT_RIGHT) change = -10.0;
            
            if (change != 0) {
                reward.setWeight(Math.max(0, reward.getWeight() + change));
                crate.removeReward(reward);
                crate.addReward(reward); // recalculate totalWeight
                crateRegistry.saveCrate(crate);
                this.open();
            }
        }));

        // Select Rarity
        com.pumpkings.pkcrates.api.rarity.RarityService rarityService = ((com.pumpkings.pkcrates.PkCratesPlugin) plugin).getRarityService();
        com.pumpkings.pkcrates.infrastructure.config.MessageManager messageManager = ((com.pumpkings.pkcrates.PkCratesPlugin) plugin).getMessageManager();
        
        Material rarityMat = config != null ? config.getItemMaterial("items.rarity", Material.NETHER_STAR) : Material.NETHER_STAR;
        ItemStack rarityIcon = new ItemStack(rarityMat);
        ItemMeta rarityMeta = rarityIcon.getItemMeta();
        if (rarityMeta != null) {
            String currentRarity = reward.getRarityId() != null ? reward.getRarityId() : "NONE";
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("<current_rarity>", currentRarity);
            if (config != null) {
                rarityMeta.displayName(config.getItemName("items.rarity", placeholders));
                rarityMeta.lore(config.getItemLore("items.rarity", placeholders));
            }
            rarityIcon.setItemMeta(rarityMeta);
        }
        setButton(11, new Button(rarityIcon, event -> {
            if (event.isRightClick()) {
                reward.setRarityId(null);
                crateRegistry.saveCrate(crate);
                messageManager.sendMessage(player, com.pumpkings.pkcrates.infrastructure.config.Messages.RARITY_REMOVED);
                this.open();
            } else {
                new RewardRaritySelectMenu(plugin, menuManager, player, crate, reward, crateRegistry, rarityService, this).open();
            }
        }));
        
        // Broadcast Toggle
        Material broadcastMat = config != null ? config.getItemMaterial("items.broadcast", Material.OAK_SIGN) : Material.OAK_SIGN;
        ItemStack broadcastIcon = new ItemStack(broadcastMat);
        ItemMeta broadcastMeta = broadcastIcon.getItemMeta();
        if (broadcastMeta != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("<broadcast_status>", reward.isBroadcastEnabled() ? "<color:#00FF45>ᴇɴᴀʙʟᴇᴅ</color>" : "<color:#FF2626>ᴅɪꜱᴀʙʟᴇᴅ</color>");
            if (config != null) {
                broadcastMeta.displayName(config.getItemName("items.broadcast", placeholders));
                broadcastMeta.lore(config.getItemLore("items.broadcast", placeholders));
            }
            broadcastIcon.setItemMeta(broadcastMeta);
        }
        setButton(12, new Button(broadcastIcon, event -> {
            reward.setBroadcastEnabled(!reward.isBroadcastEnabled());
            crateRegistry.saveCrate(crate);
            this.open();
        }));

        // Edit Commands
        Material cmdMat = config != null ? config.getItemMaterial("items.commands", Material.COMMAND_BLOCK) : Material.COMMAND_BLOCK;
        ItemStack cmdItem = new ItemStack(cmdMat);
        ItemMeta cmdMeta = cmdItem.getItemMeta();
        if (cmdMeta != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("<cmd_count>", String.valueOf(reward.getCommands() == null ? 0 : reward.getCommands().size()));
            if (config != null) {
                cmdMeta.displayName(config.getItemName("items.commands", placeholders));
                cmdMeta.lore(config.getItemLore("items.commands", placeholders));
            }
            cmdItem.setItemMeta(cmdMeta);
        }
        setButton(12, new Button(cmdItem, event -> {
            if (event.getClick() == ClickType.SHIFT_RIGHT || event.getClick() == ClickType.SHIFT_LEFT) {
                reward.setCommands(new ArrayList<>());
                crateRegistry.saveCrate(crate);
                player.sendRichMessage("<red>All commands removed.");
                this.open();
                return;
            }
            player.closeInventory();
            menuManager.getPromptManager().prompt(player, com.pumpkings.pkcrates.infrastructure.config.Messages.PROMPT_REWARD_COMMAND, input -> {
                if (input.equalsIgnoreCase("cancelar")) {
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, this::open);
                    return;
                }
                List<String> commands = reward.getCommands() != null ? new ArrayList<>(reward.getCommands()) : new ArrayList<>();
                commands.add(input);
                reward.setCommands(commands);
                crateRegistry.saveCrate(crate);
                player.sendRichMessage("<green>Command added successfully.</green>");
                org.bukkit.Bukkit.getScheduler().runTask(plugin, this::open);
            });
        }));

        // Display Item
        Material displayMat = config != null ? config.getItemMaterial("items.display", Material.ITEM_FRAME) : Material.ITEM_FRAME;
        ItemStack displayItemIcon = new ItemStack(displayMat);
        ItemMeta displayMeta = displayItemIcon.getItemMeta();
        if (displayMeta != null) {
            if (config != null) {
                displayMeta.displayName(config.getItemName("items.display", null));
                displayMeta.lore(config.getItemLore("items.display", null));
            }
            displayItemIcon.setItemMeta(displayMeta);
        }
        setButton(14, new Button(displayItemIcon, event -> {
            ItemStack cursor = event.getCursor();
            if (cursor != null && !cursor.getType().isAir()) {
                reward.setDisplayItem(cursor.clone());
                crateRegistry.saveCrate(crate);
                player.sendRichMessage("<green>Visual item updated.");
                this.open();
            } else {
                player.sendRichMessage("<red>You must hold an item with the cursor and click here.");
            }
        }));

        // Edit Items
        Material itemsMat = config != null ? config.getItemMaterial("items.items", Material.CHEST) : Material.CHEST;
        ItemStack itemsIcon = new ItemStack(itemsMat);
        ItemMeta itemsMeta = itemsIcon.getItemMeta();
        if (itemsMeta != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("<items_count>", String.valueOf(reward.getItems() == null ? 0 : reward.getItems().size()));
            if (config != null) {
                itemsMeta.displayName(config.getItemName("items.items", placeholders));
                itemsMeta.lore(config.getItemLore("items.items", placeholders));
            }
            itemsIcon.setItemMeta(itemsMeta);
        }
        setButton(16, new Button(itemsIcon, event -> {
            new RewardItemsMenu(menuManager, player, plugin, crateRegistry, crate, reward, this).open();
        }));

        // Back
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
        setButton(26, new Button(back, event -> parentMenu.open()));
    }
}
