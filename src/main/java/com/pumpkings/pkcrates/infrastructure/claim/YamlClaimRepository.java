package com.pumpkings.pkcrates.infrastructure.claim;

import com.pumpkings.pkcrates.core.model.claim.ClaimReason;
import com.pumpkings.pkcrates.core.model.claim.ClaimedReward;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * YAML-backed implementation of {@link ClaimRepository}.
 *
 * <p>Each player's claims are stored in a dedicated file:
 * {@code plugins/PkCrates/claims/<playerUuid>.yml}</p>
 *
 * <p>ItemStack data is serialised to Base64 using Paper's
 * {@link ItemStack#serializeAsBytes()} / {@link ItemStack#deserializeBytes(byte[])}
 * API, which is the most reliable cross-version method for item persistence.</p>
 *
 * <h3>I/O model</h3>
 * <p>A player's file is parsed at most once and then held in memory. Mutations update
 * the in-memory copy and mark the player dirty; a repeating task snapshots dirty
 * configurations on the main thread and writes them to disk asynchronously. Without
 * this, a mass opening into a full inventory would do one full file load + save per
 * reward, inside a single server tick.</p>
 *
 * <p>{@link #flush()} performs a blocking write of everything still dirty and is
 * called from {@code onDisable}.</p>
 */
public class YamlClaimRepository implements ClaimRepository {

    private static final String CLAIMS_FOLDER = "claims";

    /** Ticks between background flushes of dirty player files. */
    private static final long FLUSH_INTERVAL_TICKS = 100L; // 5 seconds

    private final Plugin plugin;
    private final File claimsFolder;
    private final Logger logger;

    /** Parsed configuration per player. Mutated on the main thread only. */
    private final Map<UUID, YamlConfiguration> cache = new ConcurrentHashMap<>();

    /** Players whose in-memory configuration has not yet reached disk. */
    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();

    /**
     * All claim-file writes and deletes go through this single worker.
     *
     * <p>Serialising them is what stops a queued background write from resurrecting a
     * file that {@code deleteAll} removed moments earlier.</p>
     */
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "PkCrates-ClaimIO");
        thread.setDaemon(true);
        return thread;
    });

    private int flushTaskId = -1;

    /**
     * @param plugin The owning plugin, used to locate the data folder and schedule flushes.
     */
    public YamlClaimRepository(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.claimsFolder = new File(plugin.getDataFolder(), CLAIMS_FOLDER);
        if (!claimsFolder.exists()) {
            claimsFolder.mkdirs();
        }
        startFlushTask();
    }

    // -------------------------------------------------------------------------
    // ClaimRepository
    // -------------------------------------------------------------------------

    @Override
    public void save(ClaimedReward claim) {
        YamlConfiguration config = configFor(claim.getPlayerUuid());

        String path = "claims." + claim.getId().toString();
        config.set(path + ".player-uuid", claim.getPlayerUuid().toString());
        config.set(path + ".crate-id", claim.getCrateId());
        config.set(path + ".reward-id", claim.getRewardId());
        config.set(path + ".reason", claim.getReason().name());
        config.set(path + ".stored-at", claim.getStoredAt());

        // Serialise preview item
        if (claim.getPreviewItem() != null) {
            config.set(path + ".preview-item", encodeItem(claim.getPreviewItem()));
        }

        // Serialise reward items
        List<String> encodedItems = new ArrayList<>();
        for (ItemStack item : claim.getItems()) {
            if (item != null) {
                encodedItems.add(encodeItem(item));
            }
        }
        config.set(path + ".items", encodedItems);

        // Commands are plain strings — no encoding needed
        config.set(path + ".commands", claim.getCommands());

        dirty.add(claim.getPlayerUuid());
    }

    @Override
    public void delete(UUID playerUuid, UUID claimId) {
        YamlConfiguration config = configFor(playerUuid);
        config.set("claims." + claimId.toString(), null);
        dirty.add(playerUuid);
    }

    @Override
    public List<ClaimedReward> findByPlayer(UUID playerUuid) {
        YamlConfiguration config = configFor(playerUuid);
        ConfigurationSection claimsSection = config.getConfigurationSection("claims");
        if (claimsSection == null) return Collections.emptyList();

        List<ClaimedReward> results = new ArrayList<>();

        for (String claimIdStr : claimsSection.getKeys(false)) {
            try {
                UUID claimId = UUID.fromString(claimIdStr);
                ConfigurationSection section = claimsSection.getConfigurationSection(claimIdStr);
                if (section == null) continue;

                UUID storedPlayer = UUID.fromString(section.getString("player-uuid", playerUuid.toString()));
                String crateId = section.getString("crate-id", "unknown");
                String rewardId = section.getString("reward-id", "unknown");
                long storedAt = section.getLong("stored-at", 0L);

                ClaimReason reason;
                try {
                    reason = ClaimReason.valueOf(section.getString("reason", "DELIVERY_ERROR"));
                } catch (IllegalArgumentException e) {
                    reason = ClaimReason.DELIVERY_ERROR;
                }

                // Decode preview item
                ItemStack previewItem = null;
                String previewEncoded = section.getString("preview-item");
                if (previewEncoded != null && !previewEncoded.isEmpty()) {
                    previewItem = decodeItem(previewEncoded);
                }

                // Decode reward items
                List<ItemStack> items = new ArrayList<>();
                List<String> encodedItems = section.getStringList("items");
                for (String encoded : encodedItems) {
                    ItemStack item = decodeItem(encoded);
                    if (item != null) items.add(item);
                }

                List<String> commands = section.getStringList("commands");

                results.add(ClaimedReward.restore(
                        claimId, storedPlayer, crateId, rewardId,
                        previewItem, items, commands, reason, storedAt));

            } catch (Exception e) {
                logger.warning("Skipping malformed claim entry '" + claimIdStr
                        + "' for player " + playerUuid + ": " + e.getMessage());
            }
        }

        // Sort newest first
        results.sort(Comparator.comparingLong(ClaimedReward::getStoredAt).reversed());
        return Collections.unmodifiableList(results);
    }

    @Override
    public void deleteAll(UUID playerUuid) {
        cache.remove(playerUuid);
        dirty.remove(playerUuid);
        File file = playerFile(playerUuid);
        ioExecutor.execute(() -> {
            if (file.exists()) file.delete();
        });
    }

    @Override
    public void deleteAllPlayers() {
        cache.clear();
        dirty.clear();
        ioExecutor.execute(() -> {
            File[] files = claimsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files == null) return;
            for (File file : files) {
                file.delete();
            }
        });
    }

    /**
     * Snapshots every dirty player and blocks until all queued I/O has completed.
     */
    @Override
    public void flush() {
        enqueueDirtyWrites();

        ioExecutor.shutdown();
        try {
            if (!ioExecutor.awaitTermination(15, TimeUnit.SECONDS)) {
                logger.severe("Claim files did not finish writing within 15s; some claims may be lost.");
                ioExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ioExecutor.shutdownNow();
        }
    }

    /**
     * Cancels the background flush task. Call before {@link #flush()} on shutdown.
     */
    public void shutdown() {
        if (flushTaskId != -1) {
            Bukkit.getScheduler().cancelTask(flushTaskId);
            flushTaskId = -1;
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Schedules the periodic write-back. Snapshotting happens on the main thread —
     * where all mutations occur — and only the file write is handed to an async task.
     */
    private void startFlushTask() {
        flushTaskId = Bukkit.getScheduler().runTaskTimer(
                plugin, this::enqueueDirtyWrites, FLUSH_INTERVAL_TICKS, FLUSH_INTERVAL_TICKS).getTaskId();
    }

    /**
     * Snapshots each dirty configuration and hands the write to {@link #ioExecutor}.
     *
     * <p>{@code saveToString()} runs on the calling (main) thread — where all mutations
     * happen — so the worker never serialises a configuration that is being edited.</p>
     */
    private void enqueueDirtyWrites() {
        if (dirty.isEmpty()) return;

        for (UUID playerUuid : Set.copyOf(dirty)) {
            dirty.remove(playerUuid);
            YamlConfiguration config = cache.get(playerUuid);
            if (config == null) continue;

            String snapshot = config.saveToString();
            File file = playerFile(playerUuid);
            ioExecutor.execute(() -> writeFile(file, snapshot));
        }
    }

    /**
     * Returns the cached configuration for a player, parsing the file on first access.
     */
    private YamlConfiguration configFor(UUID playerUuid) {
        return cache.computeIfAbsent(playerUuid, uuid -> {
            File file = playerFile(uuid);
            if (!file.exists()) return new YamlConfiguration();
            return YamlConfiguration.loadConfiguration(file);
        });
    }

    private File playerFile(UUID playerUuid) {
        return new File(claimsFolder, playerUuid.toString() + ".yml");
    }

    /**
     * Writes the snapshot via a temporary file and an atomic move, so a crash mid-write
     * cannot leave a truncated claim file behind.
     */
    private void writeFile(File file, String contents) {
        File temp = new File(file.getParentFile(), file.getName() + ".tmp");
        try {
            Files.writeString(temp.toPath(), contents, StandardCharsets.UTF_8);
            Files.move(temp.toPath(), file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            logger.severe("Failed to save claim file '" + file.getName() + "': " + e.getMessage());
            temp.delete();
        }
    }

    /**
     * Encodes an {@link ItemStack} to a Base64 string using Paper's native
     * binary serialisation, which preserves all NBT/components faithfully.
     */
    private String encodeItem(ItemStack item) {
        try {
            byte[] bytes = item.serializeAsBytes();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            logger.warning("Could not encode item: " + e.getMessage());
            return "";
        }
    }

    /**
     * Decodes a Base64 string back into an {@link ItemStack}.
     *
     * @return The decoded item, or {@code null} on failure.
     */
    private ItemStack decodeItem(String encoded) {
        if (encoded == null || encoded.isEmpty()) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(encoded);
            return ItemStack.deserializeBytes(bytes);
        } catch (Exception e) {
            logger.warning("Could not decode item from Base64: " + e.getMessage());
            return null;
        }
    }
}
