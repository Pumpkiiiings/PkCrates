package com.pumpkings.pkcrates.presentation.command.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.pumpkings.pkcrates.core.model.key.IKey;
import com.pumpkings.pkcrates.core.service.KeyService;
import com.pumpkings.pkcrates.infrastructure.key.KeyRegistry;
import com.pumpkings.pkcrates.infrastructure.config.MessageManager;
import com.pumpkings.pkcrates.infrastructure.config.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class GiveKeyCommand {

    public static LiteralCommandNode<CommandSourceStack> build(com.pumpkings.pkcrates.PkCratesPlugin plugin, KeyRegistry keyRegistry, KeyService keyService, MessageManager messageManager) {
        
        SuggestionProvider<CommandSourceStack> keySuggestions = (context, builder) -> {
            for (IKey key : keyRegistry.getAllKeys()) {
                if (key.getId().toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                    builder.suggest(key.getId());
                }
            }
            return builder.buildFuture();
        };

        return Commands.literal("givekey")
                .requires(source -> source.getSender().hasPermission("pkcrates.admin.givekey"))
                .then(Commands.argument("player", ArgumentTypes.player())
                        .then(Commands.argument("key_id", StringArgumentType.word())
                                .suggests(keySuggestions)
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                        .executes(context -> {
                                            
                                            PlayerSelectorArgumentResolver playerResolver = context.getArgument("player", PlayerSelectorArgumentResolver.class);
                                            List<Player> players = playerResolver.resolve(context.getSource());
                                            
                                            if (players.isEmpty()) {
                                                messageManager.sendMessage(context.getSource().getSender(), Messages.PLAYER_NOT_FOUND);
                                                return Command.SINGLE_SUCCESS;
                                            }
                                            
                                            Player target = players.get(0);
                                            String keyId = StringArgumentType.getString(context, "key_id");
                                            int amount = IntegerArgumentType.getInteger(context, "amount");
                                            
                                            IKey key = keyRegistry.getKey(keyId);
                                            if (key == null) {
                                                messageManager.sendMessage(context.getSource().getSender(), Messages.KEY_NOT_FOUND, "<key>", keyId);
                                                return Command.SINGLE_SUCCESS;
                                            }
                                            
                                            keyService.giveKey(target, key, amount);
                                            
                                            String actor = context.getSource().getSender() instanceof Player p ? p.getName() : "CONSOLE";
                                            plugin.getAuditService().success(com.pumpkings.pkcrates.infrastructure.audit.api.AuditEvent.KEY_GIVEN, actor, target.getName(), 
                                                Map.of("key", keyId, "amount", String.valueOf(amount)));
                                            
                                            messageManager.sendMessage(context.getSource().getSender(), Messages.KEY_GIVEN, 
                                                    "<amount>", String.valueOf(amount), 
                                                    "<key>", keyId, 
                                                    "<player>", target.getName());
                                            
                                            return Command.SINGLE_SUCCESS;
                                        }))))
                .build();
    }
}
