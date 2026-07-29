package com.pumpkings.pkcrates;

import org.bukkit.plugin.java.JavaPlugin;

public class PkCratesPlugin extends JavaPlugin {

    private com.pumpkings.pkcrates.infrastructure.config.ConfigManager configManager;
    private com.pumpkings.pkcrates.infrastructure.config.CrateRegistry crateRegistry;
    private com.pumpkings.pkcrates.infrastructure.key.KeyRegistry keyRegistry;
    private com.pumpkings.pkcrates.infrastructure.location.CrateLocationManager locationMgr;
    private com.pumpkings.pkcrates.core.service.KeyService keyService;
    private com.pumpkings.pkcrates.presentation.menu.MenuManager menuManager;
    private com.pumpkings.pkcrates.infrastructure.database.DatabaseManager databaseManager;
    private com.pumpkings.pkcrates.core.animation.AnimationRegistry animationRegistry;
    private com.pumpkings.pkcrates.infrastructure.config.MessageManager messageManager;
    private com.pumpkings.pkcrates.infrastructure.config.MenuConfigManager menuConfigManager;
    private com.pumpkings.pkcrates.infrastructure.config.RarityRegistry rarityRegistry;
    private com.pumpkings.pkcrates.infrastructure.config.RarityConfig rarityConfig;
    private com.pumpkings.pkcrates.api.rarity.RarityService rarityService;
    private com.pumpkings.pkcrates.core.service.ClaimService claimService;
    private com.pumpkings.pkcrates.infrastructure.claim.ClaimConfig claimConfig;
    private com.pumpkings.pkcrates.infrastructure.audit.impl.AuditServiceImpl auditService;
    private com.pumpkings.pkcrates.infrastructure.audit.impl.AuditRetryWorker auditRetryWorker;
    private com.pumpkings.pkcrates.infrastructure.config.MassOpeningGlobalSettings massOpeningGlobalSettings;
    private com.pumpkings.pkcrates.core.task.MassOpeningQueue massOpeningQueue;
    private com.pumpkings.pkcrates.core.service.MassOpeningService massOpeningService;

    @Override
    public void onEnable() {
        getLogger().info("PkCrates has been enabled! Running on Paper 1.21+ / Java 21");

        // 0. Audit System (Load first so other systems can use it if needed)
        com.pumpkings.pkcrates.infrastructure.audit.config.LoggerConfig loggerConfig = 
                new com.pumpkings.pkcrates.infrastructure.audit.config.LoggerConfig(this);
        loggerConfig.load();
        
        auditService = new com.pumpkings.pkcrates.infrastructure.audit.impl.AuditServiceImpl(this, loggerConfig);
        auditService.registerSink(new com.pumpkings.pkcrates.infrastructure.audit.sink.ConsoleAuditSink(this, loggerConfig));
        auditService.registerSink(new com.pumpkings.pkcrates.infrastructure.audit.sink.FileAuditSink(this, loggerConfig));
        
        auditRetryWorker = new com.pumpkings.pkcrates.infrastructure.audit.impl.AuditRetryWorker(auditService.getRegisteredSinks(), this);
        if (loggerConfig.isDiscordEnabled() && loggerConfig.isDiscordRetryEnabled()) {
            auditRetryWorker.start();
        }
        
        auditService.registerSink(new com.pumpkings.pkcrates.infrastructure.audit.sink.DiscordWebhookAuditSink(this, loggerConfig, auditRetryWorker));
        auditService.start();
        
        getServer().getServicesManager().register(com.pumpkings.pkcrates.infrastructure.audit.api.AuditService.class, 
                auditService, this, org.bukkit.plugin.ServicePriority.Normal);
                
        auditService.info(com.pumpkings.pkcrates.infrastructure.audit.api.AuditEvent.PLUGIN_ENABLED, 
                "SYSTEM", "PkCrates", null);

        // 1. Configuraciones
        configManager = new com.pumpkings.pkcrates.infrastructure.config.ConfigManager(this);
        configManager.load();

        messageManager = new com.pumpkings.pkcrates.infrastructure.config.MessageManager(this);
        messageManager.loadMessages();

        menuConfigManager = new com.pumpkings.pkcrates.infrastructure.config.MenuConfigManager(this);
        menuConfigManager.loadAll();

        rarityRegistry = new com.pumpkings.pkcrates.infrastructure.config.RarityRegistry(this);
        rarityConfig = new com.pumpkings.pkcrates.infrastructure.config.RarityConfig(this, rarityRegistry);
        rarityConfig.load();
        
        rarityService = new com.pumpkings.pkcrates.core.service.RarityServiceImpl(this, rarityRegistry, rarityConfig);
        getServer().getServicesManager().register(com.pumpkings.pkcrates.api.rarity.RarityService.class, rarityService, this, org.bukkit.plugin.ServicePriority.Normal);

        crateRegistry = new com.pumpkings.pkcrates.infrastructure.config.CrateRegistry(this);
        crateRegistry.loadAll();

        keyRegistry = new com.pumpkings.pkcrates.infrastructure.key.KeyRegistry(this);
        keyRegistry.loadAll();

        locationMgr = new com.pumpkings.pkcrates.infrastructure.location.CrateLocationManager(this);
        locationMgr.load();

        // 2. Servicios
        databaseManager = new com.pumpkings.pkcrates.infrastructure.database.DatabaseManager(this);
        databaseManager.connect();
        
        keyService = new com.pumpkings.pkcrates.core.service.KeyService(this, databaseManager);
        menuManager = new com.pumpkings.pkcrates.presentation.menu.MenuManager(menuConfigManager);
        com.pumpkings.pkcrates.infrastructure.display.HologramManager hologramManager = new com.pumpkings.pkcrates.infrastructure.display.HologramManager(this, locationMgr, crateRegistry);

        // Claim module
        claimConfig = new com.pumpkings.pkcrates.infrastructure.claim.YamlClaimConfig(this);
        com.pumpkings.pkcrates.infrastructure.claim.ClaimRepository claimRepository =
                new com.pumpkings.pkcrates.infrastructure.claim.YamlClaimRepository(this);
        claimService = new com.pumpkings.pkcrates.core.service.ClaimServiceImpl(this, claimRepository, claimConfig);

        // 3. Listeners
        com.pumpkings.pkcrates.presentation.listener.ChatPromptManager promptManager = new com.pumpkings.pkcrates.presentation.listener.ChatPromptManager(this, messageManager);
        menuManager.setPromptManager(promptManager);
        
        com.pumpkings.pkcrates.core.service.SessionManager sessionManager = new com.pumpkings.pkcrates.core.service.SessionManager();

        // Mass Opening module
        massOpeningGlobalSettings = new com.pumpkings.pkcrates.infrastructure.config.MassOpeningGlobalSettings();
        massOpeningGlobalSettings.load(configManager.getConfig());

        massOpeningQueue = new com.pumpkings.pkcrates.core.task.MassOpeningQueue(this, claimService, claimConfig, messageManager, massOpeningGlobalSettings.getRewardsPerTick());
        massOpeningQueue.runTaskTimer(this, 1L, 1L);

        massOpeningService = new com.pumpkings.pkcrates.core.service.MassOpeningServiceImpl(
                this, keyService, keyRegistry, sessionManager, messageManager, massOpeningGlobalSettings, massOpeningQueue
        );
        getServer().getServicesManager().register(com.pumpkings.pkcrates.core.service.MassOpeningService.class, massOpeningService, this, org.bukkit.plugin.ServicePriority.Normal);
        
        animationRegistry = new com.pumpkings.pkcrates.core.animation.AnimationRegistry();
        animationRegistry.register("ROULETTE", com.pumpkings.pkcrates.core.animation.impl.RouletteAnimation::new);
        animationRegistry.register("CSGO", com.pumpkings.pkcrates.core.animation.impl.CSGOAnimation::new);
        animationRegistry.register("FOUNTAIN", com.pumpkings.pkcrates.core.animation.impl.FountainAnimation::new);
        animationRegistry.register("SPIRAL", com.pumpkings.pkcrates.core.animation.impl.SpiralAnimation::new);
        animationRegistry.register("BLACKHOLE", com.pumpkings.pkcrates.core.animation.impl.BlackholeAnimation::new);
        animationRegistry.register("METEOR", com.pumpkings.pkcrates.core.animation.impl.MeteorAnimation::new);
        animationRegistry.register("PORTAL", com.pumpkings.pkcrates.core.animation.impl.PortalAnimation::new);
        
        com.pumpkings.pkcrates.core.task.CrateTickTask tickTask = new com.pumpkings.pkcrates.core.task.CrateTickTask(this, sessionManager, claimService, claimConfig, messageManager);
        tickTask.runTaskTimer(this, 1L, 1L);
        
        getServer().getPluginManager().registerEvents(promptManager, this);
        com.pumpkings.pkcrates.presentation.listener.CrateInteractListener crateInteractListener =
                new com.pumpkings.pkcrates.presentation.listener.CrateInteractListener(this, locationMgr, crateRegistry, keyService, keyRegistry, menuManager, sessionManager, animationRegistry, tickTask, messageManager);
        crateInteractListener.setMassOpeningService(massOpeningService, massOpeningGlobalSettings);
        getServer().getPluginManager().registerEvents(crateInteractListener, this);
        getServer().getPluginManager().registerEvents(new com.pumpkings.pkcrates.presentation.listener.MenuListener(menuManager), this);
        getServer().getPluginManager().registerEvents(hologramManager, this);
        getServer().getPluginManager().registerEvents(new com.pumpkings.pkcrates.presentation.listener.ClaimLoginListener(this, claimService, claimConfig, messageManager), this);

        // 4. Comandos (Usando la nueva API)
        this.getLifecycleManager().registerEventHandler(io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents.COMMANDS, event ->
            com.pumpkings.pkcrates.presentation.command.CommandRegistry.register(
                event, this, crateRegistry, keyRegistry, locationMgr, keyService,
                menuManager, hologramManager, messageManager, claimService, claimConfig, databaseManager)
        );
        
        // Guardamos la referencia para el onDisable
        this.hologramManager = hologramManager;
        
        org.bukkit.command.ConsoleCommandSender console = getServer().getConsoleSender();
        console.sendRichMessage("<gold>                                                 ");
        console.sendRichMessage("<gold>  ▄▄▄▄▄▄       ▄   ▄▄▄▄                          ");
        console.sendRichMessage("<gold> █▀██▀▀▀█▄     ▀██████▀            █▄            ");
        console.sendRichMessage("<gold>   ██▄▄▄█▀▄▄     ██     ▄         ▄██▄           ");
        console.sendRichMessage("<gold>   ██▀▀▀  ██ ▄█▀ ██     ████▄▄▀▀█▄ ██ ▄█▀█▄ ▄██▀█");
        console.sendRichMessage("<gold> ▄ ██     ████   ██     ██   ▄█▀██ ██ ██▄█▀ ▀███▄");
        console.sendRichMessage("<gold> ▀██▀    ▄██ ▀█▄ ▀█████▄█▀  ▄▀█▄██▄██▄▀█▄▄▄█▄▄██▀");
        console.sendRichMessage("<gold>                                                 ");
        console.sendRichMessage("");
        console.sendRichMessage("<white>Crates Loaded: <aqua>" + crateRegistry.getAllCrates().size() + "</aqua></white>");
        console.sendRichMessage("<white>Keys Loaded: <aqua>" + keyRegistry.getAllKeys().size() + "</aqua></white>");
        console.sendRichMessage("");
    }
    
    private com.pumpkings.pkcrates.infrastructure.display.HologramManager hologramManager;

    @Override
    public void onDisable() {
        if (auditService != null) {
            auditService.info(com.pumpkings.pkcrates.infrastructure.audit.api.AuditEvent.PLUGIN_DISABLED, "SYSTEM", "PkCrates", null);
            auditService.shutdown();
        }
        if (auditRetryWorker != null) {
            auditRetryWorker.interrupt();
        }
        if (hologramManager != null) {
            hologramManager.removeAll();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("PkCrates has been disabled.");
    }
    
    public void reloadPlugin() {
        configManager.load();
        messageManager.loadMessages();
        menuConfigManager.loadAll();
        rarityConfig.load();
        crateRegistry.loadAll();
        keyRegistry.loadAll();
        locationMgr.load();
        
        if (hologramManager != null) {
            hologramManager.removeAll();
            for (org.bukkit.World world : getServer().getWorlds()) {
                for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                    org.bukkit.event.world.ChunkLoadEvent event = new org.bukkit.event.world.ChunkLoadEvent(chunk, false);
                    hologramManager.onChunkLoad(event);
                }
            }
        }
    }
    
    public com.pumpkings.pkcrates.core.animation.AnimationRegistry getAnimationRegistry() {
        return animationRegistry;
    }
    
    public com.pumpkings.pkcrates.infrastructure.audit.api.AuditService getAuditService() {
        return auditService;
    }
    
    public com.pumpkings.pkcrates.infrastructure.config.RarityRegistry getRarityRegistry() {
        return rarityRegistry;
    }
    
    public com.pumpkings.pkcrates.infrastructure.config.RarityConfig getRarityConfig() {
        return rarityConfig;
    }
    
    public com.pumpkings.pkcrates.api.rarity.RarityService getRarityService() {
        return rarityService;
    }
    
    public com.pumpkings.pkcrates.infrastructure.config.MessageManager getMessageManager() {
        return messageManager;
    }

    public com.pumpkings.pkcrates.core.service.MassOpeningService getMassOpeningService() {
        return massOpeningService;
    }

    public com.pumpkings.pkcrates.infrastructure.config.MassOpeningGlobalSettings getMassOpeningGlobalSettings() {
        return massOpeningGlobalSettings;
    }
}
