package com.pumpkings.pkcrates.presentation.command.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.pumpkings.pkcrates.core.model.Crate;
import com.pumpkings.pkcrates.core.model.reward.IReward;
import com.pumpkings.pkcrates.core.model.reward.UnifiedReward;
import com.pumpkings.pkcrates.core.model.claim.ClaimReason;
import com.pumpkings.pkcrates.core.service.ClaimService;
import com.pumpkings.pkcrates.core.service.RewardsGenerator;
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
 * /crate roll <crate_id> <player>
 *
 * <p>Picks a random reward from the crate using the same weighted generator
 * as the normal opening flow, then delivers it directly to the target player.
 * If the player's inventory is full and the claim module is enabled,
 * the reward is stored in their claim queue automatically.</p>
 *
 * <p>Requires: {@code pkcrates.admin.roll}</p>
 */
public class RollCommand {

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

        return Commands.literal("roll")
                .requires(src -> src.getSender().hasPermission("pkcrates.admin.roll"))
                .then(Commands.argument("crate_id", StringArgumentType.word())
                        .suggests(crateSuggestions)
                        .then(Commands.argument("player", ArgumentTypes.player())
                                .executes(context -> {
                                    String crateId = StringArgumentType.getString(context, "crate_id");
                                    Crate crate = crateRegistry.getCrate(crateId);
                                    if (crate == null) {
                                        messageManager.sendMessage(context.getSource().getSender(),
                                                Messages.CRATE_NOT_FOUND, "<crate>", crateId);
                                        return Command.SINGLE_SUCCESS;
                                    }

                                    IReward reward = com.pumpkings.pkcrates.core.service.RewardsGenerator.generateReward(crate, null, null);
                                    if (reward == null) {
                                        messageManager.sendMessage(context.getSource().getSender(),
                                                Messages.CRATE_NO_REWARDS);
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

                                    // Attempt direct delivery; fall back to claim if needed
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
                                                Messages.CRATE_OPENING,
                                                "<crate>", crate.getName());
                                    }

                                    return Command.SINGLE_SUCCESS;
                                })))
                .build();
    }
}
