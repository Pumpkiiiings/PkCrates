package com.pumpkings.pkcrates.presentation.command.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.pumpkings.pkcrates.core.service.ClaimService;
import com.pumpkings.pkcrates.infrastructure.claim.ClaimConfig;
import com.pumpkings.pkcrates.infrastructure.config.MessageManager;
import com.pumpkings.pkcrates.infrastructure.config.Messages;
import com.pumpkings.pkcrates.presentation.menu.MenuManager;
import com.pumpkings.pkcrates.presentation.menu.user.ClaimMenu;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * /crate inspectClaim &lt;player&gt;
 *
 * <p>Opens a read-only view of another player's pending claim rewards.
 * The executing player must be online; the target may be offline (in which
 * case the admin sees a message listing the count, not a menu).</p>
 *
 * <p>Requires permission: {@code pkcrates.admin.claim.inspect}</p>
 */
public class InspectClaimCommand {

    /**
     * Builds the {@code inspectClaim} literal node.
     *
     * @param plugin         The owning plugin (for scheduler).
     * @param claimService   The claim service.
     * @param claimConfig    The claim module configuration.
     * @param menuManager    The global menu manager.
     * @param messageManager For sending feedback.
     * @return Configured Brigadier literal node.
     */
    public static LiteralCommandNode<CommandSourceStack> build(
            Plugin plugin,
            ClaimService claimService,
            ClaimConfig claimConfig,
            MenuManager menuManager,
            MessageManager messageManager) {

        return Commands.literal("inspectClaim")
                .requires(source -> source.getSender().hasPermission("pkcrates.admin.claim.inspect"))
                .then(Commands.argument("player", ArgumentTypes.player())
                        .executes(context -> {
                            if (!(context.getSource().getSender() instanceof Player executor)) {
                                messageManager.sendMessage(context.getSource().getSender(), Messages.PLAYER_ONLY);
                                return Command.SINGLE_SUCCESS;
                            }

                            PlayerSelectorArgumentResolver resolver =
                                    context.getArgument("player", PlayerSelectorArgumentResolver.class);
                            List<Player> targets = resolver.resolve(context.getSource());

                            if (targets.isEmpty()) {
                                messageManager.sendMessage(executor, Messages.PLAYER_NOT_FOUND);
                                return Command.SINGLE_SUCCESS;
                            }

                            Player target = targets.get(0);
                            messageManager.sendMessage(executor, Messages.CLAIM_INSPECTING,
                                    "<player>", target.getName());

                            // Open the ClaimMenu as if the executor were the target player
                            // The menu reads claims by UUID, so passing target works correctly
                            plugin.getServer().getScheduler().runTask(plugin, () ->
                                    new ClaimMenu(menuManager, target, claimService, claimConfig, messageManager)
                                            .open()
                            );

                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
    }
}
