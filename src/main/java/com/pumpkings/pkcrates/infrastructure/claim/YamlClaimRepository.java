package com.pumpkings.pkcrates.infrastructure.claim;

import com.pumpkings.pkcrates.core.model.claim.ClaimReason;
import com.pumpkings.pkcrates.core.model.claim.ClaimedReward;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
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
 * <p>All file operations are synchronous. If high I/O is needed in the future,
 * swap this class for an async adapter without touching the service layer.</p>
 */
public class YamlClaimRepository implements ClaimRepository {

    private static final String CLAIMS_FOLDER = "claims";

    private final Plugin plugin;
    private final File claimsFolder;
    private final Logger logger;

    /**
     * @param plugin The owning plugin, used to locate the data folder.
     */
    public YamlClaimRepository(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.claimsFolder = new File(plugin.getDataFolder(), CLAIMS_FOLDER);
        if (!claimsFolder.exists()) {
            claimsFolder.mkdirs();
        }
    }

    // -------------------------------------------------------------------------
    // ClaimRepository
    // -------------------------------------------------------------------------

    @Override
    public void save(ClaimedReward claim) {
        File file = playerFile(claim.getPlayerUuid());
        YamlConfiguration config = loadFile(file);

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

        saveFile(config, file);
    }

    @Override
    public void delete(UUID playerUuid, UUID claimId) {
        File file = playerFile(playerUuid);
        if (!file.exists()) return;

        YamlConfiguration config = loadFile(file);
        config.set("claims." + claimId.toString(), null);
        saveFile(config, file);
    }

    @Override
    public List<ClaimedReward> findByPlayer(UUID playerUuid) {
        File file = playerFile(playerUuid);
        if (!file.exists()) return Collections.emptyList();

        YamlConfiguration config = loadFile(file);
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
        File file = playerFile(playerUuid);
        if (file.exists()) {
            file.delete();
        }
    }

    @Override
    public void deleteAllPlayers() {
        File[] files = claimsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            file.delete();
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private File playerFile(UUID playerUuid) {
        return new File(claimsFolder, playerUuid.toString() + ".yml");
    }

    private YamlConfiguration loadFile(File file) {
        if (!file.exists()) return new YamlConfiguration();
        return YamlConfiguration.loadConfiguration(file);
    }

    private void saveFile(YamlConfiguration config, File file) {
        try {
            config.save(file);
        } catch (IOException e) {
            logger.severe("Failed to save claim file '" + file.getName() + "': " + e.getMessage());
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
