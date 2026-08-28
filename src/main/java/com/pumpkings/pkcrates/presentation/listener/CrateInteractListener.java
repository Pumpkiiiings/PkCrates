package com.pumpkings.pkcrates.presentation.listener;

import com.pumpkings.pkcrates.core.model.Crate;
import com.pumpkings.pkcrates.core.model.key.IKey;
import com.pumpkings.pkcrates.core.service.KeyService;
import com.pumpkings.pkcrates.infrastructure.config.CrateRegistry;
import com.pumpkings.pkcrates.infrastructure.key.KeyRegistry;
import com.pumpkings.pkcrates.infrastructure.location.CrateLocationManager;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import com.pumpkings.pkcrates.presentation.menu.MenuManager;
import com.pumpkings.pkcrates.presentation.menu.user.CratePreviewMenu;
import com.pumpkings.pkcrates.infrastructure.config.MessageManager;
import com.pumpkings.pkcrates.infrastructure.config.Messages;

public class CrateInteractListener implements Listener {

    private final Plugin plugin;
    private final CrateLocationManager locationMgr;
    private final CrateRegistry crateRegistry;
    private final KeyService keyService;
    private final KeyRegistry keyRegistry;
    private final MenuManager menuManager;
    
    private final com.pumpkings.pkcrates.core.service.SessionManager sessionManager;
    private final com.pumpkings.pkcrates.core.animation.AnimationRegistry animationRegistry;
    private final com.pumpkings.pkcrates.core.task.CrateTickTask tickTask;
    private final MessageManager messageManager;

    /** Runs future continuations on the main thread; see {@link #tryConsumeAnyKey}. */
    private final java.util.concurrent.Executor mainThread;

    public CrateInteractListener(Plugin plugin, CrateLocationManager locationMgr, CrateRegistry crateRegistry, KeyService keyService, KeyRegistry keyRegistry, MenuManager menuManager, com.pumpkings.pkcrates.core.service.SessionManager sessionManager, com.pumpkings.pkcrates.core.animation.AnimationRegistry animationRegistry, com.pumpkings.pkcrates.core.task.CrateTickTask tickTask, MessageManager messageManager) {
        this.plugin = plugin;
        this.locationMgr = locationMgr;
        this.crateRegistry = crateRegistry;
        this.keyService = keyService;
        this.keyRegistry = keyRegistry;
        this.menuManager = menuManager;
        this.sessionManager = sessionManager;
        this.animationRegistry = animationRegistry;
        this.tickTask = tickTask;
        this.messageManager = messageManager;
        this.mainThread = new com.pumpkings.pkcrates.infrastructure.scheduler.MainThreadExecutor(plugin);
    }

    private com.pumpkings.pkcrates.core.service.MassOpeningService massOpeningService;
    private com.pumpkings.pkcrates.infrastructure.config.MassOpeningGlobalSettings massOpeningGlobalSettings;

    public void setMassOpeningService(com.pumpkings.pkcrates.core.service.MassOpeningService massOpeningService,
                                      com.pumpkings.pkcrates.infrastructure.config.MassOpeningGlobalSettings massOpeningGlobalSettings) {
        this.massOpeningService = massOpeningService;
        this.massOpeningGlobalSettings = massOpeningGlobalSettings;
    }

    @EventHandler
    public void onCrateInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        String crateId = locationMgr.getCrateAt(block.getLocation());
        if (crateId == null) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        Location loc = block.getLocation();

        Crate crate = crateRegistry.getCrate(crateId);
        if (crate == null) {
            messageManager.sendMessage(player, Messages.CRATE_NOT_FOUND, "<crate>", crateId);
            return;
        }

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            new CratePreviewMenu(menuManager, player, crate).open();
            return;
        }

        if (sessionManager.isCrateInUse(loc)) {
            messageManager.sendMessage(player, Messages.CRATE_ALREADY_IN_USE);
            return;
        }

        List<String> acceptedKeys = crate.getAcceptedKeys();
        if (acceptedKeys.isEmpty()) {
            messageManager.sendMessage(player, Messages.CRATE_NO_KEYS_CONFIGURED);
            return;
        }

        if (massOpeningService != null && massOpeningGlobalSettings != null
                && massOpeningGlobalSettings.isEnabled()
                && crate.getMassOpeningConfig() != null
                && crate.getMassOpeningConfig().isEnabled()) {

            // Shift+RightClick → always open mass opening menu
            if (player.isSneaking()) {
                new com.pumpkings.pkcrates.presentation.menu.user.MassOpeningMenu(
                        plugin, menuManager, player, crate, massOpeningService, keyService, keyRegistry).open();
                return;
            }


        }

        doSingleOpening(player, crate, loc, acceptedKeys, crateId);

    }

    private void doSingleOpening(Player player, Crate crate, Location loc, List<String> acceptedKeys, String crateId) {
        // Lock session tentatively to avoid double clicks
        com.pumpkings.pkcrates.core.model.session.CrateSession tentativeSession = new com.pumpkings.pkcrates.core.model.session.CrateSession(player, crate, loc, null);
        sessionManager.startSession(loc, tentativeSession);
        
        com.pumpkings.pkcrates.PkCratesPlugin pkPlugin = (com.pumpkings.pkcrates.PkCratesPlugin) plugin;
        String playerIp = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "";
        
        pkPlugin.getDatabaseManager().checkAndRegisterIpLimit(playerIp, crateId, player.getUniqueId(), crate.getIpLimit())
            .whenCompleteAsync((allowed, dbError) -> {
                if (dbError != null) {
                    sessionManager.endSession(loc);
                    plugin.getLogger().severe("Failed to check IP limit for " + player.getName() + ": " + dbError.getMessage());
                    return;
                }
                
                if (!allowed) {
                    sessionManager.endSession(loc);
                    messageManager.sendMessage(player, com.pumpkings.pkcrates.infrastructure.config.Messages.IP_LIMIT_REACHED);
                    return;
                }
                
                tryConsumeAnyKey(player, acceptedKeys, 0, crateId).whenCompleteAsync((usedKey, error) -> {
                    if (error != null) {
                        // Release the tentative lock, otherwise the crate stays "in use" forever.
                        sessionManager.endSession(loc);
                        plugin.getLogger().severe("Failed to consume a key for crate '" + crateId
                                + "' (player: " + player.getName() + "): " + error.getMessage());
                        return;
                    }

            if (usedKey == null) {
                sessionManager.endSession(loc);
                messageManager.sendMessage(player, Messages.KEY_MISSING);
                return;
            }

            // Generate reward
            com.pumpkings.pkcrates.api.rarity.RarityService rarityService = ((com.pumpkings.pkcrates.PkCratesPlugin) plugin).getRarityService();
            com.pumpkings.pkcrates.core.model.reward.IReward wonReward = com.pumpkings.pkcrates.core.service.RewardsGenerator.generateReward(crate, usedKey, rarityService);

            if (wonReward == null) {
                sessionManager.endSession(loc);
                messageManager.sendMessage(player, Messages.CRATE_NO_REWARDS);
                return;
            }

            // Start Opening Session
            com.pumpkings.pkcrates.core.model.session.CrateSession session = new com.pumpkings.pkcrates.core.model.session.CrateSession(player, crate, loc, wonReward);
            sessionManager.startSession(loc, session);

            messageManager.sendMessage(player, Messages.CRATE_OPENING, "<crate>", crate.getName());

            // Opening effects come from config: the crate's own 'effects.on-open' when it
            // defines one, otherwise the global bundle in config.yml.
            com.pumpkings.pkcrates.core.effect.EffectEngine effects = pkPlugin.getEffectEngine();
            org.bukkit.Location effectOrigin = loc.clone().add(0.5, 1.0, 0.5);
            effects.play(
                    com.pumpkings.pkcrates.core.effect.EffectTrigger.ON_OPEN,
                    crate.getEffects(com.pumpkings.pkcrates.core.effect.EffectTrigger.ON_OPEN, effects::compile),
                    effectOrigin, player);

            messageManager.showTitle(player, Messages.CRATE_OPENING_TITLE, Messages.CRATE_OPENING_SUBTITLE,
                    java.util.Map.of("<crate>", crate.getName()));

            // Get and play animation
            String animId = crate.getAnimationId() != null ? crate.getAnimationId() : "ROULETTE";
            com.pumpkings.pkcrates.core.animation.AnimationPhase animation = animationRegistry.createPhase(animId);
            if (animation != null) {
                tickTask.playAnimation(session, animation);
            } else {
                // If there is no registered animation, finish the session instantly
                session.setFinished(true);
            }
        }, mainThread);
        }, mainThread);
    }
    
    @EventHandler
    public void onCrateBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        String crateId = locationMgr.getCrateAt(block.getLocation());
        
        if (crateId != null) {
            event.setCancelled(true);
            messageManager.sendMessage(event.getPlayer(), Messages.CRATE_CANNOT_BREAK);
        }
    }

    private CompletableFuture<IKey> tryConsumeAnyKey(Player player, List<String> keys, int index, String crateId) {
        if (index >= keys.size()) {
            return CompletableFuture.completedFuture(null);
        }
        
        String keyId = keys.get(index);
        IKey key = keyRegistry.getKey(keyId);
        
        if (key == null) {
            return tryConsumeAnyKey(player, keys, index + 1, crateId);
        }

        // Virtual keys resolve on a database thread. Hopping back to the main thread here
        // is mandatory: the next step may consume a *physical* key from a crate that mixes
        // both kinds, and inventory mutation is main-thread only.
        return keyService.hasKey(player, key).thenComposeAsync(hasIt -> {
            if (hasIt) {
                return keyService.consumeKey(player, key).thenComposeAsync(consumed -> {
                    if (consumed) {

                        com.pumpkings.pkcrates.infrastructure.audit.api.AuditService audit =
                            ((com.pumpkings.pkcrates.PkCratesPlugin) plugin).getAuditService();
                        audit.info(com.pumpkings.pkcrates.infrastructure.audit.api.AuditEvent.KEY_USED, player.getName(), keyId,
                            Map.of("crate", crateId));

                        return CompletableFuture.completedFuture(key);
                    }
                    return tryConsumeAnyKey(player, keys, index + 1, crateId);
                }, mainThread);
            } else {
                return tryConsumeAnyKey(player, keys, index + 1, crateId);
            }
        }, mainThread);
    }
}

