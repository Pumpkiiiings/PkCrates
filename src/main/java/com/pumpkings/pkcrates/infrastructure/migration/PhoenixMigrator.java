package com.pumpkings.pkcrates.infrastructure.migration;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Converts PhoenixCrates and PhoenixCratesLite definitions into PkCrates crate and key files.
 *
 * <h3>Why YAML to YAML</h3>
 * <p>The conversion never builds an {@link org.bukkit.inventory.ItemStack}. PhoenixCrates
 * stores custom-item references as {@code custom:<id>} strings that resolve through a
 * third-party item plugin; {@code Material.matchMaterial} returns {@code null} for them,
 * so an ItemStack-based pipeline would quietly produce rewards with no items. Staying at
 * the text level lets the original identifier be preserved and reported.</p>
 *
 * <h3>Field mapping</h3>
 * <pre>
 * display-name            -> name
 * hologram.lines          -> hologram.content
 * linked-keys-ids         -> accepted_keys   (+ a key stub per id)
 * rewards.N.identifier    -> reward id
 * rewards.N.percentage    -> weight
 * rewards.N.win-limits    -> limit           (dropped when &lt;= 0)
 * rewards.N.win-commands  -> commands
 * rewards.N.display-item  -> display_item
 * rewards.N.win-items.N   -> items.N
 * </pre>
 *
 * <p>Everything PkCrates has no concept of — opening animations, money cost, cooldowns,
 * guaranteed-win counters, alternative rewards, per-reward permission restrictions — is
 * recorded in the {@link MigrationReport} rather than silently discarded.</p>
 */
public class PhoenixMigrator {

    /** Prefix PhoenixCrates uses for third-party item references. */
    private static final String CUSTOM_PREFIX = "custom:";

    /** Stand-in material for items that could not be resolved. Visible and obviously wrong. */
    private static final String PLACEHOLDER_MATERIAL = "BARRIER";

    /** Key under which the unresolved original identifier is preserved. */
    private static final String ORIGINAL_MATERIAL_KEY = "migrated-material";

    private final Plugin plugin;

    /**
     * {@code custom:} strings already announced as auto-resolved, so the note is printed
     * once per material rather than once per item block. Scoped to a single run.
     */
    private final Set<String> resolvedPrefixed = new HashSet<>();

    /**
     * Key ids imported from real PhoenixCrates key files during this run.
     *
     * <p>A crate's {@code linked-keys-ids} would otherwise generate a placeholder key for
     * an id that was just migrated properly, overwriting the real item with a tripwire hook.</p>
     */
    private final Set<String> migratedKeyIds = new HashSet<>();

    public PhoenixMigrator(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Migrates every {@code .yml} found under the given source folders.
     *
     * <p>Both PhoenixCrates and PhoenixCratesLite are supported, and either may store its
     * definitions in {@code crates/} and {@code keys/} subfolders or flat in one directory.
     * Rather than guessing from the layout, each file is classified by its own content —
     * see {@link #classify}.</p>
     *
     * <p>Keys are migrated before crates so that a crate's {@code linked-keys-ids} resolve
     * against real key files instead of generating placeholders for keys that were about
     * to be imported properly.</p>
     *
     * @param sourceDirs Folders to scan, in priority order.
     * @param dryRun     When {@code true}, nothing is written; the report is produced anyway.
     * @param overwrite  When {@code false}, entries that already exist in PkCrates are skipped.
     * @return A report of everything translated, dropped, and left for manual work.
     */
    public MigrationReport migrate(List<File> sourceDirs, boolean dryRun, boolean overwrite, com.pumpkings.pkcrates.infrastructure.database.DatabaseManager databaseManager) {
        MigrationReport report = new MigrationReport();

        Map<String, org.bukkit.inventory.ItemStack> internalStore = loadInternalStore(sourceDirs, report);
        
        List<File> files = collectYmlFiles(sourceDirs);
        if (files.isEmpty()) {
            report.dropped("*", "No .yml files found in " + sourceDirs);
            return report;
        }

        File cratesFolder = new File(plugin.getDataFolder(), "crates");
        File keysFolder = new File(plugin.getDataFolder(), "keys");
        if (!dryRun) {
            cratesFolder.mkdirs();
            keysFolder.mkdirs();
        }

        List<File> crateFiles = new ArrayList<>();
        List<File> keyFiles = new ArrayList<>();

        for (File file : files) {
            switch (classify(file)) {
                case CRATE -> crateFiles.add(file);
                case KEY -> keyFiles.add(file);
                case UNKNOWN -> report.dropped(file.getName(),
                        "Not recognised as a PhoenixCrates crate or key; skipped.");
            }
        }

        for (File keyFile : keyFiles) {
            run(keyFile, report, () -> migrateKeyFile(keyFile, keysFolder, dryRun, overwrite, report, internalStore));
        }
        for (File crateFile : crateFiles) {
            run(crateFile, report, () -> migrateCrateFile(crateFile, cratesFolder, keysFolder, dryRun, overwrite, report, internalStore));
        }
        
        migrateInternalStorageKeys(sourceDirs, dryRun, report, databaseManager);

        return report;
    }

    private Map<String, org.bukkit.inventory.ItemStack> loadInternalStore(List<File> sourceDirs, MigrationReport report) {
        Map<String, org.bukkit.inventory.ItemStack> store = new HashMap<>();
        File file = findFile(sourceDirs, "internal_store.yml");
        if (file == null || !file.exists()) {
            file = findFile(sourceDirs, "internal_storage.yml");
        }
        
        if (file == null || !file.exists()) {
            report.error("PhoenixMigrator", "internal_store.yml (or internal_storage.yml) not found. Native items cannot be migrated.");
            return store;
        }

        report.info("PhoenixMigrator", file.getName() + ": found for items");
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection itemsSec = config.getConfigurationSection("items");
            if (itemsSec != null) {
                for (String key : itemsSec.getKeys(false)) {
                    report.itemFound();
                    try {
                        org.bukkit.inventory.ItemStack item = itemsSec.getItemStack(key);
                        if (item != null) {
                            store.put(key, item);
                            report.itemDeserialized();
                        } else {
                            report.itemFailed();
                            report.error("PhoenixMigrator", "Failed to deserialize Phoenix item '" + key + "' (returned null)");
                        }
                    } catch (Exception e) {
                        report.itemFailed();
                        report.error("PhoenixMigrator", "Exception deserializing Phoenix item '" + key + "': " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            report.error("PhoenixMigrator", "Failed to load internal_store.yml: " + e.getMessage());
        }
        return store;
    }

    private void migrateInternalStorageKeys(List<File> sourceDirs, boolean dryRun, MigrationReport report, com.pumpkings.pkcrates.infrastructure.database.DatabaseManager databaseManager) {
        File file = findFile(sourceDirs, "internal_storage.yml");
        if (file == null || !file.exists()) {
            report.error("PhoenixMigrator", "internal_storage.yml not found. Player keys cannot be migrated.");
            return;
        }

        report.info("PhoenixMigrator", "internal_storage.yml: found");
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            
            // We search for UUIDs at the top level or under "players".
            ConfigurationSection root = config.isConfigurationSection("players") ? config.getConfigurationSection("players") : config;
            
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (String potentialUuid : root.getKeys(false)) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(potentialUuid);
                } catch (IllegalArgumentException e) {
                    continue; // Not a UUID, skip
                }
                
                ConfigurationSection playerSec = root.getConfigurationSection(potentialUuid);
                if (playerSec == null) continue;
                
                ConfigurationSection keysSec = playerSec.isConfigurationSection("keys") ? playerSec.getConfigurationSection("keys") : playerSec;
                if (keysSec == null) continue;
                
                report.playerKeyFound(); // Count players found
                
                for (String keyId : keysSec.getKeys(false)) {
                    if (keysSec.isInt(keyId)) {
                        int amount = keysSec.getInt(keyId);
                        if (amount > 0) {
                            if (!dryRun && databaseManager != null) {
                                // We use setVirtualKeys to ensure idempotency. If migrated twice, it just resets to the same absolute amount, avoiding duplication.
                                futures.add(databaseManager.setVirtualKeys(uuid, keyId, amount).thenRun(() -> {
                                    report.playerKeyMigrated();
                                }).exceptionally(ex -> {
                                    report.playerKeyFailed();
                                    report.error("PhoenixMigrator", "Failed to migrate key '" + keyId + "' for " + uuid);
                                    return null;
                                }));
                            } else {
                                report.playerKeyMigrated(); // Just count it in dry-run
                            }
                        }
                    }
                }
            }
            if (!dryRun) {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            }
        } catch (Exception e) {
            report.error("PhoenixMigrator", "Failed to load internal_storage.yml: " + e.getMessage());
        }
    }

    private File findFile(List<File> sourceDirs, String name) {
        for (File dir : sourceDirs) {
            if (dir == null || !dir.isDirectory()) continue;
            File f = new File(dir, name);
            if (f.exists()) return f;
        }
        return null;
    }

    /**
     * Runs one migration step, turning any failure into a report entry so a single bad
     * file cannot abort the whole run.
     */
    private void run(File file, MigrationReport report, ThrowingRunnable step) {
        try {
            step.run();
        } catch (Exception e) {
            report.dropped(file.getName(),
                    "Failed to migrate: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            plugin.getLogger().warning("Migration error on " + file.getName() + ": " + e);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    /** What a source file turned out to be. */
    private enum SourceKind { CRATE, KEY, UNKNOWN }

    /**
     * Determines what a file is from its contents rather than its folder or name.
     *
     * <p>PhoenixCrates names key files after a timestamp ({@code key_1776813321157.yml}),
     * so the file name says nothing, and operators dropping files into the fallback input
     * folder will not have preserved the original directory layout.</p>
     */
    private SourceKind classify(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (config.isConfigurationSection("rewards")) return SourceKind.CRATE;
        if (config.isConfigurationSection("item") && config.contains("identifier")) return SourceKind.KEY;
        return SourceKind.UNKNOWN;
    }

    /**
     * Gathers yml files from each source folder, also descending into {@code crates/} and
     * {@code keys/} when the plugin used that layout.
     */
    private List<File> collectYmlFiles(List<File> sourceDirs) {
        List<File> files = new ArrayList<>();
        for (File dir : sourceDirs) {
            if (dir == null || !dir.isDirectory()) continue;
            addYmlFiles(dir, files);
            addYmlFiles(new File(dir, "crates"), files);
            addYmlFiles(new File(dir, "keys"), files);
        }
        return files;
    }

    private void addYmlFiles(File dir, List<File> target) {
        if (!dir.isDirectory()) return;
        File[] found = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".yml"));
        if (found != null) {
            target.addAll(List.of(found));
        }
    }

    // -------------------------------------------------------------------------
    // Key conversion
    // -------------------------------------------------------------------------

    /**
     * Converts one PhoenixCrates key definition.
     *
     * <p>The key id comes from {@code identifier}, never the file name: PhoenixCrates names
     * key files after a creation timestamp, while a crate's {@code linked-keys-ids} refers
     * to the identifier. Using the file name would break every crate-to-key link.</p>
     */
    private void migrateKeyFile(File sourceFile, File keysFolder, boolean dryRun,
                                boolean overwrite, MigrationReport report, Map<String, org.bukkit.inventory.ItemStack> internalStore) throws IOException {

        YamlConfiguration source = YamlConfiguration.loadConfiguration(sourceFile);

        String rawId = source.getString("identifier", sourceFile.getName().replaceAll("(?i)\\.yml$", ""));
        String keyId = sanitizeId(rawId);

        File target = new File(keysFolder, keyId + ".yml");
        if (target.exists() && !overwrite) {
            report.dropped(keyId, "A key with this id already exists; skipped.");
            report.keyFileSkipped();
            return;
        }

        YamlConfiguration out = new YamlConfiguration();
        out.set("virtual", source.getBoolean("virtual", false));

        ConfigurationSection item = source.getConfigurationSection("item");
        if (item != null) {
            migrateItem(item, out, "item", keyId, "key item", report, internalStore);
        } else {
            ConfigurationSection fallback = out.createSection("item");
            fallback.set("id", "TRIPWIRE_HOOK");
            fallback.set("amount", 1);
            fallback.set("name", "&e" + keyId);
            report.manual(keyId, "Key had no item block; created a TRIPWIRE_HOOK placeholder.");
        }

        if (!dryRun) {
            out.save(target);
        }
        migratedKeyIds.add(keyId);
        report.keyFileMigrated();

        if (!source.getBoolean("enabled", true)) {
            report.manual(keyId, "Key was disabled in PhoenixCrates. PkCrates has no enabled flag; it is now active.");
        }
        if (source.getBoolean("glow", false)) {
            report.dropped(keyId, "glow has no equivalent; add an enchantment in /crate editor for a similar effect.");
        }
    }

    // -------------------------------------------------------------------------
    // Crate conversion
    // -------------------------------------------------------------------------

    private void migrateCrateFile(File sourceFile, File cratesFolder, File keysFolder,
                                  boolean dryRun, boolean overwrite, MigrationReport report, Map<String, org.bukkit.inventory.ItemStack> internalStore) throws IOException {

        report.crateFound();
        YamlConfiguration source = YamlConfiguration.loadConfiguration(sourceFile);

        String rawId = source.getString("identifier", sourceFile.getName().replaceAll("(?i)\\.yml$", ""));
        String crateId = sanitizeId(rawId);
        if (!crateId.equals(rawId)) {
            report.info(crateId, "Identifier '" + rawId + "' contained characters invalid in a file name; using '" + crateId + "'.");
        }

        File target = new File(cratesFolder, crateId + ".yml");
        if (target.exists() && !overwrite) {
            report.dropped(crateId, "A crate with this id already exists; skipped. Re-run with 'overwrite' to replace it.");
            report.crateSkipped();
            return;
        }

        YamlConfiguration out = new YamlConfiguration();

        out.set("name", source.getString("display-name", crateId));
        out.set("animation", "ROULETTE");

        migrateHologram(source, out, crateId, report);
        List<String> keyIds = migrateKeys(source, out, keysFolder, crateId, dryRun, report);
        migrateRewards(source, out, crateId, report, internalStore);
        reportUnmappedCrateFields(source, crateId, keyIds, report);

        if (!dryRun) {
            out.save(target);
        }
        report.crateMigrated();
    }

    private void migrateHologram(YamlConfiguration source, YamlConfiguration out,
                                 String crateId, MigrationReport report) {
        ConfigurationSection holo = source.getConfigurationSection("hologram");
        if (holo == null) return;

        List<String> lines = holo.getStringList("lines");
        if (lines.isEmpty()) return;

        // Legacy '&' codes are left as-is: TextUtil converts them to MiniMessage at render
        // time, so rewriting them here would only risk mangling the operator's formatting.
        ConfigurationSection outHolo = out.createSection("hologram");
        outHolo.set("content", lines);
        outHolo.set("billboard", "CENTER");
        outHolo.set("background-color", "none");
        outHolo.set("shadowtext", true);
        outHolo.set("scale", 1.0);

        double offset = holo.getDouble("offset", 0.0);
        if (offset != 0.0) {
            report.dropped(crateId, "hologram.offset (" + offset + ") has no equivalent; hologram sits at the default height.");
        }
    }

    /**
     * Copies the linked key ids onto the crate and writes a stub key file for each one
     * that PkCrates does not already know about.
     */
    private List<String> migrateKeys(YamlConfiguration source, YamlConfiguration out, File keysFolder,
                                     String crateId, boolean dryRun, MigrationReport report) throws IOException {

        List<String> rawKeys = source.getStringList("linked-keys-ids");
        List<String> keyIds = new ArrayList<>();

        for (String rawKey : rawKeys) {
            String keyId = sanitizeId(rawKey);
            keyIds.add(keyId);

            // A real key file was imported for this id — leave it alone.
            if (migratedKeyIds.contains(keyId)) continue;

            File keyFile = new File(keysFolder, keyId + ".yml");
            if (keyFile.exists()) continue;

            YamlConfiguration keyConfig = new YamlConfiguration();
            keyConfig.set("virtual", false);
            ConfigurationSection item = keyConfig.createSection("item");
            item.set("id", "TRIPWIRE_HOOK");
            item.set("amount", 1);
            item.set("name", "&e" + keyId);

            if (!dryRun) {
                keyConfig.save(keyFile);
            }
            report.keyFileCreated();
            report.manual(crateId, "Linked key '" + keyId + "' had no key file in the source folders, so a "
                    + "TRIPWIRE_HOOK placeholder was created. Add PhoenixCrates' keys/ folder to the migration "
                    + "input and re-run, or set the item in /crate editor.");
        }

        out.set("accepted_keys", keyIds);

        if (!source.getBoolean("key-required", true)) {
            report.dropped(crateId, "key-required was false. PkCrates always requires a key; this crate now needs " + keyIds + ".");
        }
        if (keyIds.isEmpty()) {
            report.manual(crateId, "No linked keys found. The crate cannot be opened until accepted_keys is filled in.");
        }
        return keyIds;
    }

    private void migrateRewards(YamlConfiguration source, YamlConfiguration out,
                                String crateId, MigrationReport report, Map<String, org.bukkit.inventory.ItemStack> internalStore) {

        ConfigurationSection rewards = source.getConfigurationSection("rewards");
        if (rewards == null) {
            report.manual(crateId, "Crate has no rewards section.");
            return;
        }

        ConfigurationSection outRewards = out.createSection("rewards");

        for (String index : rewards.getKeys(false)) {
            ConfigurationSection reward = rewards.getConfigurationSection(index);
            if (reward == null) continue;

            String rewardId = sanitizeId(reward.getString("identifier", "reward_" + index));
            ConfigurationSection outReward = outRewards.createSection(rewardId);

            // Phoenix percentages become relative weights. See reportWeightSemantics().
            outReward.set("weight", reward.getDouble("percentage", 10.0));

            int winLimit = reward.getInt("win-limits", -1);
            if (winLimit > 0) {
                outReward.set("limit", winLimit);
            }

            if (reward.getBoolean("broadcast", false)) {
                outReward.set("broadcast", true);
            }

            List<String> commands = reward.getStringList("win-commands");
            if (!commands.isEmpty()) {
                outReward.set("commands", commands);
            }

            ConfigurationSection displayItem = reward.getConfigurationSection("display-item");
            if (displayItem != null) {
                migrateItem(displayItem, outReward, "display_item", crateId, rewardId, report, internalStore);
            }

            ConfigurationSection winItems = reward.getConfigurationSection("win-items");
            if (winItems != null && !winItems.getKeys(false).isEmpty()) {
                ConfigurationSection outItems = outReward.createSection("items");
                int slot = 1;
                for (String itemKey : winItems.getKeys(false)) {
                    ConfigurationSection item = winItems.getConfigurationSection(itemKey);
                    if (item == null) continue;
                    migrateItem(item, outItems, String.valueOf(slot++), crateId, rewardId, report, internalStore);
                }
            } else if (commands.isEmpty()) {
                report.manual(crateId, "Reward '" + rewardId + "' has neither items nor commands; it will give nothing.");
            }

            reportUnmappedRewardFields(reward, crateId, rewardId, report);
            report.rewardMigrated();
        }

        reportWeightSemantics(rewards, crateId, report);
    }

    private void migrateItem(ConfigurationSection source, ConfigurationSection parent, String key,
                             String crateId, String rewardId, MigrationReport report, Map<String, org.bukkit.inventory.ItemStack> internalStore) {

        String identifier = source.getString("identifier");
        String rawMaterial = source.getString("material", "");
        
        // Sometimes the identifier is in the 'identifier' field, other times it's in 'material' as 'custom:<id>'
        String lookupId = identifier;
        if ((lookupId == null || lookupId.isEmpty()) && rawMaterial.startsWith("custom:")) {
            lookupId = rawMaterial.substring(7);
        }

        if (lookupId != null && !lookupId.isEmpty()) {
            org.bukkit.inventory.ItemStack storedItem = internalStore.get(lookupId);
            if (storedItem != null) {
                com.pumpkings.pkcrates.infrastructure.item.ConfigItemSerializer.serialize(storedItem, parent, key);
                return;
            } else if (identifier != null && !identifier.isEmpty()) {
                report.error(crateId, "Phoenix reward '" + rewardId + "' references missing item '" + lookupId + "'");
            }
        }

        ConfigurationSection out = parent.createSection(key);
        String resolved = resolveMaterial(rawMaterial, crateId, rewardId, report);

        if (resolved != null) {
            out.set("id", resolved);
        } else {
            out.set("id", PLACEHOLDER_MATERIAL);
            out.set(ORIGINAL_MATERIAL_KEY, rawMaterial);

            // The same custom item appears as both display item and win item, and often
            // across several rewards. Explain it once; the counter still sees every block.
            if (report.itemNeedsFix(rawMaterial)) {
                report.manual(crateId, "'" + rawMaterial + "' is a third-party item PkCrates cannot resolve. "
                        + "Replaced with " + PLACEHOLDER_MATERIAL + "; original kept under '" + ORIGINAL_MATERIAL_KEY + "'.");
            }
        }

        out.set("amount", source.getInt("amount", 1));

        String displayName = source.getString("display-name");
        if (displayName != null && !displayName.isEmpty()) {
            out.set("name", displayName);
        }

        List<String> lore = source.getStringList("lore");
        if (!lore.isEmpty()) {
            out.set("lore", lore);
        }
    }

    /**
     * Maps a PhoenixCrates material string onto a Bukkit {@link Material} name.
     *
     * <p>{@code custom:} references are retried without the prefix first: entries such as
     * {@code custom:golden_apple} are plain vanilla materials wearing a prefix, and those
     * resolve cleanly. Genuine third-party ids cannot.</p>
     *
     * <p>Reporting of unresolvable materials is left to the caller so it can be deduplicated.</p>
     *
     * @return The Bukkit material name, or {@code null} when it could not be resolved.
     */
    private String resolveMaterial(String raw, String crateId, String rewardId, MigrationReport report) {
        if (raw == null || raw.isBlank()) {
            report.manual(crateId, "Reward '" + rewardId + "' has an item with no material; using " + PLACEHOLDER_MATERIAL + ".");
            return null;
        }

        boolean hadPrefix = raw.regionMatches(true, 0, CUSTOM_PREFIX, 0, CUSTOM_PREFIX.length());
        String candidate = hadPrefix ? raw.substring(CUSTOM_PREFIX.length()) : raw;

        Material material = Material.matchMaterial(candidate);
        if (material != null) {
            if (hadPrefix && resolvedPrefixed.add(raw)) {
                report.info(crateId, "'" + raw + "' resolved to vanilla " + material.name() + ".");
            }
            return material.name();
        }

        return null;
    }

    // -------------------------------------------------------------------------
    // Reporting of fields with no target equivalent
    // -------------------------------------------------------------------------

    /**
     * PhoenixCrates rolls each reward against its own percentage. PkCrates picks exactly
     * one reward using relative weights. Copying the numbers keeps the ordering — the most
     * likely reward stays the most likely — but the absolute odds necessarily change.
     */
    private void reportWeightSemantics(ConfigurationSection rewards, String crateId, MigrationReport report) {
        double total = 0.0;
        for (String index : rewards.getKeys(false)) {
            ConfigurationSection reward = rewards.getConfigurationSection(index);
            if (reward != null) {
                total += reward.getDouble("percentage", 0.0);
            }
        }

        // A total at or under 100% is already a valid single-pick distribution, so the
        // weights carry over untouched and there is nothing to compare.
        if (total <= 100.5 || total <= 0.0) return;

        report.manual(crateId, String.format(
                "Reward percentages total %.1f%%. If PhoenixCrates rolled each reward independently, "
                        + "a player averaged %.1f rewards per opening; PkCrates always gives exactly one. "
                        + "Compare the odds table below against your PhoenixCrates server before going live.",
                total, total / 100.0));

        for (String index : rewards.getKeys(false)) {
            ConfigurationSection reward = rewards.getConfigurationSection(index);
            if (reward == null) continue;

            String rewardId = sanitizeId(reward.getString("identifier", "reward_" + index));
            double percentage = reward.getDouble("percentage", 0.0);
            report.odds(crateId, rewardId, percentage, percentage / total * 100.0);
        }
    }

    private void reportUnmappedCrateFields(YamlConfiguration source, String crateId,
                                           List<String> keyIds, MigrationReport report) {

        if (!source.getBoolean("enabled", true)) {
            report.manual(crateId, "Crate was disabled in PhoenixCrates. PkCrates has no enabled flag; it is now active.");
        }

        String block = source.getString("block-material");
        if (block != null && !block.isEmpty()) {
            report.manual(crateId, "block-material was " + block
                    + ". PkCrates binds crates to a placed block at runtime — place the block and run /crate setlocation " + crateId + ".");
        }

        double cost = source.getDouble("open-money-cost", 0.0);
        if (cost > 0.0) {
            report.dropped(crateId, "open-money-cost (" + cost + ") has no equivalent; opening is now free.");
        }

        int cooldown = source.getInt("open-cooldown", 0);
        if (cooldown > 0) {
            report.dropped(crateId, "open-cooldown (" + cooldown + ") has no equivalent; there is no per-crate cooldown.");
        }

        if (source.isConfigurationSection("animation")) {
            report.dropped(crateId, "Idle and opening particle effects were dropped. PkCrates uses its own animations; "
                    + "set one with /crate editor (ROULETTE, CSGO, BLACKHOLE, SPIRAL, METEOR, FOUNTAIN, PORTAL).");
        }

        if (source.getBoolean("permission.required", false)) {
            String node = source.getString("permission.permission", "");
            report.manual(crateId, "Crate required permission '" + node
                    + "'. PkCrates uses 'pkcrates.open." + crateId + "' instead — regrant it.");
        }

        String broadcast = source.getString("broadcast.message");
        if (broadcast != null && !broadcast.isEmpty()) {
            report.dropped(crateId, "Crate-level broadcast message was dropped. PkCrates broadcasts per reward "
                    + "or per rarity; configure it in rarities.yml.");
        }

        int maxWin = source.getInt("max-win-rewards", 0);
        if (maxWin > 0) {
            report.dropped(crateId, "max-win-rewards (" + maxWin + ") has no equivalent; one reward is given per opening.");
        }

        String rewardsMode = source.getString("rewards-mode");
        if (rewardsMode != null && !rewardsMode.equalsIgnoreCase("RANDOM")) {
            report.manual(crateId, "rewards-mode was " + rewardsMode + ". PkCrates only supports weighted random selection.");
        }

        if (source.isConfigurationSection("menus")) {
            report.dropped(crateId, "Custom menu bindings were dropped. PkCrates menus are configured in the menus/ folder.");
        }

        if (source.getBoolean("simultaneous-openings", false)) {
            report.info(crateId, "simultaneous-openings was true — enable mass opening under 'mass-opening' in "
                    + crateId + ".yml to get comparable behaviour.");
        }
    }

    private void reportUnmappedRewardFields(ConfigurationSection reward, String crateId,
                                            String rewardId, MigrationReport report) {

        if (reward.getInt("guaranteed-win", -1) > 0) {
            report.dropped(crateId, "Reward '" + rewardId + "': guaranteed-win counter has no equivalent and was dropped.");
        }

        if (!reward.getStringList("restricted-permissions").isEmpty()) {
            report.dropped(crateId, "Reward '" + rewardId + "': restricted-permissions has no equivalent; "
                    + "every player can now win it.");
        }

        if (reward.getBoolean("alternative-reward.enabled", false)) {
            report.dropped(crateId, "Reward '" + rewardId + "': alternative-reward has no equivalent and was dropped.");
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Strips characters that are unsafe in a file name or a config path.
     */
    private String sanitizeId(String raw) {
        String cleaned = raw.replaceAll("[^A-Za-z0-9_-]", "_");
        return cleaned.isEmpty() ? "migrated" : cleaned;
    }
}
