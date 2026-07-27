package com.pumpkings.pkcrates.presentation.command.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import com.pumpkings.pkcrates.infrastructure.config.MessageManager;
import com.pumpkings.pkcrates.infrastructure.config.Messages;
import org.bukkit.plugin.Plugin;

public class ReloadCommand {

    public static LiteralCommandNode<CommandSourceStack> build(Plugin plugin, MessageManager messageManager) {
        return Commands.literal("reload")
                .requires(source -> source.getSender().hasPermission("pkcrates.admin.reload"))
                .executes(context -> {
                    String actor = context.getSource().getSender() instanceof org.bukkit.entity.Player p ? p.getName() : "CONSOLE";
                    if (plugin instanceof com.pumpkings.pkcrates.PkCratesPlugin pkPlugin) {
                        pkPlugin.reloadPlugin();
                        pkPlugin.getAuditService().info(com.pumpkings.pkcrates.infrastructure.audit.api.AuditEvent.CONFIG_RELOADED, actor, "Plugin", null);
                    }

                    messageManager.sendMessage(context.getSource().getSender(), Messages.PLUGIN_RELOADED);
                    
                    return Command.SINGLE_SUCCESS;
                })
                .build();
    }
}
