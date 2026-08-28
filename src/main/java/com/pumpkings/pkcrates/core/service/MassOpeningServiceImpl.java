package com.pumpkings.pkcrates.core.service;

import com.pumpkings.pkcrates.api.rarity.RarityService;
import com.pumpkings.pkcrates.core.event.MassOpeningCancelEvent;
import com.pumpkings.pkcrates.core.event.MassOpeningCompleteEvent;
import com.pumpkings.pkcrates.core.event.MassOpeningStartEvent;
import com.pumpkings.pkcrates.core.model.Crate;
import com.pumpkings.pkcrates.core.model.key.IKey;
import com.pumpkings.pkcrates.core.model.massopening.MassOpeningOption;
import com.pumpkings.pkcrates.core.model.reward.IReward;
import com.pumpkings.pkcrates.core.task.MassOpeningQueue;
import com.pumpkings.pkcrates.core.task.PendingMassOpening;
import com.pumpkings.pkcrates.infrastructure.config.MassOpeningConfig;
import com.pumpkings.pkcrates.infrastructure.config.MassOpeningGlobalSettings;
import com.pumpkings.pkcrates.infrastructure.config.MessageManager;
import com.pumpkings.pkcrates.infrastructure.config.Messages;
import com.pumpkings.pkcrates.infrastructure.key.KeyRegistry;
import com.pumpkings.pkcrates.infrastructure.permission.PermissionLimitResolver;
import com.pumpkings.pkcrates.infrastructure.scheduler.MainThreadExecutor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class MassOpeningServiceImpl implements MassOpeningService {

    private final Plugin plugin;
    private final KeyService keyService;
    private final KeyRegistry keyRegistry;
    private final SessionManager sessionManager;
    private final MessageManager messageManager;
    private final MassOpeningGlobalSettings globalSettings;
    private final MassOpeningQueue massOpeningQueue;
    private final RarityService rarityService;

    /**
     * Every continuation chained onto a key-lookup future runs through this executor.
     * Key lookups for virtual keys complete on a database thread, and the continuations
     * below dispatch Bukkit events and mutate player inventories — both of which are
     * main-thread-only operations.
     */
    private final Executor mainThread;

    private final Set<UUID> activeMassOpenings = ConcurrentHashMap.newKeySet();

    public MassOpeningServiceImpl(Plugin plugin, KeyService keyService, KeyRegistry keyRegistry, SessionManager sessionManager,
                                  MessageManager messageManager, MassOpeningGlobalSettings globalSettings,
                                  MassOpeningQueue massOpeningQueue, RarityService rarityService) {
        this.plugin = plugin;
        this.keyService = keyService;
        this.keyRegistry = keyRegistry;
        this.sessionManager = sessionManager;
        this.messageManager = messageManager;
        this.globalSettings = globalSettings;
        this.massOpeningQueue = massOpeningQueue;
        this.rarityService = rarityService;
        this.mainThread = new MainThreadExecutor(plugin);
    }

    @Override
    public CompletableFuture<CanOpenResult> canMassOpen(Player player, Crate crate, int amount) {
        if (!globalSettings.isEnabled() || (crate.getMassOpeningConfig() != null && !crate.getMassOpeningConfig().isEnabled())) {
            return CompletableFuture.completedFuture(CanOpenResult.denied(CanOpenResult.Reason.MASS_OPENING_DISABLED, Messages.MASS_OPENING_DISABLED));
        }

        if (!player.hasPermission("pkcrates.massopen") && !player.hasPermission("pkcrates.open." + crate.getId())) {
            return CompletableFuture.completedFuture(CanOpenResult.denied(CanOpenResult.Reason.NO_PERMISSION, Messages.MASS_OPENING_NO_PERMISSION));
        }

        if (isPlayerInSession(player) || isMassOpeningInProgress(player)) {
            return CompletableFuture.completedFuture(CanOpenResult.denied(CanOpenResult.Reason.CRATE_BUSY, Messages.CRATE_ALREADY_IN_USE));
        }

        int maxAllowed = resolveMaxAllowed(player, crate);
        if (amount != MassOpeningOption.ALL_AMOUNT && amount > maxAllowed) {
            return CompletableFuture.completedFuture(CanOpenResult.denied(CanOpenResult.Reason.NO_PERMISSION, Messages.MASS_OPENING_LIMIT_REACHED));
        }

        if (crate.getAcceptedKeys().isEmpty()) {
            return CompletableFuture.completedFuture(CanOpenResult.denied(CanOpenResult.Reason.NOT_ENOUGH_KEYS, Messages.MASS_OPENING_NOT_ENOUGH_KEYS));
        }

        String keyId = crate.getAcceptedKeys().get(0);
        IKey key = keyRegistry.getKey(keyId);
        if (key == null) {
            return CompletableFuture.completedFuture(CanOpenResult.denied(CanOpenResult.Reason.NOT_ENOUGH_KEYS, Messages.MASS_OPENING_NOT_ENOUGH_KEYS));
        }

        return keyService.countKeys(player, key).thenCompose(count -> {
            int required = (amount == MassOpeningOption.ALL_AMOUNT) ? 1 : amount;
            if (count < required) {
                return CompletableFuture.completedFuture(CanOpenResult.denied(CanOpenResult.Reason.NOT_ENOUGH_KEYS, Messages.MASS_OPENING_NOT_ENOUGH_KEYS));
            }
            
            com.pumpkings.pkcrates.PkCratesPlugin pkPlugin = (com.pumpkings.pkcrates.PkCratesPlugin) plugin;
            String playerIp = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "";
            
            return pkPlugin.getDatabaseManager().checkAndRegisterIpLimit(playerIp, crate.getId(), player.getUniqueId(), crate.getIpLimit())
                .thenApply(allowed -> {
                    if (!allowed) {
                        return CanOpenResult.denied(CanOpenResult.Reason.NO_PERMISSION, com.pumpkings.pkcrates.infrastructure.config.Messages.IP_LIMIT_REACHED);
                    }
                    return CanOpenResult.success();
                });
        });
    }

    @Override
    public CompletableFuture<MassOpeningResult> startMassOpening(Player player, Crate crate, int amount) {
        long startMillis = System.currentTimeMillis();

        if (crate.getAcceptedKeys().isEmpty()) {
            return CompletableFuture.completedFuture(new MassOpeningResult(amount, 0, List.of(), 0, true, "No accepted keys"));
        }

        String keyId = crate.getAcceptedKeys().get(0);
        IKey key = keyRegistry.getKey(keyId);
        if (key == null) {
            return CompletableFuture.completedFuture(new MassOpeningResult(amount, 0, List.of(), 0, true, "Invalid key"));
        }

        return keyService.countKeys(player, key).thenComposeAsync(count -> {
            int maxAllowed = resolveMaxAllowed(player, crate);
            int effectiveAmount;
            if (amount == MassOpeningOption.ALL_AMOUNT) {
                effectiveAmount = (int) Math.min(count, (long) maxAllowed);
            } else {
                effectiveAmount = amount;
            }

            if (effectiveAmount <= 0) {
                messageManager.sendMessage(player, Messages.MASS_OPENING_NOT_ENOUGH_KEYS);
                return CompletableFuture.completedFuture(new MassOpeningResult(amount, 0, List.of(), 0, true, "Not enough keys"));
            }

            MassOpeningStartEvent startEvent = new MassOpeningStartEvent(player, crate, amount, effectiveAmount);
            Bukkit.getPluginManager().callEvent(startEvent);

            if (startEvent.isCancelled()) {
                return CompletableFuture.completedFuture(new MassOpeningResult(amount, 0, List.of(), 0, true, "Cancelled by event"));
            }

            int finalAmount = startEvent.getEffectiveAmount();
            activeMassOpenings.add(player.getUniqueId());

            return keyService.consumeKeys(player, key, finalAmount).thenComposeAsync(consumed -> {
                if (!consumed) {
                    activeMassOpenings.remove(player.getUniqueId());
                    messageManager.sendMessage(player, Messages.MASS_OPENING_NOT_ENOUGH_KEYS);
                    return CompletableFuture.completedFuture(new MassOpeningResult(amount, 0, List.of(), 0, true, "Failed to consume keys"));
                }

                messageManager.sendMessage(player, Messages.MASS_OPENING_STARTED,
                        Map.of("<count>", String.valueOf(finalAmount), "<crate>", crate.getName()));

                CompletableFuture<List<IReward>> generationFuture = new CompletableFuture<>();
                if (finalAmount >= globalSettings.getAsyncGenerationThreshold()) {
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        List<IReward> rewards = generateRewards(crate, key, finalAmount);
                        Bukkit.getScheduler().runTask(plugin, () -> generationFuture.complete(rewards));
                    });
                } else {
                    generationFuture.complete(generateRewards(crate, key, finalAmount));
                }

                return generationFuture.thenApplyAsync(rewards -> {
                    long durationMillis = System.currentTimeMillis() - startMillis;
                    MassOpeningCompleteEvent completeEvent = new MassOpeningCompleteEvent(player, crate, finalAmount, rewards, durationMillis);
                    Bukkit.getPluginManager().callEvent(completeEvent);

                    PendingMassOpening pending = new PendingMassOpening(player, crate, rewards);
                    massOpeningQueue.addPending(pending);

                    return new MassOpeningResult(amount, finalAmount, rewards, durationMillis, false, null);
                }, mainThread);
            }, mainThread).whenComplete((result, error) -> {
                // Release the in-progress lock on every exit path, including exceptional ones.
                activeMassOpenings.remove(player.getUniqueId());
                if (error != null) {
                    plugin.getLogger().severe("Mass opening failed for '" + player.getName()
                            + "' on crate '" + crate.getId() + "': " + error.getMessage());
                }
            });
        }, mainThread);
    }

    /**
     * Generates {@code amount} rewards honouring the rarity restrictions carried by the
     * key that was consumed, exactly as a single opening does.
     *
     * <p>Delegating to {@link RewardsGenerator} keeps mass opening and single opening on
     * the same roll logic — a raw {@code crate.pickRandomReward()} would ignore both the
     * key's {@code RarityRestriction} and {@code RarityChanceMode.SYNCED} weighting.</p>
     */
    private List<IReward> generateRewards(Crate crate, IKey usedKey, int amount) {
        List<IReward> rewards = new ArrayList<>(amount);
        for (int i = 0; i < amount; i++) {
            IReward reward = RewardsGenerator.generateReward(crate, usedKey, rarityService);
            if (reward != null) {
                rewards.add(reward);
            }
        }
        return rewards;
    }

    @Override
    public List<MassOpeningOption> getAvailableOptions(Player player, Crate crate, int userKeys) {
        List<MassOpeningOption> allOptions = crate.getMassOpeningConfig() != null
                ? crate.getMassOpeningConfig().getOptions()
                : MassOpeningConfig.createDefault(true).getOptions();

        int maxAllowed = resolveMaxAllowed(player, crate);
        List<MassOpeningOption> available = new ArrayList<>();

        for (MassOpeningOption option : allOptions) {
            if (option.isAll()) {
                available.add(option);
            } else if (option.getAmount() <= maxAllowed) {
                available.add(option);
            }
        }
        return available;
    }

    @Override
    public int resolveMaxAllowed(Player player, Crate crate) {
        if (player.hasPermission("pkcrates.massopen.unlimited")) {
            return Integer.MAX_VALUE;
        }
        PermissionLimitResolver resolver = PermissionLimitResolver.builder("pkcrates.massopen.limit.")
                .defaultLimit(globalSettings.getDefaultLimit())
                .build();
        int res = resolver.resolve(player);
        return res == PermissionLimitResolver.UNLIMITED ? Integer.MAX_VALUE : res;
    }

    @Override
    public boolean isMassOpeningInProgress(Player player) {
        return player != null && activeMassOpenings.contains(player.getUniqueId());
    }

    @Override
    public void cancelMassOpening(Player player, String reason) {
        if (player == null) return;
        activeMassOpenings.remove(player.getUniqueId());
        Bukkit.getPluginManager().callEvent(new MassOpeningCancelEvent(player, null, 0, reason));
    }

    private boolean isPlayerInSession(Player player) {
        return sessionManager.getActiveSessions().stream()
                .anyMatch(s -> s.getPlayer().getUniqueId().equals(player.getUniqueId()));
    }
}
