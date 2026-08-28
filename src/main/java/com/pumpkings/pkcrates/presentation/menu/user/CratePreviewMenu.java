package com.pumpkings.pkcrates.presentation.menu.user;

import com.pumpkings.pkcrates.core.model.Crate;
import com.pumpkings.pkcrates.core.model.reward.IReward;
import com.pumpkings.pkcrates.presentation.menu.Button;
import com.pumpkings.pkcrates.presentation.menu.MenuManager;
import com.pumpkings.pkcrates.presentation.menu.PaginatedPkMenu;
import com.pumpkings.pkcrates.presentation.utils.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.pumpkings.pkcrates.infrastructure.config.MenuConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class CratePreviewMenu extends PaginatedPkMenu<IReward> {

    private final Crate crate;
    private final List<IReward> sortedRewards;
    private final double totalWeight;
    
    // Slots configured according to spec
    private final int[] SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    };

    public CratePreviewMenu(MenuManager menuManager, Player player, Crate crate) {
        super(menuManager, player);
        this.crate = crate;
        
        // Cache list and weight on open
        this.sortedRewards = new ArrayList<>(crate.getRewards());
        this.sortedRewards.sort((r1, r2) -> Double.compare(r2.getWeight(), r1.getWeight()));
        this.totalWeight = sortedRewards.stream().mapToDouble(IReward::getWeight).sum();
    }

    @Override
    public Component getTitle() {
        MenuConfig config = menuManager.getMenuConfigManager().getMenu("crate_preview");
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("<crate_name>", crate.getName());
        return config != null ? config.getTitle(placeholders) : TextUtil.parse("<dark_gray><underlined>Previewing <blue>" + crate.getName());
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public List<IReward> getItems() {
        return sortedRewards;
    }

    @Override
    public int[] getItemSlots() {
        return SLOTS;
    }

    @Override
    public Button createItemButton(IReward reward) {
        MenuConfig config = menuManager.getMenuConfigManager().getMenu("crate_preview");
        double chance = (totalWeight > 0) ? (reward.getWeight() / totalWeight) * 100 : 0;
        String chanceFormatted = String.format("%.2f", chance);

        ItemStack displayItem;
        if (reward instanceof com.pumpkings.pkcrates.core.model.reward.UnifiedReward unifiedReward) {
            displayItem = unifiedReward.getDisplayItem().clone();
        } else {
            displayItem = new ItemStack(Material.STONE);
        }
        
        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.lore();
            if (lore == null) lore = new ArrayList<>();
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("<chance>", chanceFormatted);
            if (config != null) {
                lore.addAll(config.getItemLore("items.reward_format_append", placeholders));
            }
            
            meta.lore(lore);
            displayItem.setItemMeta(meta);
        }

        return new Button(displayItem, event -> {
            event.setCancelled(true); // Read only
        });
    }

    @Override
    protected void addDecorations() {
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            filler.setItemMeta(meta);
        }

        // Bottom decoration according to spec (45 to 53, omitting 49 which is close)
        for (int i = 45; i <= 53; i++) {
            if (i != 49) {
                setButton(i, Button.visual(filler));
            }
        }
    }
}
