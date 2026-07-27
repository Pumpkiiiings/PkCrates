package com.pumpkings.pkcrates.presentation.command.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.pumpkings.pkcrates.core.model.key.IKey;
import com.pumpkings.pkcrates.infrastructure.database.DatabaseManager;
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

public class RevokeKeyCommand {

    public static LiteralCommandNode<CommandSourceStack> build(
            com.pumpkings.pkcrates.PkCratesPlugin plugin,
            KeyRegistry keyRegistry,
            DatabaseManager databaseManager,
            MessageManager messageManager) {

        SuggestionProvider<CommandSourceStack> keySuggestions = (ctx, builder) -> {
            keyRegistry.getAllKeys().stream()
                    .filter(IKey::isVirtual)
                    .forEach(k -> {
                        if (k.getId().toLowerCase().startsWith(builder.getRemainingLowerCase()))
                            builder.suggest(k.getId());
                    });
            return builder.buildFuture();
        };

        return Commands.literal("revokeKey")
                .requires(src -> src.getSender().hasPermission("pkcrates.admin.revokekey"))
                .then(Commands.argument("key_id", StringArgumentType.word())
                        .suggests(keySuggestions)
                        .then(Commands.argument("player", ArgumentTypes.player())
                                // /crate revokeKey <key_id> <player>  — defaults to 1
                                .executes(context -> executeRevoke(context.getSource(), plugin, keyRegistry,
                                        databaseManager, messageManager,
                                        StringArgumentType.getString(context, "key_id"),
                                        context.getArgument("player", PlayerSelectorArgumentResolver.class), 1))
                                // /crate revokeKey <key_id> <player> <amount>
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(context -> executeRevoke(context.getSource(), plugin, keyRegistry,
                                                databaseManager, messageManager,
                                                StringArgumentType.getString(context, "key_id"),
                                                context.getArgument("player", PlayerSelectorArgumentResolver.class),
                                                IntegerArgumentType.getInteger(context, "amount"))))))
                .build();
    }

    private static int executeRevoke(CommandSourceStack source, com.pumpkings.pkcrates.PkCratesPlugin plugin, KeyRegistry keyRegistry,
                                     DatabaseManager databaseManager, MessageManager messageManager,
                                     String keyId, PlayerSelectorArgumentResolver playerResolver, int amount) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
                                     
        List<Player> players = playerResolver.resolve(source);
        if (players.isEmpty()) {
            messageManager.sendMessage(source.getSender(), Messages.PLAYER_NOT_FOUND);
            return Command.SINGLE_SUCCESS;
        }

        Player target = players.get(0);
        IKey key = keyRegistry.getKey(keyId);

        if (key == null) {
            messageManager.sendMessage(source.getSender(), Messages.KEY_NOT_FOUND, "<key>", keyId);
            return Command.SINGLE_SUCCESS;
        }

        if (!key.isVirtual()) {
            source.getSender().sendRichMessage("<red>Revoke command only works with virtual keys. Use /clear to remove physical keys.");
            return Command.SINGLE_SUCCESS;
        }

        databaseManager.getVirtualKeys(target.getUniqueId(), keyId).thenAccept(currentAmount -> {
            int finalRevoke = Math.min(amount, currentAmount);

            if (finalRevoke > 0) {
                databaseManager.takeVirtualKeys(target.getUniqueId(), keyId, finalRevoke).thenAccept(success -> {
                    if (success) {
                        source.getSender().sendRichMessage("<green>Successfully revoked " + finalRevoke + " " + keyId + " keys from " + target.getName());
                        
                        String actor = source.getSender() instanceof Player p ? p.getName() : "CONSOLE";
                        plugin.getAuditService().warning(com.pumpkings.pkcrates.infrastructure.audit.api.AuditEvent.KEY_REMOVED, actor, target.getName(), 
                            Map.of("key", keyId, "amount", String.valueOf(finalRevoke)));
                    } else {
                        source.getSender().sendRichMessage("<red>Failed to revoke keys from " + target.getName() + ".");
                    }
                });
            } else {
                source.getSender().sendRichMessage("<red>" + target.getName() + " does not have any virtual keys for " + keyId + ".");
            }
        });

        return Command.SINGLE_SUCCESS;
    }
}
