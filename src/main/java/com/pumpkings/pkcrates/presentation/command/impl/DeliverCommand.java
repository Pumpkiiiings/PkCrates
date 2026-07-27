package com.pumpkings.pkcrates.presentation.command.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.pumpkings.pkcrates.core.model.Crate;
import com.pumpkings.pkcrates.core.model.reward.IReward;
import com.pumpkings.pkcrates.core.model.reward.UnifiedReward;
import com.pumpkings.pkcrates.core.service.ClaimService;
import com.pumpkings.pkcrates.core.model.claim.ClaimReason;
import com.pumpkings.pkcrates.infrastructure.claim.ClaimConfig;
import com.pumpkings.pkcrates.infrastructure.config.CrateRegistry;
import com.pumpkings.pkcrates.infrastructure.config.MessageManager;
import com.pumpkings.pkcrates.infrastructure.config.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /crate deliver <crate_id> <reward_id> <player>
 *
 * <p>Forces delivery of a specific reward from a crate directly to a player.
 * If the player's inventory is full and the claim module is enabled,
 * the reward is stored in their claim queue automatically.</p>
 *
 * <p>Requires: {@code pkcrates.admin.deliver}</p>
 */
public class DeliverCommand {

    public static LiteralCommandNode<CommandSourceStack> build(
            CrateRegistry crateRegistry,
            ClaimService claimService,
            ClaimConfig claimConfig,
            MessageManager messageManager) {

        SuggestionProvider<CommandSourceStack> crateSuggestions = (ctx, builder) -> {
            crateRegistry.getAllCrates().forEach(c -> {
                if (c.getId().toLowerCase().startsWith(builder.getRemainingLowerCase()))
                    builder.suggest(c.getId());
            });
            return builder.buildFuture();
        };

        SuggestionProvider<CommandSourceStack> rewardSuggestions = (ctx, builder) -> {
            try {
                String crateId = StringArgumentType.getString(ctx, "crate_id");
                Crate crate = crateRegistry.getCrate(crateId);
                if (crate != null) {
                    crate.getRewards().forEach(r -> {
                        if (r.getId().toLowerCase().startsWith(builder.getRemainingLowerCase()))
                            builder.suggest(r.getId());
                    });
                }
            } catch (Exception ignored) {}
            return builder.buildFuture();
        };

        return Commands.literal("deliver")
                .requires(src -> src.getSender().hasPermission("pkcrates.admin.deliver"))
                .then(Commands.argument("crate_id", StringArgumentType.word())
                        .suggests(crateSuggestions)
                        .then(Commands.argument("reward_id", StringArgumentType.word())
                                .suggests(rewardSuggestions)
                                .then(Commands.argument("player", ArgumentTypes.player())
                                        .executes(context -> {
                                            String crateId = StringArgumentType.getString(context, "crate_id");
                                            String rewardId = StringArgumentType.getString(context, "reward_id");

                                            Crate crate = crateRegistry.getCrate(crateId);
                                            if (crate == null) {
                                                messageManager.sendMessage(context.getSource().getSender(),
                                                        Messages.CRATE_NOT_FOUND, "<crate>", crateId);
                                                return Command.SINGLE_SUCCESS;
                                            }

                                            IReward reward = crate.getRewards().stream()
                                                    .filter(r -> r.getId().equalsIgnoreCase(rewardId))
                                                    .findFirst().orElse(null);
                                            if (reward == null) {
                                                messageManager.sendMessage(context.getSource().getSender(),
                                                        Messages.REWARD_NOT_FOUND, "<reward>", rewardId);
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

                                            // Try direct delivery; fall back to claim if inventory full
                                            if (reward instanceof UnifiedReward unified
                                                    && !unified.getItems().isEmpty()
                                                    && claimConfig.isEnabled()
                                                    && claimConfig.storeIfInventoryFull()
                                                    && target.getInventory().firstEmpty() == -1) {
                                                claimService.addClaim(target, reward, crateId, ClaimReason.FORCED);
                                                messageManager.sendMessage(context.getSource().getSender(),
                                                        Messages.CLAIM_STORED_NOTIFICATION);
                                            } else {
                                                reward.give(target);
                                                messageManager.sendMessage(context.getSource().getSender(),
                                                        Messages.KEY_GIVEN,
                                                        "<player>", target.getName(),
                                                        "<key>", rewardId,
                                                        "<amount>", "1");
                                            }

                                            return Command.SINGLE_SUCCESS;
                                        }))))
                .build();
    }
}
