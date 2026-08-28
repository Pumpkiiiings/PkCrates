package com.pumpkings.pkcrates.presentation.command.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.pumpkings.pkcrates.core.model.Crate;
import com.pumpkings.pkcrates.infrastructure.config.CrateRegistry;
import com.pumpkings.pkcrates.infrastructure.config.MessageManager;
import com.pumpkings.pkcrates.infrastructure.config.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import java.util.stream.Collectors;

public class DeleteCommand {

    public static LiteralCommandNode<CommandSourceStack> build(com.pumpkings.pkcrates.PkCratesPlugin plugin, CrateRegistry registry, MessageManager messageManager) {
        
        SuggestionProvider<CommandSourceStack> crateSuggestions = (context, builder) -> {
            for (Crate crate : registry.getAllCrates()) {
                if (crate.getId().toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                    builder.suggest(crate.getId());
                }
            }
            return builder.buildFuture();
        };

        return Commands.literal("delete")
                .requires(source -> source.getSender().hasPermission("pkcrates.admin.delete"))
                .then(Commands.argument("crate_id", StringArgumentType.word())
                        .suggests(crateSuggestions)
                        .executes(context -> {
                            String id = StringArgumentType.getString(context, "crate_id");
                            
                            if (registry.getCrate(id) == null) {
                                messageManager.sendMessage(context.getSource().getSender(), Messages.CRATE_NOT_FOUND, "<crate>", id);
                                return 0;
                            }
                            
                            registry.deleteCrate(id);
                            
                            String actor = context.getSource().getSender() instanceof org.bukkit.entity.Player p ? p.getName() : "CONSOLE";
                            plugin.getAuditService().warning(com.pumpkings.pkcrates.infrastructure.audit.api.AuditEvent.CRATE_DELETED, actor, id, null);
                            
                            messageManager.sendMessage(context.getSource().getSender(), Messages.CRATE_DELETED);
                            
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
    }
}
