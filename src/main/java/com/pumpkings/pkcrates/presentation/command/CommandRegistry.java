package com.pumpkings.pkcrates.presentation.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.pumpkings.pkcrates.core.service.ClaimService;
import com.pumpkings.pkcrates.core.service.KeyService;
import com.pumpkings.pkcrates.infrastructure.claim.ClaimConfig;
import com.pumpkings.pkcrates.infrastructure.config.CrateRegistry;
import com.pumpkings.pkcrates.infrastructure.key.KeyRegistry;
import com.pumpkings.pkcrates.infrastructure.location.CrateLocationManager;
import com.pumpkings.pkcrates.infrastructure.config.MessageManager;
import com.pumpkings.pkcrates.infrastructure.config.Messages;
import com.pumpkings.pkcrates.presentation.command.impl.*;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import org.bukkit.plugin.Plugin;

public class CommandRegistry {

    public static void register(ReloadableRegistrarEvent<Commands> event, com.pumpkings.pkcrates.PkCratesPlugin plugin,
                                CrateRegistry crateRegistry, KeyRegistry keyRegistry,
                                CrateLocationManager locationMgr, KeyService keyService,
                                com.pumpkings.pkcrates.presentation.menu.MenuManager menuManager,
                                com.pumpkings.pkcrates.infrastructure.display.HologramManager hologramManager,
                                MessageManager messageManager,
                                ClaimService claimService,
                                ClaimConfig claimConfig,
                                com.pumpkings.pkcrates.infrastructure.database.DatabaseManager databaseManager) {
        
        Commands commands = event.registrar();

        LiteralCommandNode<CommandSourceStack> rootNode = Commands.literal("crate")
                .requires(source -> source.getSender().hasPermission("pkcrates.admin"))
                .executes(context -> {
                    context.getSource().getSender().sendRichMessage("<gradient:#FFCD47:#FFB900>PkCrates v" + plugin.getPluginMeta().getVersion() + "</gradient>");
                    context.getSource().getSender().sendRichMessage("<gray>Usage: /crate <create|delete|list|setlocation|givekey|editor></gray>");
                    return Command.SINGLE_SUCCESS;
                })
                .then(CreateCommand.build(plugin, crateRegistry, locationMgr, hologramManager, messageManager))
                .then(DeleteCommand.build(plugin, crateRegistry, messageManager))
                .then(ListCommand.build(crateRegistry, messageManager))
                .then(GiveKeyCommand.build(plugin, keyRegistry, keyService, messageManager))
                .then(SetLocationCommand.build(crateRegistry, locationMgr, hologramManager, messageManager))
                .then(EditCommand.build(plugin, crateRegistry, keyRegistry, locationMgr, menuManager, hologramManager, messageManager))
                .then(EditorCommand.build(plugin, crateRegistry, keyRegistry, locationMgr, menuManager, hologramManager, messageManager))
                .then(ReloadCommand.build(plugin, messageManager))
                .then(ClaimCommand.build(plugin, claimService, claimConfig, menuManager, messageManager))
                .then(InspectClaimCommand.build(plugin, claimService, claimConfig, menuManager, messageManager))
                .then(ClearClaimsCommand.build(plugin, claimService, messageManager))
                .then(ClearClaimsAllCommand.build(plugin, claimService, messageManager))
                .then(PeekCommand.build(plugin, crateRegistry, menuManager, messageManager))
                .then(DeliverCommand.build(crateRegistry, claimService, claimConfig, messageManager))
                .then(RollCommand.build(crateRegistry, claimService, claimConfig, messageManager))
                .then(RevokeKeyCommand.build(plugin, keyRegistry, databaseManager, messageManager))
                .then(SetKeysCommand.build(keyRegistry, databaseManager, messageManager))
                .then(KeysOfCommand.build(databaseManager, messageManager))
                .then(BroadcastKeyCommand.build(plugin, keyRegistry, keyService, messageManager))
                .then(MigrateCommand.build(plugin, crateRegistry, keyRegistry))
                .build();

        commands.register(rootNode, "Main command for PkCrates");
        // Alias
        commands.register(Commands.literal("pkcrates").redirect(rootNode).build(), "Alias for PkCrates");
    }
}
