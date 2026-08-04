package com.pumpkings.pkcrates.presentation.command.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.pumpkings.pkcrates.infrastructure.config.CrateRegistry;
import com.pumpkings.pkcrates.infrastructure.key.KeyRegistry;
import com.pumpkings.pkcrates.infrastructure.migration.MigrationReport;
import com.pumpkings.pkcrates.infrastructure.migration.PhoenixMigrator;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code /crate migrate phoenix [confirm|overwrite]} — imports PhoenixCrates crates.
 *
 * <p>The bare form is a dry run: it parses everything and prints the full report without
 * touching a file. Writing requires an explicit {@code confirm}, because migration creates
 * crate and key files that an operator would otherwise have to clean up by hand.</p>
 */
public class MigrateCommand {

    /**
     * Plugin folders scanned for source data, in order.
     *
     * <p>Both editions ship under their own name and a server may have used either, so both
     * are checked; the migrator descends into each one's {@code crates/} and {@code keys/}.</p>
     */
    private static final String[] PHOENIX_PLUGIN_FOLDERS = {
            "PhoenixCrates",
            "PhoenixCratesLite"
    };

    /** Fallback drop-box for operators who only have the yml files. */
    private static final String FALLBACK_INPUT_FOLDER = "migration/input";

    /** Report lines printed inline before switching to a "see console" summary. */
    private static final int MAX_INLINE_ENTRIES = 15;

    public static LiteralCommandNode<CommandSourceStack> build(Plugin plugin,
                                                              CrateRegistry crateRegistry,
                                                              KeyRegistry keyRegistry) {
        return Commands.literal("migrate")
                .requires(source -> source.getSender().hasPermission("pkcrates.admin.migrate"))
                .then(Commands.literal("phoenix")
                        .executes(context -> run(context.getSource().getSender(), plugin, crateRegistry, keyRegistry, true, false))
                        .then(Commands.literal("confirm")
                                .executes(context -> run(context.getSource().getSender(), plugin, crateRegistry, keyRegistry, false, false)))
                        .then(Commands.literal("overwrite")
                                .executes(context -> run(context.getSource().getSender(), plugin, crateRegistry, keyRegistry, false, true))))
                .build();
    }

    private static int run(CommandSender sender, Plugin plugin, CrateRegistry crateRegistry,
                           KeyRegistry keyRegistry, boolean dryRun, boolean overwrite) {

        List<File> sourceDirs = resolveSourceDirs(plugin);
        if (sourceDirs.isEmpty()) {
            File fallback = new File(plugin.getDataFolder(), FALLBACK_INPUT_FOLDER);
            fallback.mkdirs();
            sender.sendRichMessage("<red>No PhoenixCrates data found.</red>");
            sender.sendRichMessage("<gray>Looked in <white>plugins/PhoenixCrates</white>, <white>plugins/PhoenixCratesLite</white>"
                    + " and <white>" + fallback.getPath() + "</white> (including their crates/ and keys/ folders).</gray>");
            sender.sendRichMessage("<gray>Drop the crate and key .yml files into the last folder and run this again.</gray>");
            return Command.SINGLE_SUCCESS;
        }

        for (File dir : sourceDirs) {
            sender.sendRichMessage("<gray>Reading from <white>" + dir.getPath() + "</white>…</gray>");
        }

        // File parsing and writing must not block the main thread; the registries are
        // reloaded back on it once the run finishes.
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            MigrationReport report = new PhoenixMigrator(plugin).migrate(sourceDirs, dryRun, overwrite);

            Bukkit.getScheduler().runTask(plugin, () -> {
                printReport(sender, report, dryRun);

                if (!dryRun && report.getCratesMigrated() > 0) {
                    crateRegistry.loadAll();
                    keyRegistry.loadAll();
                    sender.sendRichMessage("<green>Registries reloaded.</green>");
                }
            });
        });

        return Command.SINGLE_SUCCESS;
    }

    /**
     * @return Every source folder that actually holds yml data. Both Phoenix editions are
     *         included when both are present, so a server that switched between them gets
     *         all of its crates in one pass.
     */
    private static List<File> resolveSourceDirs(Plugin plugin) {
        File pluginsDir = plugin.getDataFolder().getParentFile();
        List<File> found = new ArrayList<>();

        for (String folder : PHOENIX_PLUGIN_FOLDERS) {
            File candidate = new File(pluginsDir, folder);
            if (holdsYml(candidate)) {
                found.add(candidate);
            }
        }

        File fallback = new File(plugin.getDataFolder(), FALLBACK_INPUT_FOLDER);
        if (holdsYml(fallback)) {
            found.add(fallback);
        }

        return found;
    }

    /**
     * @return {@code true} when the folder, or its {@code crates/} or {@code keys/}
     *         subfolder, contains at least one yml file.
     */
    private static boolean holdsYml(File dir) {
        return hasYml(dir) || hasYml(new File(dir, "crates")) || hasYml(new File(dir, "keys"));
    }

    private static boolean hasYml(File dir) {
        if (!dir.isDirectory()) return false;
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".yml"));
        return files != null && files.length > 0;
    }

    private static void printReport(CommandSender sender, MigrationReport report, boolean dryRun) {
        sender.sendRichMessage("");
        sender.sendRichMessage(dryRun
                ? "<gradient:#FFCD47:#FFB900><bold>Migration preview — nothing was written</bold></gradient>"
                : "<gradient:#42f554:#2eb344><bold>Migration complete</bold></gradient>");

        if (report.isEmpty()) {
            sender.sendRichMessage("<red>No crates were processed.</red>");
            return;
        }

        sender.sendRichMessage("<gray>Crates: <white>" + report.getCratesMigrated() + "</white>"
                + (report.getCratesSkipped() > 0 ? " <gray>(skipped <white>" + report.getCratesSkipped() + "</white>)</gray>" : "")
                + " <gray>| Rewards: <white>" + report.getRewardsMigrated() + "</white>");
        sender.sendRichMessage("<gray>Keys: <white>" + report.getKeysMigrated() + "</white> migrated"
                + (report.getKeysSkipped() > 0 ? " <gray>(skipped <white>" + report.getKeysSkipped() + "</white>)</gray>" : "")
                + (report.getKeysCreated() > 0 ? " <gray>, <white>" + report.getKeysCreated() + "</white> placeholder(s)</gray>" : ""));

        var unresolved = report.getUnresolvedMaterials();
        if (!unresolved.isEmpty()) {
            sender.sendRichMessage("");
            sender.sendRichMessage("<red><bold>" + unresolved.size() + "</bold> item(s) could not be resolved</red>"
                    + " <gray>(" + report.getPlaceholderBlocks() + " slots became BARRIER)</gray>");
            for (String material : unresolved) {
                sender.sendRichMessage("<dark_gray>  ▪ <red>" + escape(material) + "</red>");
            }
            sender.sendRichMessage("<gray>Each is kept under <white>migrated-material</white> in the crate file.</gray>");
        }

        printOdds(sender, report);
        printSection(sender, report, MigrationReport.Severity.MANUAL, "<red>Needs your attention</red>");
        printSection(sender, report, MigrationReport.Severity.DROPPED, "<yellow>Dropped (no equivalent)</yellow>");
        printSection(sender, report, MigrationReport.Severity.INFO, "<gray>Notes</gray>");

        if (dryRun) {
            sender.sendRichMessage("");
            sender.sendRichMessage("<gray>Run <white>/crate migrate phoenix confirm</white> to write these files.</gray>");
            sender.sendRichMessage("<gray>Use <white>overwrite</white> instead of <white>confirm</white> to replace existing crates.</gray>");
        }
    }

    /**
     * Prints the before/after odds for crates whose percentages did not already behave as
     * weights, so the operator can check them against their live PhoenixCrates server.
     */
    private static void printOdds(CommandSender sender, MigrationReport report) {
        var odds = report.getOdds();
        if (odds.isEmpty()) return;

        sender.sendRichMessage("");
        sender.sendRichMessage("<yellow>Reward odds — Phoenix percentage → PkCrates chance</yellow>");

        String currentCrate = null;
        for (MigrationReport.OddsRow row : odds) {
            if (!row.crateId().equals(currentCrate)) {
                currentCrate = row.crateId();
                sender.sendRichMessage("<gray>  " + escape(currentCrate) + "</gray>");
            }
            sender.sendRichMessage(String.format(
                    "<dark_gray>    %s <gray>%.1f%% <dark_gray>→ <white>%.1f%%</white>",
                    escape(row.rewardId()), row.sourcePercentage(), row.resultingChance()));
        }

        // The console copy is written by printSection for the accompanying MANUAL entry.
        for (MigrationReport.OddsRow row : odds) {
            Bukkit.getLogger().info(String.format("[PkCrates][migrate][odds][%s] %s: %.1f%% -> %.1f%%",
                    row.crateId(), row.rewardId(), row.sourcePercentage(), row.resultingChance()));
        }
    }

    private static void printSection(CommandSender sender, MigrationReport report,
                                     MigrationReport.Severity severity, String title) {

        var entries = report.entriesOf(severity);
        if (entries.isEmpty()) return;

        sender.sendRichMessage("");
        sender.sendRichMessage(title + " <gray>(" + entries.size() + ")</gray>");

        int shown = 0;
        for (MigrationReport.Entry entry : entries) {
            if (shown++ >= MAX_INLINE_ENTRIES) {
                sender.sendRichMessage("<dark_gray>  … " + (entries.size() - MAX_INLINE_ENTRIES)
                        + " more; see the server console.</dark_gray>");
                break;
            }
            sender.sendRichMessage("<dark_gray>  ▪ <gray>[" + entry.scope() + "] " + escape(entry.message()) + "</gray>");
        }

        // The console always gets the complete list, however long it is.
        for (MigrationReport.Entry entry : entries) {
            Bukkit.getLogger().info("[PkCrates][migrate][" + severity + "][" + entry.scope() + "] " + entry.message());
        }
    }

    /**
     * Neutralises MiniMessage tags in migrated content so an operator's {@code <gradient>}
     * in a crate name cannot alter the report's own formatting.
     */
    private static String escape(String text) {
        return text.replace("<", "\\<");
    }
}
