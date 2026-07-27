package com.pumpkings.pkcrates.presentation.menu.user;

import com.pumpkings.pkcrates.core.model.claim.ClaimResult;
import com.pumpkings.pkcrates.core.model.claim.ClaimedReward;
import com.pumpkings.pkcrates.core.service.ClaimService;
import com.pumpkings.pkcrates.infrastructure.claim.ClaimConfig;
import com.pumpkings.pkcrates.infrastructure.config.MenuConfig;
import com.pumpkings.pkcrates.infrastructure.config.MessageManager;
import com.pumpkings.pkcrates.infrastructure.config.Messages;
import com.pumpkings.pkcrates.presentation.menu.Button;
import com.pumpkings.pkcrates.presentation.menu.MenuManager;
import com.pumpkings.pkcrates.presentation.menu.PaginatedPkMenu;
import com.pumpkings.pkcrates.presentation.utils.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Player-facing paginated menu that shows all pending claimed rewards.
 *
 * <p>Layout (54 slots):</p>
 * <pre>
 * Row 0 (0–8):   top decoration + info button (slot 4)
 * Row 1–3 (9–35): paginated reward items in slots {@link #ITEM_SLOTS}
 * Row 4 (36–44): (reserved / unused)
 * Row 5 (45–53): Prev (45) · empty · Claim All (49) · empty · Next (53) · Close (hidden by parent at 49 → we override)
 * </pre>
 *
 * <p>Actual control slots are managed by {@link com.pumpkings.pkcrates.presentation.menu.PaginatedPkMenu}.
 * We override slot 49 with "Claim All" and add "Close" at slot 53.</p>
 */
public class ClaimMenu extends PaginatedPkMenu<ClaimedReward> {

    /** Date format used to display when the reward was stored. */
    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("dd/MM/yyyy HH:mm");

    /** Grid of slots where claim reward items are rendered. */
    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final ClaimService claimService;
    private final ClaimConfig claimConfig;
    private final MessageManager messageManager;

    /**
     * Constructs the claim menu for the given player.
     *
     * @param menuManager    The global menu manager (for registration).
     * @param player         The player whose claims are displayed.
     * @param claimService   Service used to perform claim operations.
     * @param claimConfig    Configuration for the claim module.
     * @param messageManager Used to send feedback messages to the player.
     */
    public ClaimMenu(MenuManager menuManager, Player player,
                     ClaimService claimService, ClaimConfig claimConfig,
                     MessageManager messageManager) {
        super(menuManager, player);
        this.claimService = claimService;
        this.claimConfig = claimConfig;
        this.messageManager = messageManager;
    }

    // -------------------------------------------------------------------------
    // PaginatedPkMenu contract
    // -------------------------------------------------------------------------

    @Override
    public Component getTitle() {
        MenuConfig cfg = cfg();
        return cfg != null
                ? cfg.getTitle(null)
                : TextUtil.parse("<gold><bold>✦ Pending Rewards</bold></gold>");
    }

    @Override
    public int getSize() { return 54; }

    @Override
    public List<ClaimedReward> getItems() {
        return claimService.getClaims(player.getUniqueId());
    }

    @Override
    public int[] getItemSlots() { return ITEM_SLOTS; }

    // -------------------------------------------------------------------------
    // Reward button
    // -------------------------------------------------------------------------

    @Override
    public Button createItemButton(ClaimedReward claim) {
        MenuConfig cfg = cfg();

        // Build the display item — use the stored preview item, or a fallback
        ItemStack displayItem = claim.getPreviewItem();
        if (displayItem == null || displayItem.getType() == Material.AIR) {
            displayItem = new ItemStack(Material.CHEST_MINECART);
        }

        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {
            // Keep the original item name — only append extra lore
            List<Component> lore = meta.lore();
            if (lore == null) lore = new ArrayList<>();

            Map<String, String> placeholders = buildPlaceholders(claim);

            if (cfg != null) {
                lore.addAll(cfg.getExactLore("items.claim_format.lore", placeholders));
            } else {
                lore.add(Component.empty());
                lore.add(TextUtil.parse("<gray>Crate: <gold>" + claim.getCrateId()));
                lore.add(TextUtil.parse("<gray>Stored: <aqua>"
                        + DATE_FMT.format(new Date(claim.getStoredAt()))));
                lore.add(Component.empty());
                lore.add(TextUtil.parse("<green>▶ Click to claim"));
            }
            meta.lore(lore);
            displayItem.setItemMeta(meta);
        }

        // Click action → attempt to deliver
        return new Button(displayItem, event -> {
            ClaimResult result = claimService.claim(player, claim.getId());
            if (result.isSuccess()) {
                messageManager.sendMessage(player, Messages.CLAIM_REWARD_CLAIMED);
            } else {
                messageManager.sendMessage(player, Messages.CLAIM_INVENTORY_FULL);
            }
            decorate(); // Refresh menu
        });
    }

    // -------------------------------------------------------------------------
    // Decorations (top row + bottom row)
    // -------------------------------------------------------------------------

    @Override
    protected void addDecorations() {
        MenuConfig cfg = cfg();

        // ── Top border (row 0) filled with dark glass ──
        ItemStack topFiller = fillerPane(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < 9; slot++) {
            if (slot != 4) setButton(slot, Button.visual(topFiller));
        }

        // ── Info button — slot 4 ──
        addInfoButton(cfg, slot4 -> {});

        // ── Middle spacer border (between grid and bottom row) ──
        ItemStack midFiller = fillerPane(Material.BLACK_STAINED_GLASS_PANE);
        for (int slot = 36; slot < 45; slot++) {
            setButton(slot, Button.visual(midFiller));
        }

        // ── Bottom border slots (row 5) except nav/claim-all/close ──
        ItemStack btmFiller = fillerPane(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 45; slot <= 53; slot++) {
            setButton(slot, Button.visual(btmFiller));
        }

        // ── Empty-state placeholder when no claims exist ──
        List<ClaimedReward> claims = getItems();
        if (claims.isEmpty()) {
            addEmptyPlaceholder(cfg);
        }

        // ── Claim All button — slot 49 ──
        addClaimAllButton(cfg);

        // ── Close button — slot 53 ──
        addCloseButton(cfg);
    }

    /**
     * Overrides the parent's {@code renderControls} so that the Prev/Next
     * arrows go to slots 45 and 47 instead of 18/26, freeing the inner grid.
     */
    @Override
    protected void renderControls(int totalPages) {
        // Previous page — slot 45
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            if (prevMeta != null) {
                prevMeta.displayName(TextUtil.parse("<red>◀ Previous"));
                prev.setItemMeta(prevMeta);
            }
            setButton(45, new Button(prev, e -> { page--; decorate(); }));
        } else {
            setButton(45, Button.visual(fillerPane(Material.GRAY_STAINED_GLASS_PANE)));
        }

        // Next page — slot 47
        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            if (nextMeta != null) {
                nextMeta.displayName(TextUtil.parse("<green>Next ▶"));
                next.setItemMeta(nextMeta);
            }
            setButton(47, new Button(next, e -> { page++; decorate(); }));
        } else {
            setButton(47, Button.visual(fillerPane(Material.GRAY_STAINED_GLASS_PANE)));
        }
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    /** Returns a named filler pane with an invisible display name. */
    private ItemStack fillerPane(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            pane.setItemMeta(meta);
        }
        return pane;
    }

    /** Slot 4 info button showing pending count vs limit. */
    private void addInfoButton(MenuConfig cfg, java.util.function.Consumer<Void> ignored) {
        int pending = claimService.getPendingAmount(player.getUniqueId());
        int effectiveLimit = claimService.getEffectiveLimit(player);
        String maxStr = effectiveLimit < 0 ? "∞" : String.valueOf(effectiveLimit);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("<pending>", String.valueOf(pending));
        placeholders.put("<max>", maxStr);

        Material mat = cfg != null
                ? cfg.getItemMaterial("items.info", Material.PAPER)
                : Material.PAPER;

        ItemStack info = new ItemStack(mat);
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            if (cfg != null) {
                meta.displayName(cfg.getItemName("items.info", placeholders));
                meta.lore(cfg.getItemLore("items.info", placeholders));
            } else {
                meta.displayName(TextUtil.parse("<aqua><bold>Pending Rewards</bold>"));
                List<Component> lore = new ArrayList<>();
                lore.add(Component.empty());
                lore.add(TextUtil.parse("<gray>Stored: <white>" + pending + " <dark_gray>/ <white>" + maxStr));
                meta.lore(lore);
            }
            info.setItemMeta(meta);
        }
        setButton(4, Button.visual(info));
    }

    /** Slot 49 — Claim All button. */
    private void addClaimAllButton(MenuConfig cfg) {
        Material mat = cfg != null
                ? cfg.getItemMaterial("items.claim_all", Material.CHEST)
                : Material.CHEST;

        ItemStack claimAll = new ItemStack(mat);
        ItemMeta meta = claimAll.getItemMeta();
        if (meta != null) {
            if (cfg != null) {
                meta.displayName(cfg.getItemName("items.claim_all", null));
                meta.lore(cfg.getItemLore("items.claim_all", null));
            } else {
                meta.displayName(TextUtil.parse("<green><bold>⬇ Claim All</bold>"));
                List<Component> lore = new ArrayList<>();
                lore.add(Component.empty());
                lore.add(TextUtil.parse("<gray>Claim all pending rewards at once."));
                meta.lore(lore);
            }
            claimAll.setItemMeta(meta);
        }
        setButton(49, new Button(claimAll, event -> {
            List<ClaimResult> results = claimService.claimAll(player);
            long succeeded = results.stream().filter(ClaimResult::isSuccess).count();
            long failed = results.size() - succeeded;

            if (results.isEmpty()) {
                messageManager.sendMessage(player, Messages.CLAIM_NOTHING_TO_CLAIM);
            } else if (failed == 0) {
                messageManager.sendMessage(player, Messages.CLAIM_ALL_CLAIMED,
                        "<claimed>", String.valueOf(succeeded));
            } else {
                messageManager.sendMessage(player, Messages.CLAIM_INVENTORY_FULL);
            }
            decorate();
        }));
    }

    /** Slot 53 — Close button. */
    private void addCloseButton(MenuConfig cfg) {
        Material mat = cfg != null
                ? cfg.getItemMaterial("items.close", Material.BARRIER)
                : Material.BARRIER;

        ItemStack close = new ItemStack(mat);
        ItemMeta meta = close.getItemMeta();
        if (meta != null) {
            if (cfg != null) {
                meta.displayName(cfg.getItemName("items.close", null));
                meta.lore(cfg.getItemLore("items.close", null));
            } else {
                meta.displayName(TextUtil.parse("<red>Close"));
            }
            close.setItemMeta(meta);
        }
        setButton(53, new Button(close, event -> player.closeInventory()));
    }

    /** Empty-state: fill item slots with a green placeholder. */
    private void addEmptyPlaceholder(MenuConfig cfg) {
        Material mat = cfg != null
                ? cfg.getItemMaterial("items.empty", Material.LIME_STAINED_GLASS_PANE)
                : Material.LIME_STAINED_GLASS_PANE;

        ItemStack empty = new ItemStack(mat);
        ItemMeta meta = empty.getItemMeta();
        if (meta != null) {
            if (cfg != null) {
                meta.displayName(cfg.getItemName("items.empty", null));
                meta.lore(cfg.getItemLore("items.empty", null));
            } else {
                meta.displayName(TextUtil.parse("<green><bold>✓ No pending rewards!</bold>"));
            }
            empty.setItemMeta(meta);
        }
        Button emptyBtn = Button.visual(empty);
        for (int slot : ITEM_SLOTS) {
            setButton(slot, emptyBtn);
        }
    }

    /** Builds the placeholder map for a specific {@link ClaimedReward}. */
    private Map<String, String> buildPlaceholders(ClaimedReward claim) {
        Map<String, String> p = new HashMap<>();
        p.put("<crate_id>", claim.getCrateId());
        p.put("<reward_id>", claim.getRewardId());
        p.put("<reason>", formatReason(claim));
        p.put("<stored_at>", DATE_FMT.format(new Date(claim.getStoredAt())));
        return p;
    }

    /** Returns a human-readable label for the {@code ClaimReason}. */
    private String formatReason(ClaimedReward claim) {
        return switch (claim.getReason()) {
            case INVENTORY_FULL -> "<red>Inventory Full";
            case PLAYER_OFFLINE -> "<gray>Offline";
            case FORCED         -> "<yellow>Forced";
            case DELIVERY_ERROR -> "<dark_red>Error";
        };
    }

    /** Convenience accessor for this menu's {@link MenuConfig}. */
    private MenuConfig cfg() {
        return menuManager.getMenuConfigManager().getMenu("claim_menu");
    }
}
