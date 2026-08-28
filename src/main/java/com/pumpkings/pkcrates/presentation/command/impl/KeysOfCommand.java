package com.pumpkings.pkcrates.presentation.command.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.pumpkings.pkcrates.infrastructure.database.DatabaseManager;
import com.pumpkings.pkcrates.infrastructure.config.MessageManager;
import com.pumpkings.pkcrates.infrastructure.config.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * /crate keysOf <player>
 *
 * <p>Displays all virtual keys held by the specified player as a formatted
 * chat list. Physical keys are not shown (they live in the inventory).</p>
 *
 * <p>Requires: {@code pkcrates.admin.keysof}</p>
 */
public class KeysOfCommand {

    public static LiteralCommandNode<CommandSourceStack> build(
            DatabaseManager databaseManager,
            MessageManager messageManager) {

        return Commands.literal("keysOf")
                .requires(src -> src.getSender().hasPermission("pkcrates.admin.keysof"))
                .then(Commands.argument("player", ArgumentTypes.player())
                        .executes(context -> {
                            PlayerSelectorArgumentResolver resolver =
                                    context.getArgument("player", PlayerSelectorArgumentResolver.class);
                            List<Player> targets = resolver.resolve(context.getSource());
                            if (targets.isEmpty()) {
                                messageManager.sendMessage(context.getSource().getSender(), Messages.PLAYER_NOT_FOUND);
                                return Command.SINGLE_SUCCESS;
                            }

                            Player target = targets.get(0);
                            databaseManager.getAllVirtualKeys(target.getUniqueId()).thenAccept(keys -> {
                                if (keys.isEmpty()) {
                                    context.getSource().getSender().sendRichMessage(
                                            "<gray>" + target.getName() + " has no virtual keys.");
                                    return;
                                }
                                context.getSource().getSender().sendRichMessage(
                                        "<gold><bold>Virtual keys for <white>" + target.getName() + "</white>:</bold></gold>");
                                for (Map.Entry<String, Integer> entry : keys.entrySet()) {
                                    context.getSource().getSender().sendRichMessage(
                                            "  <gray>▸ <aqua>" + entry.getKey()
                                                    + " <dark_gray>→ <white>" + entry.getValue());
                                }
                            });

                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
    }
}
