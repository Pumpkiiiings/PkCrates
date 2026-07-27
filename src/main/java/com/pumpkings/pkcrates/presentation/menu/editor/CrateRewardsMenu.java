package com.pumpkings.pkcrates.presentation.menu.editor;

import com.pumpkings.pkcrates.core.model.Crate;
import com.pumpkings.pkcrates.core.model.reward.IReward;
import com.pumpkings.pkcrates.core.model.reward.UnifiedReward;
import com.pumpkings.pkcrates.infrastructure.config.CrateRegistry;
import com.pumpkings.pkcrates.infrastructure.item.ItemBuilder;
import com.pumpkings.pkcrates.presentation.menu.Button;
import com.pumpkings.pkcrates.presentation.menu.MenuManager;
import com.pumpkings.pkcrates.presentation.menu.PaginatedPkMenu;
import com.pumpkings.pkcrates.presentation.menu.PkMenu;
import com.pumpkings.pkcrates.presentation.utils.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import com.pumpkings.pkcrates.infrastructure.config.MenuConfig;
import java.util.HashMap;
import java.util.Map;

import java.util.ArrayList;
import java.util.List;

public class CrateRewardsMenu extends PaginatedPkMenu<IReward> {

    private final Plugin plugin;
    private final Crate crate;
    private final CrateRegistry crateRegistry;
    private final PkMenu parentMenu;

    private final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    public CrateRewardsMenu(Plugin plugin, MenuManager menuManager, Player player, Crate crate, CrateRegistry crateRegistry, PkMenu parentMenu) {
        super(menuManager, player);
        this.plugin = plugin;
        this.crate = crate;
        this.crateRegistry = crateRegistry;
        this.parentMenu = parentMenu;
    }

    @Override
    public Component getTitle() {
        MenuConfig config = menuManager.getMenuConfigManager().getMenu("crate_rewards");
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("<crate_id>", crate.getId());
        return config != null ? config.getTitle(placeholders) : TextUtil.parse("<dark_gray>Editor > <yellow>" + crate.getId() + " <dark_gray>> Rewards");
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public List<IReward> getItems() {
        return new ArrayList<>(crate.getRewards());
    }

    @Override
    public int[] getItemSlots() {
        return SLOTS;
    }

    @Override
    public Button createItemButton(IReward reward) {
        MenuConfig config = menuManager.getMenuConfigManager().getMenu("crate_rewards");
        ItemStack display = reward instanceof UnifiedReward ur && ur.getDisplayItem() != null
                ? ur.getDisplayItem().clone()
                : new ItemStack(Material.PAPER);

        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.lore();
            if (lore == null) lore = new ArrayList<>();
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("<weight>", String.valueOf(reward.getWeight()));
            placeholders.put("<limit>", reward.getWinLimit() > 0 ? String.valueOf(reward.getWinLimit()) : "Unlimited");
            
            if (config != null) {
                lore.addAll(config.getItemLore("items.reward_format_append", placeholders));
            } else {
                lore.add(Component.empty());
                lore.add(TextUtil.parse("<gray>Weight (Probability): <yellow>" + reward.getWeight()));
                lore.add(TextUtil.parse("<gray>Global Limit: <yellow>" + (reward.getWinLimit() > 0 ? reward.getWinLimit() : "Unlimited")));
                lore.add(Component.empty());
                lore.add(TextUtil.parse("<green>▶ Left-Click to edit reward"));
                lore.add(TextUtil.parse("<red>▶ Shift-Right-Click to remove reward"));
            }
            meta.lore(lore);
            display.setItemMeta(meta);
        }

        return new Button(display, event -> {
            if (event.getClick() == ClickType.SHIFT_RIGHT) {
                crate.removeReward(reward);
                crateRegistry.saveCrate(crate);
                player.sendRichMessage("<red>Reward removed.");
                decorate();
            } else if (event.getClick() == ClickType.LEFT) {
                if (reward instanceof UnifiedReward ur) {
                    new RewardEditorMenu(plugin, menuManager, player, crate, ur, crateRegistry, this).open();
                } else {
                    player.sendRichMessage("<red>You can only edit unified rewards.");
                }
            }
        });
    }

    @Override
    protected void addDecorations() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            filler.setItemMeta(meta);
        }
        Button bg = Button.visual(filler);

        for (int i = 0; i < 9; i++) setButton(i, bg);
        for (int i = 45; i < 54; i++) {
            if (i != 49) setButton(i, bg);
        }

        MenuConfig config = menuManager.getMenuConfigManager().getMenu("crate_rewards");

        // Help button (45)
        Material helpMat = config != null ? config.getItemMaterial("items.help", Material.BOOK) : Material.BOOK;
        ItemStack help = new ItemStack(helpMat);
        ItemMeta helpMeta = help.getItemMeta();
        if (helpMeta != null) {
            if (config != null) {
                helpMeta.displayName(config.getItemName("items.help", null));
                helpMeta.lore(config.getItemLore("items.help", null));
            } else {
                helpMeta.displayName(TextUtil.parse("<aqua><bold>How to create rewards?"));
                List<Component> lore = new ArrayList<>();
                lore.add(TextUtil.parse("<gray>Open your inventory (E) and click"));
                lore.add(TextUtil.parse("<gray>on any item to add it"));
                lore.add(TextUtil.parse("<gray>as a new reward automatically."));
                helpMeta.lore(lore);
            }
            help.setItemMeta(helpMeta);
        }
        setButton(45, Button.visual(help));

        // Back button overrides close button (49)
        Material backMat = config != null ? config.getItemMaterial("items.back", Material.ARROW) : Material.ARROW;
        ItemStack back = new ItemStack(backMat);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            if (config != null) {
                backMeta.displayName(config.getItemName("items.back", null));
                backMeta.lore(config.getItemLore("items.back", null));
            } else {
                backMeta.displayName(TextUtil.parse("<red>Return to Crate Editor"));
            }
            back.setItemMeta(backMeta);
        }
        setButton(49, new Button(back, event -> parentMenu.open()));
    }

    @Override
    public void onBottomClick(InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType().isAir()) return;

        // Auto-create reward via Drop & Drop from bottom inventory!
        String newId = "reward_" + (crate.getRewards().size() + 1) + "_" + System.currentTimeMillis();
        
        List<ItemStack> items = new ArrayList<>();
        items.add(clickedItem.clone());
        
        UnifiedReward newReward = new UnifiedReward(
                newId, 10.0, clickedItem.clone(), -1, null, items, new ArrayList<>()
        );
        
        crate.addReward(newReward);
        crateRegistry.saveCrate(crate);
        
        String itemName = clickedItem.getItemMeta() != null && clickedItem.getItemMeta().hasDisplayName() ? clickedItem.getItemMeta().getDisplayName() : clickedItem.getType().name();
        player.sendMessage(TextUtil.parse("<green>Reward '" + itemName + "' successfully added!</green>"));
        this.open();
    }
}
