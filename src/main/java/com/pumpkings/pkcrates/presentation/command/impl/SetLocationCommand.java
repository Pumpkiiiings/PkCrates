package com.pumpkings.pkcrates.presentation.command.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.pumpkings.pkcrates.core.model.Crate;
import com.pumpkings.pkcrates.infrastructure.config.CrateRegistry;
import com.pumpkings.pkcrates.infrastructure.location.CrateLocationManager;
import com.pumpkings.pkcrates.infrastructure.config.MessageManager;
import com.pumpkings.pkcrates.infrastructure.config.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.stream.Collectors;

public class SetLocationCommand {

    public static LiteralCommandNode<CommandSourceStack> build(CrateRegistry crateRegistry, CrateLocationManager locationMgr, com.pumpkings.pkcrates.infrastructure.display.HologramManager hologramManager, MessageManager messageManager) {
        
        SuggestionProvider<CommandSourceStack> crateSuggestions = (context, builder) -> {
            for (Crate crate : crateRegistry.getAllCrates()) {
                if (crate.getId().toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                    builder.suggest(crate.getId());
                }
            }
            return builder.buildFuture();
        };

        return Commands.literal("setlocation")
                .requires(source -> source.getSender().hasPermission("pkcrates.admin.setlocation"))
                .then(Commands.argument("crate_id", StringArgumentType.word())
                        .suggests(crateSuggestions)
                        .executes(context -> {
                            if (!(context.getSource().getSender() instanceof Player player)) {
                                messageManager.sendMessage(context.getSource().getSender(), Messages.PLAYER_ONLY);
                                return Command.SINGLE_SUCCESS;
                            }
                            
                            String id = StringArgumentType.getString(context, "crate_id");
                            Crate crate = crateRegistry.getCrate(id);
                            
                            if (crate == null) {
                                messageManager.sendMessage(player, Messages.CRATE_NOT_FOUND, "<crate>", id);
                                return Command.SINGLE_SUCCESS;
                            }
                            
                            Block targetBlock = player.getTargetBlockExact(5);
                            if (targetBlock == null || targetBlock.getType().isAir()) {
                                messageManager.sendMessage(player, Messages.LOCATION_LOOK_AT_BLOCK);
                            } else {
                                locationMgr.addLocation(targetBlock.getLocation(), id);
                                hologramManager.spawnFor(targetBlock.getLocation(), crate);
                                messageManager.sendMessage(player, Messages.LOCATION_SET, "<crate>", id);
                            }
                            
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
    }
}
