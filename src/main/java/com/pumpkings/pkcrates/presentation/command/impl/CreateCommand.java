package com.pumpkings.pkcrates.presentation.command.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.pumpkings.pkcrates.infrastructure.config.CrateRegistry;
import com.pumpkings.pkcrates.infrastructure.location.CrateLocationManager;
import com.pumpkings.pkcrates.infrastructure.config.MessageManager;
import com.pumpkings.pkcrates.infrastructure.config.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;
import org.bukkit.block.Block;

public class CreateCommand {

    public static LiteralCommandNode<CommandSourceStack> build(com.pumpkings.pkcrates.PkCratesPlugin plugin, CrateRegistry crateRegistry, CrateLocationManager locationMgr, com.pumpkings.pkcrates.infrastructure.display.HologramManager hologramManager, MessageManager messageManager) {
        return Commands.literal("create")
                .requires(source -> source.getSender().hasPermission("pkcrates.admin.create"))
                .then(Commands.argument("crate_id", StringArgumentType.word())
                        .executes(context -> {
                            String id = StringArgumentType.getString(context, "crate_id");
                            
                            if (crateRegistry.getCrate(id) != null) {
                                messageManager.sendMessage(context.getSource().getSender(), Messages.CRATE_ALREADY_EXISTS, "<crate>", id);
                                return Command.SINGLE_SUCCESS;
                            }
                            
                            crateRegistry.createCrate(id);
                            
                            // Audit
                            String actor = context.getSource().getSender() instanceof Player p ? p.getName() : "CONSOLE";
                            plugin.getAuditService().success(com.pumpkings.pkcrates.infrastructure.audit.api.AuditEvent.CRATE_CREATED, actor, id, null);
                            
                            // Add default hologram
                            com.pumpkings.pkcrates.core.model.Crate crate = crateRegistry.getCrate(id);
                            if (crate != null) {
                                java.util.List<String> content = new java.util.ArrayList<>();
                                content.add("<bold><gradient:#4287f5:#42d4f5>CRATE " + id.toUpperCase() + "</gradient></bold>");
                                content.add("<gray>Edit this in " + id + ".yml</gray>");
                                
                                com.pumpkings.pkcrates.core.model.HologramConfig holoConfig = new com.pumpkings.pkcrates.core.model.HologramConfig(
                                    content,
                                    org.bukkit.entity.Display.Billboard.CENTER,
                                    "none",
                                    true,
                                    1.0f
                                );
                                crate.setHologramConfig(holoConfig);
                                crateRegistry.saveCrate(crate);
                            }
                            
                            messageManager.sendMessage(context.getSource().getSender(), Messages.CRATE_CREATED, "<crate>", id);
                            
                            if (context.getSource().getSender() instanceof Player player) {
                                Block targetBlock = player.getTargetBlockExact(5);
                                if (targetBlock != null) {
                                    locationMgr.addLocation(targetBlock.getLocation(), id);
                                    if (crate != null) {
                                        hologramManager.spawnFor(targetBlock.getLocation(), crate);
                                    }
                                    messageManager.sendMessage(player, Messages.LOCATION_SET, "<crate>", id);
                                }
                            }
                            
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
    }
}
