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
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.Map;

public class BroadcastKeyCommand {

    public static LiteralCommandNode<CommandSourceStack> build(com.pumpkings.pkcrates.PkCratesPlugin plugin, KeyRegistry keyRegistry, KeyService keyService, MessageManager messageManager) {
        
        SuggestionProvider<CommandSourceStack> keySuggestions = (context, builder) -> {
            for (IKey key : keyRegistry.getAllKeys()) {
                if (key.getId().toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                    builder.suggest(key.getId());
                }
            }
            return builder.buildFuture();
        };

        return Commands.literal("broadcastKey")
                .requires(source -> source.getSender().hasPermission("pkcrates.admin.broadcastkey"))
                .then(Commands.argument("key_id", StringArgumentType.word())
                        .suggests(keySuggestions)
                        .executes(context -> execute(context.getSource(), plugin, keyRegistry, keyService,
                                messageManager, StringArgumentType.getString(context, "key_id"), 1))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                .executes(context -> execute(context.getSource(), plugin, keyRegistry, keyService,
                                        messageManager, StringArgumentType.getString(context, "key_id"),
                                        IntegerArgumentType.getInteger(context, "amount")))))
                .build();
    }

    private static int execute(CommandSourceStack source, com.pumpkings.pkcrates.PkCratesPlugin plugin, KeyRegistry keyRegistry,
                                KeyService keyService, MessageManager messageManager,
                                String keyId, int amount) {
        IKey key = keyRegistry.getKey(keyId);
        
        if (key == null) {
            messageManager.sendMessage(source.getSender(), Messages.KEY_NOT_FOUND, "<key>", keyId);
            return Command.SINGLE_SUCCESS;
        }

        int onlineCount = Bukkit.getOnlinePlayers().size();
        if (onlineCount == 0) {
            // Can't give keys to nobody, but technically success command execution
            messageManager.sendMessage(source.getSender(), Messages.KEY_GIVEN, 
                    "<amount>", String.valueOf(amount), 
                    "<key>", keyId, 
                    "<player>", "nobody (0 online)");
            return Command.SINGLE_SUCCESS;
        }

        for (Player target : Bukkit.getOnlinePlayers()) {
            keyService.giveKey(target, key, amount);
        }
        
        String actor = source.getSender() instanceof Player p ? p.getName() : "CONSOLE";
        plugin.getAuditService().success(com.pumpkings.pkcrates.infrastructure.audit.api.AuditEvent.KEY_GIVEN, actor, "@all", 
            Map.of("key", keyId, "amount", String.valueOf(amount), "players", String.valueOf(onlineCount)));
        
        messageManager.sendMessage(source.getSender(), Messages.KEY_GIVEN, 
                "<amount>", String.valueOf(amount), 
                "<key>", keyId, 
                "<player>", "all online players");
                
        return Command.SINGLE_SUCCESS;
    }
}
