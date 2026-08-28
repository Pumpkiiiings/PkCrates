package com.pumpkings.pkcrates.presentation.command.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.pumpkings.pkcrates.core.model.Crate;
import com.pumpkings.pkcrates.infrastructure.config.CrateRegistry;
import com.pumpkings.pkcrates.infrastructure.config.MessageManager;
import com.pumpkings.pkcrates.infrastructure.config.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import java.util.List;

public class ListCommand {

    public static LiteralCommandNode<CommandSourceStack> build(CrateRegistry registry, MessageManager messageManager) {
        return Commands.literal("list")
                .requires(source -> source.getSender().hasPermission("pkcrates.admin.list"))
                .executes(context -> {
                    List<Crate> crates = registry.getAllCrates();
                    if (crates.isEmpty()) {
                        messageManager.sendMessage(context.getSource().getSender(), Messages.CRATE_LIST_EMPTY);
                        return Command.SINGLE_SUCCESS;
                    }
                    
                    messageManager.sendMessage(context.getSource().getSender(), Messages.CRATE_LIST_HEADER);
                    for (Crate crate : crates) {
                        messageManager.sendMessage(context.getSource().getSender(), Messages.CRATE_LIST_FORMAT,
                            "<crate>", crate.getId(),
                            "<rewards>", String.valueOf(crate.getRewards().size())
                        );
                    }
                    
                    return Command.SINGLE_SUCCESS;
                })
                .build();
    }
}
