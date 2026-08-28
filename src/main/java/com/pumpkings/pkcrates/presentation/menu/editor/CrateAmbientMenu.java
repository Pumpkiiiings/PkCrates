package com.pumpkings.pkcrates.presentation.menu.editor;

import com.pumpkings.pkcrates.core.ambient.AmbientEffect;
import com.pumpkings.pkcrates.core.model.Crate;
import com.pumpkings.pkcrates.infrastructure.config.CrateRegistry;
import com.pumpkings.pkcrates.presentation.menu.Button;
import com.pumpkings.pkcrates.presentation.menu.MenuManager;
import com.pumpkings.pkcrates.presentation.menu.PaginatedPkMenu;
import com.pumpkings.pkcrates.presentation.menu.PkMenu;
import com.pumpkings.pkcrates.presentation.utils.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.pumpkings.pkcrates.PkCratesPlugin;
import org.bukkit.plugin.Plugin;
import com.pumpkings.pkcrates.infrastructure.config.MenuConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Menu for selecting the ambient particle effect of a placed crate block.
 * Follows the same pattern as {@link CrateAnimationMenu}.
 */
public class CrateAmbientMenu extends PaginatedPkMenu<AmbientEffect> {

    private final PkCratesPlugin plugin;
    private final Crate crate;
    private final CrateRegistry crateRegistry;
    private final PkMenu parentMenu;

    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    public CrateAmbientMenu(Plugin plugin, MenuManager menuManager, Player player,
                            Crate crate, CrateRegistry crateRegistry, PkMenu parentMenu) {
        super(menuManager, player);
        this.plugin       = (PkCratesPlugin) plugin;
        this.crate        = crate;
        this.crateRegistry = crateRegistry;
        this.parentMenu   = parentMenu;
    }

    // -----------------------------------------------------------------------

    @Override
    public net.kyori.adventure.text.Component getTitle() {
        MenuConfig config = menuManager.getMenuConfigManager().getMenu("crate_ambient");
        Map<String, String> ph = new HashMap<>();
        ph.put("<crate_id>", crate.getId());
        return config != null
                ? config.getTitle(ph)
                : TextUtil.parse("<dark_gray>ᴀᴍʙɪᴇɴᴛ ᴇꜰꜰᴇᴄᴛ ꜰᴏʀ: <color:#00CFFF>" + crate.getId());
    }

    @Override
    public int getSize() { return 54; }

    // -----------------------------------------------------------------------

    @Override
    public List<AmbientEffect> getItems() {
        return Arrays.asList(AmbientEffect.values());
    }

    @Override
    public int[] getItemSlots() { return SLOTS; }

    // -----------------------------------------------------------------------

    @Override
    public Button createItemButton(AmbientEffect effect) {
        MenuConfig config = menuManager.getMenuConfigManager().getMenu("crate_ambient");
        boolean isSelected = crate.getAmbientEffect() == effect;

        Material mat = isSelected ? effectMaterialSelected(effect) : effectMaterial(effect);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            Map<String, String> ph = new HashMap<>();
            ph.put("<effect_id>", effect.name());
            ph.put("<effect_desc>", effectDescription(effect));

            if (config != null) {
                meta.displayName(config.getItemName("items.effect_format", ph));
                String lorePath = isSelected ? "items.effect_format.active_lore" : "items.effect_format.inactive_lore";
                meta.lore(config.getExactLore(lorePath, ph));
            } else {
                // Fallback if no config file
                meta.displayName(TextUtil.parse(
                        "<color:#00CFFF><bold>" + effect.name()));
                List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                lore.add(TextUtil.parse("<gray>" + effectDescription(effect)));
                lore.add(net.kyori.adventure.text.Component.empty());
                if (isSelected) {
                    lore.add(TextUtil.parse("<color:#00FF45>▶ ꜱᴇʟᴇᴄᴛᴇᴅ"));
                } else {
                    lore.add(TextUtil.parse("<gray>▶ ᴄʟɪᴄᴋ ᴛᴏ ꜱᴇʟᴇᴄᴛ"));
                }
                meta.lore(lore);
            }

            if (isSelected) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }

        return new Button(item, event -> {
            if (!isSelected) {
                crate.setAmbientEffect(effect);
                crateRegistry.saveCrate(crate);
                player.sendRichMessage("<green>Ambient effect updated to: <white>" + effect.name());
                decorate(); // Refresh
            }
        });
    }

    // -----------------------------------------------------------------------

    @Override
    protected void addDecorations() {
        // Bottom bar glass
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.displayName(net.kyori.adventure.text.Component.empty());
            glass.setItemMeta(glassMeta);
        }
        for (int i = 45; i <= 53; i++) {
            if (i != 45) setButton(i, Button.visual(glass));
        }

        MenuConfig config = menuManager.getMenuConfigManager().getMenu("crate_ambient");

        // Back button
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
        setButton(45, new Button(back, event -> parentMenu.open()));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Returns a representative material for each effect (unselected). */
    private Material effectMaterial(AmbientEffect effect) {
        return switch (effect) {
            case ENCHANT     -> Material.ENCHANTING_TABLE;
            case FLAME_RING  -> Material.BLAZE_POWDER;
            case SNOWFALL    -> Material.SNOWBALL;
            case PORTAL_RING -> Material.ENDER_EYE;
            case STAR_BURST  -> Material.END_CRYSTAL;
            case TOTEM       -> Material.TOTEM_OF_UNDYING;
            case RAINBOW     -> Material.PRISMARINE_CRYSTALS;
            case SOUL_FLAME  -> Material.SOUL_CAMPFIRE;
            case VOID        -> Material.DRAGON_BREATH;
            case HEARTS      -> Material.POPPY;
            case CHERRY_BLOSSOM -> Material.CHERRY_LEAVES;
            case SCULK       -> Material.SCULK_CATALYST;
            case ELECTRIC    -> Material.LIGHTNING_ROD;
            case WATER       -> Material.WATER_BUCKET;
            case HONEY       -> Material.HONEY_BOTTLE;
            case NONE        -> Material.BARRIER;
            default          -> Material.BARRIER;
        };
    }

    /** Returns the selected highlight material for each effect. */
    private Material effectMaterialSelected(AmbientEffect effect) {
        return switch (effect) {
            case ENCHANT     -> Material.EXPERIENCE_BOTTLE;
            case FLAME_RING  -> Material.FIRE_CHARGE;
            case SNOWFALL    -> Material.PACKED_ICE;
            case PORTAL_RING -> Material.ENDER_PEARL;
            case STAR_BURST  -> Material.NETHER_STAR;
            case TOTEM       -> Material.TOTEM_OF_UNDYING;
            case RAINBOW     -> Material.HEART_OF_THE_SEA;
            case SOUL_FLAME  -> Material.SOUL_TORCH;
            case VOID        -> Material.ENDER_CHEST;
            case HEARTS      -> Material.RED_TULIP;
            case CHERRY_BLOSSOM -> Material.PINK_PETALS;
            case SCULK       -> Material.SCULK_SHRIEKER;
            case ELECTRIC    -> Material.COPPER_BLOCK;
            case WATER       -> Material.KELP;
            case HONEY       -> Material.HONEYCOMB;
            case NONE        -> Material.BLACK_STAINED_GLASS_PANE;
            default          -> Material.BLACK_STAINED_GLASS_PANE;
        };
    }

    /** Short human-readable description per effect, shown in lore. */
    private String effectDescription(AmbientEffect effect) {
        return switch (effect) {
            case ENCHANT     -> "Orbiting enchantment glyphs";
            case FLAME_RING  -> "Spinning ring of flames";
            case SNOWFALL    -> "Gentle snowflakes & frost";
            case PORTAL_RING -> "Pulsing portal ring above";
            case STAR_BURST  -> "Rising star-burst particles";
            case TOTEM       -> "Divine totem columns";
            case RAINBOW     -> "Colorful rainbow dust orbit";
            case SOUL_FLAME  -> "Blue soul flame & smoke";
            case VOID        -> "Void portal & dragon breath";
            case HEARTS      -> "Floating romantic hearts";
            case CHERRY_BLOSSOM -> "Falling cherry petals";
            case SCULK       -> "Sculk souls and charge";
            case ELECTRIC    -> "Electric sparks";
            case WATER       -> "Water bubbles and splashes";
            case HONEY       -> "Falling honey drips";
            case NONE        -> "No ambient particles";
            default          -> "Unknown effect";
        };
    }
}
