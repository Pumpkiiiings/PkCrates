package com.pumpkings.pkcrates.presentation.menu.editor;

import com.pumpkings.pkcrates.core.model.Crate;
import com.pumpkings.pkcrates.core.model.reward.UnifiedReward;
import com.pumpkings.pkcrates.core.model.rarity.Rarity;
import com.pumpkings.pkcrates.api.rarity.RarityService;
import com.pumpkings.pkcrates.infrastructure.config.CrateRegistry;
import com.pumpkings.pkcrates.presentation.menu.Button;
import com.pumpkings.pkcrates.presentation.menu.MenuManager;
import com.pumpkings.pkcrates.presentation.menu.PaginatedPkMenu;
import com.pumpkings.pkcrates.presentation.menu.PkMenu;
import com.pumpkings.pkcrates.presentation.utils.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import com.pumpkings.pkcrates.infrastructure.config.MenuConfig;

import java.util.ArrayList;
import java.util.List;

public class RewardRaritySelectMenu extends PaginatedPkMenu<Rarity> {

    private final Plugin plugin;
    private final Crate crate;
    private final UnifiedReward reward;
    private final CrateRegistry crateRegistry;
    private final RarityService rarityService;
    private final PkMenu parentMenu;

    private final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    public RewardRaritySelectMenu(Plugin plugin, MenuManager menuManager, Player player, Crate crate, UnifiedReward reward, CrateRegistry crateRegistry, RarityService rarityService, PkMenu parentMenu) {
        super(menuManager, player);
        this.plugin = plugin;
        this.crate = crate;
        this.reward = reward;
        this.crateRegistry = crateRegistry;
        this.rarityService = rarityService;
        this.parentMenu = parentMenu;
    }

    @Override
    public Component getTitle() {
        MenuConfig config = menuManager.getMenuConfigManager().getMenu("reward_rarity_select");
        java.util.Map<String, String> placeholders = new java.util.HashMap<>();
        placeholders.put("<reward_id>", reward.getId());
        return config != null ? config.getTitle(placeholders) : TextUtil.parse("<dark_gray>Select Rarity <gray>| <white>" + reward.getId());
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public List<Rarity> getItems() {
        return new ArrayList<>(rarityService.getAll());
    }

    @Override
    public int[] getItemSlots() {
        return SLOTS;
    }

    @Override
    public Button createItemButton(Rarity rarity) {
        Material mat = Material.matchMaterial(rarity.getIcon() != null ? rarity.getIcon() : "NETHER_STAR");
        if (mat == null) mat = Material.NETHER_STAR;

        MenuConfig config = menuManager.getMenuConfigManager().getMenu("reward_rarity_select");
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.parse(rarity.getDisplayName())); // Keep the rarity's custom display name
            
            java.util.Map<String, String> placeholders = new java.util.HashMap<>();
            placeholders.put("<rarity_id>", rarity.getId());
            placeholders.put("<weight>", String.valueOf(rarity.getWeight()));
            
            if (config != null) {
                meta.lore(config.getItemLore("items.rarity_format", placeholders));
            }
            item.setItemMeta(meta);
        }

        return new Button(item, event -> {
            reward.setRarityId(rarity.getId());
            crateRegistry.saveCrate(crate);
            player.sendRichMessage("<color:#00FF45>ʀᴀʀɪᴛʏ ᴀꜱꜱɪɢɴᴇᴅ ꜱᴜᴄᴄᴇꜱꜱꜰᴜʟʟʏ.");
            parentMenu.open();
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
            if (i != 49) setButton(i, bg); // 49 is usually for pagination pages
        }

        MenuConfig config = menuManager.getMenuConfigManager().getMenu("reward_rarity_select");

        // Clear Rarity Option
        Material clearMat = config != null ? config.getItemMaterial("items.clear", Material.BARRIER) : Material.BARRIER;
        ItemStack clear = new ItemStack(clearMat);
        ItemMeta clearMeta = clear.getItemMeta();
        if (clearMeta != null) {
            if (config != null) {
                clearMeta.displayName(config.getItemName("items.clear", null));
                clearMeta.lore(config.getItemLore("items.clear", null));
            }
            clear.setItemMeta(clearMeta);
        }
        setButton(53, new Button(clear, event -> {
            reward.setRarityId(null);
            crateRegistry.saveCrate(crate);
            player.sendRichMessage("<color:#FF2626>ʀᴀʀɪᴛʏ ʀᴇᴍᴏᴠᴇᴅ.");
            parentMenu.open();
        }));

        // Back Button
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
        setButton(45, new Button(back, event -> {
            parentMenu.open();
        }));
    }
}
