package com.example.plugin.bootstrap;

import com.example.plugin.config.RegistryManager;
import com.example.plugin.database.DatabaseManager;
import com.example.plugin.database.SaveQueueService;
import com.example.plugin.economy.repository.PlayerRepository;
import com.example.plugin.economy.service.EconomyService;
import com.example.plugin.economy.service.EconomyServiceImpl;
import com.example.plugin.territory.repository.TerritoryRepository;
import com.example.plugin.territory.service.TerritoryService;
import com.example.plugin.territory.service.TerritoryServiceImpl;
import com.example.plugin.protection.BlockBreakPlaceListener;
import com.example.plugin.protection.EntityInteractListener;
import com.example.plugin.protection.EnvironmentalProtectionListener;
import com.example.plugin.protection.ProtectionService;
import com.example.plugin.onboarding.service.OnboardingService;
import com.example.plugin.onboarding.service.OnboardingServiceImpl;
import com.example.plugin.node.repository.NodeRepository;
import com.example.plugin.node.service.NodeService;
import com.example.plugin.node.service.NodeServiceImpl;
import com.example.plugin.worker.repository.WorkerRepository;
import com.example.plugin.worker.service.WorkerService;
import com.example.plugin.worker.service.WorkerServiceImpl;
import com.example.plugin.storage.repository.StorageRepository;
import com.example.plugin.storage.repository.WalletRepository;
import com.example.plugin.storage.service.StorageService;
import com.example.plugin.storage.service.StorageServiceImpl;
import com.example.plugin.exploration.repository.ExplorationRepository;
import com.example.plugin.exploration.service.ExplorationService;
import com.example.plugin.exploration.service.ExplorationServiceImpl;
import com.example.plugin.production.listener.PlayerJoinOfflineSyncListener;
import com.example.plugin.production.service.ProductionService;
import com.example.plugin.production.service.ProductionServiceImpl;
import com.example.plugin.production.task.ProductionTickerTask;
import com.example.plugin.command.IdleCommand;
import com.example.plugin.gui.GUIController;
import com.example.plugin.integration.vault.VaultBridge;
import com.example.plugin.visual.service.VisualService;
import com.example.plugin.visual.service.VisualServiceImpl;
import com.example.plugin.onboarding.listener.OnboardingCompletedListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main JavaPlugin bootstrap entry point for Branz.Idle.
 * Orchestrates initialization of database pools, config registries, and service locators.
 */
public final class BranzIdlePlugin extends JavaPlugin {

    private DatabaseManager databaseManager;
    private SaveQueueService saveQueueService;
    private RegistryManager registryManager;
    private ProviderManager providerManager;
    private ServiceRegistry serviceRegistry;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        getLogger().info("==========================================");
        getLogger().info("Initializing Branz.Idle MMORPG Framework");
        getLogger().info("==========================================");

        // 1. Save default config files
        saveDefaultConfig();

        // 2. Detect Soft-Dependency Providers
        this.providerManager = new ProviderManager(this);
        providerManager.detectProviders();

        // 3. Initialize Database Manager & DDL Schemas
        this.databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        // 4. Start Asynchronous Save Queue
        this.saveQueueService = new SaveQueueService(this);
        saveQueueService.start();

        // 5. Load Data Registries (YAML configs)
        this.registryManager = new RegistryManager(this);
        registryManager.loadAll();

        // 6. Initialize Service Registry & Wiring
        this.serviceRegistry = new ServiceRegistry(
            this,
            databaseManager,
            saveQueueService,
            registryManager,
            providerManager
        );

        // 7. Initialize & Wire Domain Services
        PlayerRepository playerRepository = new PlayerRepository(this, databaseManager);
        EconomyService economyService = new EconomyServiceImpl(this, playerRepository, saveQueueService);
        serviceRegistry.registerService(EconomyService.class, economyService);

        VaultBridge vaultBridge = new VaultBridge(this, providerManager, economyService);
        vaultBridge.initialize();

        TerritoryRepository territoryRepository = new TerritoryRepository(this, databaseManager);
        TerritoryService territoryService = new TerritoryServiceImpl(this, territoryRepository, saveQueueService);
        territoryService.initialize();
        serviceRegistry.registerService(TerritoryService.class, territoryService);

        ProtectionService protectionService = new ProtectionService(this, territoryService);
        serviceRegistry.registerService(ProtectionService.class, protectionService);

        OnboardingService onboardingService = new OnboardingServiceImpl(this, economyService, territoryService);
        serviceRegistry.registerService(OnboardingService.class, onboardingService);

        NodeRepository nodeRepository = new NodeRepository(this, databaseManager);
        NodeService nodeService = new NodeServiceImpl(this, nodeRepository, saveQueueService, registryManager, economyService, territoryService);
        nodeService.initialize();
        serviceRegistry.registerService(NodeService.class, nodeService);

        WorkerRepository workerRepository = new WorkerRepository(this, databaseManager);
        WorkerService workerService = new WorkerServiceImpl(this, workerRepository, saveQueueService, registryManager, nodeService);
        workerService.initialize();
        serviceRegistry.registerService(WorkerService.class, workerService);

        StorageRepository storageRepository = new StorageRepository(this, databaseManager);
        WalletRepository walletRepository = new WalletRepository(this, databaseManager);
        StorageService storageService = new StorageServiceImpl(this, storageRepository, walletRepository, saveQueueService, nodeService);
        storageService.initialize();
        serviceRegistry.registerService(StorageService.class, storageService);

        ExplorationRepository explorationRepository = new ExplorationRepository(this, databaseManager);
        ExplorationService explorationService = new ExplorationServiceImpl(this, explorationRepository, saveQueueService, registryManager);
        explorationService.initialize();
        serviceRegistry.registerService(ExplorationService.class, explorationService);

        ProductionService productionService = new ProductionServiceImpl(this, nodeService, workerService, storageService, explorationService, registryManager);
        serviceRegistry.registerService(ProductionService.class, productionService);

        VisualService visualService = new VisualServiceImpl(this, providerManager);
        visualService.initialize();
        serviceRegistry.registerService(VisualService.class, visualService);

        // 8. Schedule Production Ticker Task (100 ticks = 5 seconds)
        Bukkit.getScheduler().runTaskTimer(this, new ProductionTickerTask(productionService), 100L, 100L);

        // 9. Register Protection, Sync, and GUI Listeners
        Bukkit.getPluginManager().registerEvents(new BlockBreakPlaceListener(protectionService), this);
        Bukkit.getPluginManager().registerEvents(new EntityInteractListener(protectionService), this);
        Bukkit.getPluginManager().registerEvents(new EnvironmentalProtectionListener(protectionService), this);
        Bukkit.getPluginManager().registerEvents(new PlayerJoinOfflineSyncListener(this, productionService), this);
        Bukkit.getPluginManager().registerEvents(new GUIController(), this);
        Bukkit.getPluginManager().registerEvents(new OnboardingCompletedListener(nodeService, workerService), this);

        // 10. Register Commands
        IdleCommand idleCommand = new IdleCommand(
            this,
            territoryService,
            nodeService,
            workerService,
            storageService,
            economyService,
            onboardingService,
            registryManager,
            visualService
        );
        if (getCommand("idle") != null) {
            getCommand("idle").setExecutor(idleCommand);
            getCommand("idle").setTabCompleter(idleCommand);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        getLogger().info(String.format("Branz.Idle successfully enabled in %d ms!", elapsed));
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down Branz.Idle...");

        // 1. Clear spawned visual NPCs
        if (serviceRegistry != null) {
            serviceRegistry.getService(VisualService.class).ifPresent(VisualService::clearAllVisuals);
            serviceRegistry.getService(EconomyService.class).ifPresent(EconomyService::flushAll);
        }

        // 2. Stop and flush save queue tasks synchronously
        if (saveQueueService != null) {
            saveQueueService.shutdown();
        }

        // 3. Close database connection pool
        if (databaseManager != null) {
            databaseManager.shutdown();
        }

        getLogger().info("Branz.Idle has been successfully disabled.");
    }

    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }
}
