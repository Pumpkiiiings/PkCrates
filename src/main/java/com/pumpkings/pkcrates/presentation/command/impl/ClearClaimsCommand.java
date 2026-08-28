package com.pumpkings.pkcrates.presentation.command.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.pumpkings.pkcrates.core.service.ClaimService;
import com.pumpkings.pkcrates.infrastructure.config.MessageManager;
import com.pumpkings.pkcrates.infrastructure.config.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.entity.Player;
import java.util.List;
import java.util.Map;

public class ClearClaimsCommand {

    public static LiteralCommandNode<CommandSourceStack> build(
            com.pumpkings.pkcrates.PkCratesPlugin plugin,
            ClaimService claimService,
            MessageManager messageManager) {

        return Commands.literal("clearClaims")
                .requires(source -> source.getSender().hasPermission("pkcrates.admin.claim.clear"))
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
                            int cleared = claimService.clearClaims(target.getUniqueId());

                            String actor = context.getSource().getSender() instanceof Player p ? p.getName() : "CONSOLE";
                            plugin.getAuditService().warning(com.pumpkings.pkcrates.infrastructure.audit.api.AuditEvent.CLAIM_CLEARED, actor, target.getName(), 
                                Map.of("cleared", String.valueOf(cleared)));

                            messageManager.sendMessage(
                                    context.getSource().getSender(),
                                    Messages.CLAIM_CLEARED,
                                    "<player>", target.getName());

                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
    }
}
