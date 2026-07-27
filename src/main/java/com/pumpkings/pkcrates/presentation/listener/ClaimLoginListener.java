package com.pumpkings.pkcrates.presentation.listener;

import com.pumpkings.pkcrates.core.service.ClaimService;
import com.pumpkings.pkcrates.infrastructure.claim.ClaimConfig;
import com.pumpkings.pkcrates.infrastructure.config.MessageManager;
import com.pumpkings.pkcrates.infrastructure.config.Messages;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

/**
 * Notifies a player on login if they have pending claims waiting.
 *
 * <p>The notification is sent 1 tick after the join event so the player's
 * client is fully ready to display chat messages.</p>
 *
 * <p>Only fires when:
 * <ul>
 *   <li>{@link ClaimConfig#isEnabled()} → {@code true}</li>
 *   <li>{@link ClaimConfig#notifyOnLogin()} → {@code true}</li>
 *   <li>The player has at least 1 pending claim.</li>
 * </ul>
 * </p>
 */
public class ClaimLoginListener implements Listener {

    private final Plugin plugin;
    private final ClaimService claimService;
    private final ClaimConfig claimConfig;
    private final MessageManager messageManager;

    /**
     * @param plugin         Owning plugin used for the scheduler.
     * @param claimService   Used to query the pending claim count.
     * @param claimConfig    Claim module configuration.
     * @param messageManager Used to dispatch the notification.
     */
    public ClaimLoginListener(Plugin plugin,
                              ClaimService claimService,
                              ClaimConfig claimConfig,
                              MessageManager messageManager) {
        this.plugin = plugin;
        this.claimService = claimService;
        this.claimConfig = claimConfig;
        this.messageManager = messageManager;
    }

    /**
     * Fired at MONITOR priority so game-state is settled before we query claims.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!claimConfig.isEnabled() || !claimConfig.notifyOnLogin()) return;

        Player player = event.getPlayer();
        int pending = claimService.getPendingAmount(player.getUniqueId());
        if (pending <= 0) return;

        // Delay 1 tick so the client renders the join message first
        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                messageManager.sendMessage(
                        player,
                        Messages.CLAIM_LOGIN_NOTIFICATION,
                        "<pending>", String.valueOf(pending)),
                20L
        );
    }
}
