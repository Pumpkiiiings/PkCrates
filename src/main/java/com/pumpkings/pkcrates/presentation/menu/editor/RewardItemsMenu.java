package com.pumpkings.pkcrates.presentation.menu.editor;

import com.pumpkings.pkcrates.core.model.Crate;
import com.pumpkings.pkcrates.core.model.reward.UnifiedReward;
import com.pumpkings.pkcrates.infrastructure.config.CrateRegistry;
import com.pumpkings.pkcrates.presentation.menu.MenuManager;
import com.pumpkings.pkcrates.presentation.menu.PkMenu;
import com.pumpkings.pkcrates.presentation.utils.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import com.pumpkings.pkcrates.infrastructure.config.MenuConfig;
import java.util.ArrayList;
import java.util.List;

public class RewardItemsMenu extends PkMenu {

    private final Plugin plugin;
    private final CrateRegistry crateRegistry;
    private final Crate crate;
    private final UnifiedReward reward;
    private final PkMenu parentMenu;

    public RewardItemsMenu(MenuManager menuManager, Player player, Plugin plugin, CrateRegistry crateRegistry, Crate crate, UnifiedReward reward, PkMenu parentMenu) {
        super(menuManager, player);
        this.plugin = plugin;
        this.crateRegistry = crateRegistry;
        this.crate = crate;
        this.reward = reward;
        this.parentMenu = parentMenu;
    }

    @Override
    public Component getTitle() {
        MenuConfig config = menuManager.getMenuConfigManager().getMenu("reward_items");
        return config != null ? config.getTitle(null) : TextUtil.parse("<dark_red>Place Physical Items Here");
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public boolean isCancelClicks() {
        return false; // Crucial: Allows admin to drag and drop items from their inventory
    }

    @Override
    public void decorate() {
        List<ItemStack> items = reward.getItems();
        if (items != null) {
            for (int i = 0; i < items.size() && i < getSize(); i++) {
                getInventory().setItem(i, items.get(i).clone());
            }
        }
    }

    @Override
    public void onClose() {
        List<ItemStack> newItems = new ArrayList<>();
        
        for (int i = 0; i < getSize(); i++) {
            ItemStack item = getInventory().getItem(i);
            if (item != null && !item.getType().isAir()) {
                newItems.add(item.clone());
            }
        }
        
        reward.setItems(newItems);
        crateRegistry.saveCrate(crate);
        
        // Re-open parent menu on close, scheduling it for the next tick to avoid async issues
        org.bukkit.Bukkit.getScheduler().runTask(plugin, parentMenu::open);
    }
}
