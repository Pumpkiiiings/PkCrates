package com.pumpkings.pkcrates.core.model.claim;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * An immutable snapshot of a reward that is waiting to be claimed by a player.
 *
 * <p>This class intentionally stores a <em>copy</em> of all mutable data
 * (items, commands) at the moment of creation so that later edits to the
 * original crate/reward configuration cannot corrupt pending claims.</p>
 *
 * <p>All instances must be created via {@link #create}.</p>
 */
public final class ClaimedReward {

    /** Unique identifier of this pending claim entry. */
    private final UUID id;

    /** UUID of the player who owns this claim. */
    private final UUID playerUuid;

    /** ID of the crate that generated this reward. */
    private final String crateId;

    /** ID of the reward within the crate (for audit/display). */
    private final String rewardId;

    /**
     * A defensive copy of the display item used to render a preview
     * in the {@code ClaimMenu}. May be null if the reward had no display item.
     */
    private final ItemStack previewItem;

    /** Defensive copies of all physical items to deliver. */
    private final List<ItemStack> items;

    /** Console commands to run (with {@code %player%} as placeholder). */
    private final List<String> commands;

    /** The reason this reward was stored instead of delivered. */
    private final ClaimReason reason;

    /** Unix epoch milliseconds when this claim was created. */
    private final long storedAt;

    private ClaimedReward(
            UUID id,
            UUID playerUuid,
            String crateId,
            String rewardId,
            ItemStack previewItem,
            List<ItemStack> items,
            List<String> commands,
            ClaimReason reason,
            long storedAt) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.crateId = crateId;
        this.rewardId = rewardId;
        this.previewItem = previewItem != null ? previewItem.clone() : null;
        this.items = copyItems(items);
        this.commands = commands == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(commands));
        this.reason = reason;
        this.storedAt = storedAt;
    }

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    /**
     * Creates a new {@code ClaimedReward} with a freshly generated UUID and
     * the current time as {@code storedAt}.
     *
     * @param playerUuid  UUID of the player who wins the reward.
     * @param crateId     ID of the originating crate.
     * @param rewardId    ID of the reward within the crate.
     * @param previewItem Display item shown in the claim menu (may be null).
     * @param items       Physical items to deliver (may be empty).
     * @param commands    Console commands to execute (may be empty).
     * @param reason      Why the reward was stored rather than delivered directly.
     * @return A fully initialised, immutable {@code ClaimedReward}.
     */
    public static ClaimedReward create(
            UUID playerUuid,
            String crateId,
            String rewardId,
            ItemStack previewItem,
            List<ItemStack> items,
            List<String> commands,
            ClaimReason reason) {
        return new ClaimedReward(
                UUID.randomUUID(),
                playerUuid,
                crateId,
                rewardId,
                previewItem,
                items,
                commands,
                reason,
                System.currentTimeMillis()
        );
    }

    /**
     * Reconstructs a {@code ClaimedReward} from persisted data
     * (e.g., when loading from YAML or a database row).
     *
     * @param id          The previously persisted claim UUID.
     * @param playerUuid  UUID of the owning player.
     * @param crateId     Originating crate ID.
     * @param rewardId    Originating reward ID.
     * @param previewItem Deserialized preview item (may be null).
     * @param items       Deserialized physical items.
     * @param commands    Commands list.
     * @param reason      Original reason for storage.
     * @param storedAt    Original epoch-ms timestamp.
     * @return A fully initialised, immutable {@code ClaimedReward}.
     */
    public static ClaimedReward restore(
            UUID id,
            UUID playerUuid,
            String crateId,
            String rewardId,
            ItemStack previewItem,
            List<ItemStack> items,
            List<String> commands,
            ClaimReason reason,
            long storedAt) {
        return new ClaimedReward(id, playerUuid, crateId, rewardId,
                previewItem, items, commands, reason, storedAt);
    }

    // -------------------------------------------------------------------------
    // Accessors (all return defensive copies where needed)
    // -------------------------------------------------------------------------

    /** @return The unique ID of this claim entry. */
    public UUID getId() { return id; }

    /** @return The UUID of the player who owns this claim. */
    public UUID getPlayerUuid() { return playerUuid; }

    /** @return The ID of the crate that generated this reward. */
    public String getCrateId() { return crateId; }

    /** @return The ID of the reward within the crate. */
    public String getRewardId() { return rewardId; }

    /**
     * @return A clone of the display item, or {@code null} if none was stored.
     */
    public ItemStack getPreviewItem() {
        return previewItem != null ? previewItem.clone() : null;
    }

    /** @return An unmodifiable view of the physical items to deliver. */
    public List<ItemStack> getItems() {
        return items;
    }

    /** @return An unmodifiable view of the console commands to execute. */
    public List<String> getCommands() {
        return commands;
    }

    /** @return The reason this reward was stored in the claim system. */
    public ClaimReason getReason() { return reason; }

    /** @return Unix epoch milliseconds when the claim was created. */
    public long getStoredAt() { return storedAt; }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static List<ItemStack> copyItems(List<ItemStack> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<ItemStack> copy = new ArrayList<>(source.size());
        for (ItemStack item : source) {
            copy.add(item != null ? item.clone() : null);
        }
        return Collections.unmodifiableList(copy);
    }

    @Override
    public String toString() {
        return "ClaimedReward{id=" + id + ", player=" + playerUuid
                + ", crate=" + crateId + ", reward=" + rewardId
                + ", reason=" + reason + ", storedAt=" + storedAt + "}";
    }
}
