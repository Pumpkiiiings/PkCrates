package com.pumpkings.pkcrates.presentation.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import com.pumpkings.pkcrates.infrastructure.config.MessageManager;
import com.pumpkings.pkcrates.infrastructure.config.Messages;

public class ChatPromptManager implements Listener {

    private final Plugin plugin;
    private final Map<UUID, Consumer<String>> activePrompts;
    private final MessageManager messageManager;

    public ChatPromptManager(Plugin plugin, MessageManager messageManager) {
        this.plugin = plugin;
        this.activePrompts = new HashMap<>();
        this.messageManager = messageManager;
    }

    public void prompt(Player player, String messageKey, Consumer<String> onResponse) {
        messageManager.sendMessage(player, messageKey);
        messageManager.sendMessage(player, Messages.PROMPT_CANCEL);
        activePrompts.put(player.getUniqueId(), onResponse);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (activePrompts.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            String input = PlainTextComponentSerializer.plainText().serialize(event.message());
            
            Consumer<String> action = activePrompts.remove(player.getUniqueId());
            
            if (input.equalsIgnoreCase("cancelar")) {
                messageManager.sendMessage(player, Messages.ACTION_CANCELLED);
                return;
            }

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                action.accept(input);
            });
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        activePrompts.remove(event.getPlayer().getUniqueId());
    }
}
