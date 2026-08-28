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
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * /crate claim
 *
 * <p>Opens the {@link ClaimMenu} for the executing player so they can view
 * and collect all their pending rewards.</p>
 *
 * <p>Requires permission: {@code pkcrates.claim}</p>
 */
public class ClaimCommand {

    /**
     * Builds the {@code claim} literal node to be registered under
     * the root {@code /crate} command.
     *
     * @param plugin         The owning plugin (for scheduler).
     * @param claimService   The claim service used by the menu.
     * @param claimConfig    The claim module configuration.
     * @param menuManager    The global menu manager.
     * @param messageManager For sending feedback messages.
     * @return A fully configured Brigadier literal command node.
     */
    public static LiteralCommandNode<CommandSourceStack> build(
            Plugin plugin,
            ClaimService claimService,
            ClaimConfig claimConfig,
            MenuManager menuManager,
            MessageManager messageManager) {

        return Commands.literal("claim")
                .requires(source -> source.getSender().hasPermission("pkcrates.claim"))
                .executes(context -> {
                    if (!(context.getSource().getSender() instanceof Player player)) {
                        messageManager.sendMessage(context.getSource().getSender(), Messages.PLAYER_ONLY);
                        return Command.SINGLE_SUCCESS;
                    }

                    if (!claimConfig.isEnabled()) {
                        messageManager.sendMessage(player, Messages.CLAIM_DISABLED);
                        return Command.SINGLE_SUCCESS;
                    }

                    // Open the menu on the next tick (Bukkit requirement)
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            new ClaimMenu(menuManager, player, claimService, claimConfig, messageManager).open()
                    );

                    return Command.SINGLE_SUCCESS;
                })
                .build();
    }
}
