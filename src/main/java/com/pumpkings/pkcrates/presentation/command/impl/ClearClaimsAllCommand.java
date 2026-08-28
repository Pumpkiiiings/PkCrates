package com.pumpkings.pkcrates.presentation.command.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.pumpkings.pkcrates.core.service.ClaimService;
import com.pumpkings.pkcrates.infrastructure.config.MessageManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;
import java.util.Map;

public class ClearClaimsAllCommand {

    public static LiteralCommandNode<CommandSourceStack> build(
            com.pumpkings.pkcrates.PkCratesPlugin plugin,
            ClaimService claimService,
            MessageManager messageManager) {

        return Commands.literal("clearClaimsAll")
                .requires(source -> source.getSender().hasPermission("pkcrates.admin.claim.clearall"))
                .executes(context -> {
                    int totalCleared = claimService.clearAllClaims();
                    
                    String actor = context.getSource().getSender() instanceof Player p ? p.getName() : "CONSOLE";
                    plugin.getAuditService().warning(com.pumpkings.pkcrates.infrastructure.audit.api.AuditEvent.CLAIM_CLEARED, actor, "@all", 
                        Map.of("cleared", String.valueOf(totalCleared)));
                        
                    context.getSource().getSender().sendRichMessage(
                            "<green>Successfully cleared all pending claims globally.");
                    return Command.SINGLE_SUCCESS;
                })
                .build();
    }
}
