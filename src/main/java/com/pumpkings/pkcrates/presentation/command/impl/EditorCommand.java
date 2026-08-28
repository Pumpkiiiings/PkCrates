package com.pumpkings.pkcrates.presentation.command.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.pumpkings.pkcrates.infrastructure.config.CrateRegistry;
import com.pumpkings.pkcrates.infrastructure.config.MessageManager;
import com.pumpkings.pkcrates.infrastructure.config.Messages;
import com.pumpkings.pkcrates.infrastructure.key.KeyRegistry;
import com.pumpkings.pkcrates.infrastructure.location.CrateLocationManager;
import com.pumpkings.pkcrates.presentation.menu.MenuManager;
import com.pumpkings.pkcrates.presentation.menu.editor.CratesDashboardMenu;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class EditorCommand {

    public static LiteralCommandNode<CommandSourceStack> build(Plugin plugin, CrateRegistry crateRegistry, KeyRegistry keyRegistry, CrateLocationManager locationMgr, MenuManager menuManager, com.pumpkings.pkcrates.infrastructure.display.HologramManager hologramManager, MessageManager messageManager) {
        return Commands.literal("editor")
                .requires(source -> source.getSender().hasPermission("pkcrates.admin.editor"))
                .executes(context -> {
                    if (!(context.getSource().getSender() instanceof Player player)) {
                        messageManager.sendMessage(context.getSource().getSender(), Messages.PLAYER_ONLY);
                        return Command.SINGLE_SUCCESS;
                    }
                    
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        new CratesDashboardMenu(plugin, menuManager, player, crateRegistry, keyRegistry, locationMgr, hologramManager).open();
                    });
                    
                    return Command.SINGLE_SUCCESS;
                })
                .build();
    }
}
