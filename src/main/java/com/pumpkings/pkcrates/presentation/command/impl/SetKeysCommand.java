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

/**
 * /crate setKeys <key_id> <player> <amount>
 *
 * <p>Sets the virtual key count for a player to an exact value,
 * regardless of what they currently hold. Use 0 to reset completely.</p>
 *
 * <p>Only works with virtual keys.</p>
 *
 * <p>Requires: {@code pkcrates.admin.setkeys}</p>
 */
public class SetKeysCommand {

    public static LiteralCommandNode<CommandSourceStack> build(
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

        return Commands.literal("setKeys")
                .requires(src -> src.getSender().hasPermission("pkcrates.admin.setkeys"))
                .then(Commands.argument("key_id", StringArgumentType.word())
                        .suggests(keySuggestions)
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(context -> {
                                            String keyId = StringArgumentType.getString(context, "key_id");
                                            int amount = IntegerArgumentType.getInteger(context, "amount");

                                            IKey key = keyRegistry.getKey(keyId);
                                            if (key == null) {
                                                messageManager.sendMessage(context.getSource().getSender(),
                                                        Messages.KEY_NOT_FOUND, "<key>", keyId);
                                                return Command.SINGLE_SUCCESS;
                                            }
                                            if (!key.isVirtual()) {
                                                context.getSource().getSender().sendRichMessage(
                                                        "<red>setKeys only works with virtual keys.");
                                                return Command.SINGLE_SUCCESS;
                                            }

                                            PlayerSelectorArgumentResolver resolver =
                                                    context.getArgument("player", PlayerSelectorArgumentResolver.class);
                                            List<Player> targets = resolver.resolve(context.getSource());
                                            if (targets.isEmpty()) {
                                                messageManager.sendMessage(context.getSource().getSender(),
                                                        Messages.PLAYER_NOT_FOUND);
                                                return Command.SINGLE_SUCCESS;
                                            }

                                            Player target = targets.get(0);
                                            databaseManager.setVirtualKeys(target.getUniqueId(), keyId, amount)
                                                    .thenRun(() -> context.getSource().getSender().sendRichMessage(
                                                            "<green>Set <gold>" + keyId + "</gold> keys for <white>"
                                                                    + target.getName() + "</white> to <white>"
                                                                    + amount + "</white>."));

                                            return Command.SINGLE_SUCCESS;
                                        }))))
                .build();
    }
}
