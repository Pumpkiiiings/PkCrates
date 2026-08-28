package com.pumpkings.pkcrates.infrastructure.migration;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Accumulates everything a migration run did, changed, or could not translate.
 *
 * <p>A migration between two plugins is never lossless — the source format carries
 * features the target has no concept of. Silently dropping those produces a crate that
 * looks migrated but behaves differently, so every unmapped field is recorded here and
 * shown to the operator instead.</p>
 */
public class MigrationReport {

    /**
     * How much attention an entry needs.
     */
    public enum Severity {
        /** Translated automatically; recorded for traceability. */
        INFO,
        /** Source field has no target equivalent and was dropped. */
        DROPPED,
        /** Migrated, but the result will not work until a human fixes it. */
        MANUAL,
        /** A critical failure occurred while migrating a specific entry. */
        ERROR
    }

    /**
     * @param severity How urgent the entry is.
     * @param scope    Crate id, or {@code "*"} for run-wide notes.
     * @param message  Human-readable description.
     */
    public record Entry(Severity severity, String scope, String message) {}

    /**
     * How one reward's odds look before and after the move.
     *
     * @param crateId           Owning crate.
     * @param rewardId          Reward identifier in the migrated file.
     * @param sourcePercentage  The {@code percentage} PhoenixCrates had.
     * @param resultingChance   Probability of this reward being picked in PkCrates, as a
     *                          percentage — the weight divided by the crate's total weight.
     */
    public record OddsRow(String crateId, String rewardId, double sourcePercentage, double resultingChance) {}

    private final List<Entry> entries = new ArrayList<>();

    /** Populated only for crates whose percentages do not already behave as weights. */
    private final List<OddsRow> odds = new ArrayList<>();

    private int cratesFound;
    private int cratesMigrated;
    private int cratesSkipped;
    private int cratesFailed;
    
    private int rewardsMigrated;
    
    private int keyFilesCreated;
    private int keyFilesMigrated;
    private int keyFilesSkipped;

    private int itemsFound;
    private int itemsSuccessfullyDeserialized;
    private int itemsFailed;

    private int playerKeysFound;
    private int playerKeysMigrated;
    private int playerKeysFailed;

    /**
     * Distinct source materials that could not be resolved.
     *
     * <p>Counted by identity rather than by occurrence: the same custom item usually
     * appears twice per reward (display item and win item), and an operator fixing it
     * does the work once. Insertion-ordered so the list reads in crate order.</p>
     */
    private final Set<String> unresolvedMaterials = new LinkedHashSet<>();

    /** Item blocks that ended up on the placeholder material. */
    private int placeholderBlocks;

    // -------------------------------------------------------------------------
    // Recording
    // -------------------------------------------------------------------------

    public void info(String scope, String message) {
        entries.add(new Entry(Severity.INFO, scope, message));
    }

    public void dropped(String scope, String message) {
        entries.add(new Entry(Severity.DROPPED, scope, message));
    }

    public void manual(String scope, String message) {
        entries.add(new Entry(Severity.MANUAL, scope, message));
    }

    public void error(String scope, String message) {
        entries.add(new Entry(Severity.ERROR, scope, message));
    }

    /**
     * Records the before/after odds of one reward so the operator can compare the migrated
     * crate against their live PhoenixCrates server.
     */
    public void odds(String crateId, String rewardId, double sourcePercentage, double resultingChance) {
        odds.add(new OddsRow(crateId, rewardId, sourcePercentage, resultingChance));
    }

    public List<OddsRow> getOdds() {
        return List.copyOf(odds);
    }

    public void crateFound() {
        cratesFound++;
    }

    public void crateMigrated() {
        cratesMigrated++;
    }

    public void crateSkipped() {
        cratesSkipped++;
    }
    
    public void crateFailed() {
        cratesFailed++;
    }

    public void rewardMigrated() {
        rewardsMigrated++;
    }

    public void keyFileCreated() {
        keyFilesCreated++;
    }

    public void keyFileMigrated() {
        keyFilesMigrated++;
    }

    public void keyFileSkipped() {
        keyFilesSkipped++;
    }

    public void itemFound() {
        itemsFound++;
    }

    public void itemDeserialized() {
        itemsSuccessfullyDeserialized++;
    }

    public void itemFailed() {
        itemsFailed++;
    }

    public void playerKeyFound() {
        playerKeysFound++;
    }

    public void playerKeyMigrated() {
        playerKeysMigrated++;
    }

    public void playerKeyFailed() {
        playerKeysFailed++;
    }

    /**
     * Records one item block that fell back to the placeholder material.
     *
     * @param sourceMaterial The original, unresolvable material string.
     * @return {@code true} when this material had not been seen before, so the caller
     *         can log its explanation exactly once.
     */
    public boolean itemNeedsFix(String sourceMaterial) {
        placeholderBlocks++;
        return unresolvedMaterials.add(sourceMaterial);
    }

    // -------------------------------------------------------------------------
    // Reading
    // -------------------------------------------------------------------------

    public List<Entry> getEntries() {
        return List.copyOf(entries);
    }

    /**
     * @return Entries at the given severity, in the order they were recorded.
     */
    public List<Entry> entriesOf(Severity severity) {
        return entries.stream().filter(entry -> entry.severity() == severity).toList();
    }

    public int getCratesFound() {
        return cratesFound;
    }

    public int getCratesMigrated() {
        return cratesMigrated;
    }

    public int getCratesSkipped() {
        return cratesSkipped;
    }
    
    public int getCratesFailed() {
        return cratesFailed;
    }

    public int getRewardsMigrated() {
        return rewardsMigrated;
    }

    public int getKeyFilesCreated() {
        return keyFilesCreated;
    }

    public int getKeyFilesMigrated() {
        return keyFilesMigrated;
    }

    public int getKeyFilesSkipped() {
        return keyFilesSkipped;
    }

    public int getItemsFound() {
        return itemsFound;
    }

    public int getItemsSuccessfullyDeserialized() {
        return itemsSuccessfullyDeserialized;
    }

    public int getItemsFailed() {
        return itemsFailed;
    }

    public int getPlayerKeysFound() {
        return playerKeysFound;
    }

    public int getPlayerKeysMigrated() {
        return playerKeysMigrated;
    }

    public int getPlayerKeysFailed() {
        return playerKeysFailed;
    }

    /**
     * @return Distinct source materials needing a manual fix, in encounter order.
     */
    public List<String> getUnresolvedMaterials() {
        return List.copyOf(unresolvedMaterials);
    }

    /**
     * @return How many item blocks were written with the placeholder material.
     */
    public int getPlaceholderBlocks() {
        return placeholderBlocks;
    }

    /**
     * @return {@code true} when nothing at all was produced.
     */
    public boolean isEmpty() {
        return cratesMigrated == 0 && cratesSkipped == 0 && keyFilesMigrated == 0 && keyFilesSkipped == 0;
    }
}
