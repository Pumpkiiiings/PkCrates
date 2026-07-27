package com.pumpkings.pkcrates.presentation.command.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.pumpkings.pkcrates.core.model.Crate;
import com.pumpkings.pkcrates.infrastructure.config.CrateRegistry;
import com.pumpkings.pkcrates.infrastructure.config.MessageManager;
import com.pumpkings.pkcrates.infrastructure.config.Messages;
import com.pumpkings.pkcrates.presentation.menu.MenuManager;
import com.pumpkings.pkcrates.presentation.menu.user.CratePreviewMenu;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import java.util.List;

/**
 * /crate peek <crate_id> [player]
 *
 * <p>Opens the crate preview menu for the executing player (or a target player
 * if an admin specifies one). No key is consumed.</p>
 *
 * <p>Requires: {@code pkcrates.peek}</p>
 */
public class PeekCommand {

    public static LiteralCommandNode<CommandSourceStack> build(
            Plugin plugin,
            CrateRegistry crateRegistry,
            MenuManager menuManager,
            MessageManager messageManager) {

        SuggestionProvider<CommandSourceStack> crateSuggestions = (ctx, builder) -> {
            crateRegistry.getAllCrates().forEach(c -> {
                if (c.getId().toLowerCase().startsWith(builder.getRemainingLowerCase()))
                    builder.suggest(c.getId());
            });
            return builder.buildFuture();
        };

        return Commands.literal("peek")
                .requires(src -> src.getSender().hasPermission("pkcrates.peek"))
                .then(Commands.argument("crate_id", StringArgumentType.word())
                        .suggests(crateSuggestions)
                        // /crate peek <crate_id>  — opens for the executor
                        .executes(context -> {
                            if (!(context.getSource().getSender() instanceof Player player)) {
                                messageManager.sendMessage(context.getSource().getSender(), Messages.PLAYER_ONLY);
                                return Command.SINGLE_SUCCESS;
                            }
                            String crateId = StringArgumentType.getString(context, "crate_id");
                            Crate crate = crateRegistry.getCrate(crateId);
                            if (crate == null) {
                                messageManager.sendMessage(player, Messages.CRATE_NOT_FOUND, "<crate>", crateId);
                                return Command.SINGLE_SUCCESS;
                            }
                            plugin.getServer().getScheduler().runTask(plugin, () ->
                                    new CratePreviewMenu(menuManager, player, crate).open());
                            return Command.SINGLE_SUCCESS;
                        })
                        // /crate peek <crate_id> <player>  — admin opens for another player
                        .then(Commands.argument("target", ArgumentTypes.player())
                                .requires(src -> src.getSender().hasPermission("pkcrates.admin.peek"))
                                .executes(context -> {
                                    String crateId = StringArgumentType.getString(context, "crate_id");
                                    Crate crate = crateRegistry.getCrate(crateId);
                                    if (crate == null) {
                                        messageManager.sendMessage(context.getSource().getSender(),
                                                Messages.CRATE_NOT_FOUND, "<crate>", crateId);
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    PlayerSelectorArgumentResolver resolver =
                                            context.getArgument("target", PlayerSelectorArgumentResolver.class);
                                    List<Player> targets = resolver.resolve(context.getSource());
                                    if (targets.isEmpty()) {
                                        messageManager.sendMessage(context.getSource().getSender(), Messages.PLAYER_NOT_FOUND);
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    Player target = targets.get(0);
                                    plugin.getServer().getScheduler().runTask(plugin, () ->
                                            new CratePreviewMenu(menuManager, target, crate).open());
                                    return Command.SINGLE_SUCCESS;
                                })))
                .build();
    }
}
