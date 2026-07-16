package com.example.plugin.worker.service;

import com.example.plugin.config.NodeRegistry;
import com.example.plugin.config.RegistryManager;
import com.example.plugin.config.WorkerRegistry;
import com.example.plugin.database.SaveQueueService;
import com.example.plugin.node.model.ProductionNode;
import com.example.plugin.node.service.NodeService;
import com.example.plugin.worker.biography.WorkerBiographyService;
import com.example.plugin.worker.biography.WorkerBiographyServiceImpl;
import com.example.plugin.worker.factory.WorkerFactory;
import com.example.plugin.worker.fusion.WorkerFusionService;
import com.example.plugin.worker.fusion.WorkerFusionServiceImpl;
import com.example.plugin.worker.model.WorkerInstance;
import com.example.plugin.worker.model.WorkerStats;
import com.example.plugin.worker.repository.WorkerRepository;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of WorkerService managing memory caching, periodic database flushing, and sub-service delegates.
 */
public class WorkerServiceImpl implements WorkerService, org.bukkit.event.Listener {

    private final JavaPlugin plugin;
    private final WorkerRepository repository;
    private final SaveQueueService saveQueue;
    private final RegistryManager registryManager;
    private final NodeService nodeService;
    
    // Memory caches
    private final Map<UUID, WorkerInstance> workerCache = new ConcurrentHashMap<>();
    private final Set<UUID> dirtyWorkers = ConcurrentHashMap.newKeySet();

    // Delegate Services
    private final WorkerBiographyService biographyService;
    private final WorkerFactory workerFactory;
    private final WorkerFusionService fusionService;

    public WorkerServiceImpl(
        JavaPlugin plugin,
        WorkerRepository repository,
        SaveQueueService saveQueue,
        RegistryManager registryManager,
        NodeService nodeService
    ) {
        this.plugin = plugin;
        this.repository = repository;
        this.saveQueue = saveQueue;
        this.registryManager = registryManager;
        this.nodeService = nodeService;

        // Initialize domain delegates
        this.biographyService = new WorkerBiographyServiceImpl(plugin);
        this.workerFactory = new WorkerFactory(plugin, registryManager, biographyService);
        this.fusionService = new WorkerFusionServiceImpl(this, registryManager, biographyService, repository, saveQueue);
    }

    @Override
    public void initialize() {
        workerCache.clear();
        dirtyWorkers.clear();
        for (WorkerInstance w : repository.loadAll()) {
            workerCache.put(w.getWorkerId(), w);
        }
        plugin.getLogger().info("[WorkerService] Cached " + workerCache.size() + " worker instances.");

        // Register events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Start periodic autosave flush task (runs asynchronously every 5 minutes / 6000 ticks)
        org.bukkit.Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::flushDirtyWorkers, 6000L, 6000L);
    }

    @org.bukkit.event.EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        // Asynchronously flush this quitting player's dirty workers to the database
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            for (WorkerInstance w : getPlayerWorkers(playerId)) {
                if (dirtyWorkers.remove(w.getWorkerId())) {
                    repository.save(w);
                }
            }
        });
    }

    @Override
    public void shutdown() {
        plugin.getLogger().info("[WorkerService] Flushing dirty workers on shutdown...");
        flushDirtyWorkers();
    }

    @Override
    public Optional<WorkerInstance> getWorker(UUID workerId) {
        return Optional.ofNullable(workerCache.get(workerId));
    }

    @Override
    public List<WorkerInstance> getPlayerWorkers(UUID ownerId) {
        List<WorkerInstance> list = new ArrayList<>();
        for (WorkerInstance w : workerCache.values()) {
            if (w.getOwnerId().equals(ownerId)) {
                list.add(w);
            }
        }
        return list;
    }

    @Override
    public List<WorkerInstance> getNodeWorkers(UUID nodeId) {
        List<WorkerInstance> list = new ArrayList<>();
        for (WorkerInstance w : workerCache.values()) {
            if (nodeId.equals(w.getAssignedNodeId())) {
                list.add(w);
            }
        }
        return list;
    }

    @Override
    public List<WorkerInstance> getAllWorkers() {
        return new ArrayList<>(workerCache.values());
    }

    @Override
    public WorkerInstance spawnWorker(UUID ownerId, String templateId) {
        WorkerInstance worker = workerFactory.generateWorker(ownerId, templateId);
        saveWorkerImmediate(worker); // Critical creation event: write immediately
        return worker;
    }

    @Override
    public boolean assignWorker(Player player, UUID workerId, UUID nodeId) {
        WorkerInstance worker = workerCache.get(workerId);
        if (worker == null) {
            player.sendMessage("§cWorker instance not found!");
            return false;
        }

        if (!worker.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("branzidle.admin")) {
            player.sendMessage("§cYou do not own this worker!");
            return false;
        }

        Optional<ProductionNode> nodeOpt = nodeService.getNode(nodeId);
        if (nodeOpt.isEmpty()) {
            player.sendMessage("§cTarget production node not found!");
            return false;
        }

        ProductionNode node = nodeOpt.get();
        if (!node.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("branzidle.admin")) {
            player.sendMessage("§cYou do not own this target production node!");
            return false;
        }

        // Verify profession match
        Optional<WorkerRegistry.WorkerTemplate> tplOpt = registryManager.getWorkerRegistry().getTemplate(worker.getTemplateId());
        if (tplOpt.isPresent()) {
            String profession = tplOpt.get().profession();
            if (!profession.equalsIgnoreCase(node.getNodeType().name()) && !profession.equalsIgnoreCase("GENERAL")) {
                player.sendMessage("§cWorker profession (" + profession + ") does not match node type (" + node.getNodeType().name() + ")!");
                return false;
            }
        }

        // Check node slot limits
        int currentAssigned = getNodeWorkers(nodeId).size();
        Optional<NodeRegistry.NodeDefinition> defOpt = registryManager.getNodeRegistry().getNodeDefinition(node.getTierKey());
        int maxSlots = defOpt.map(NodeRegistry.NodeDefinition::workerSlots).orElse(2);

        if (currentAssigned >= maxSlots) {
            player.sendMessage("§cThis production node has reached its worker slot limit (" + maxSlots + ")!");
            return false;
        }

        worker.setAssignedNodeId(nodeId);
        saveWorkerImmediate(worker); // Assignment change: write immediately
        player.sendMessage("§aAssigned worker to node successfully!");
        return true;
    }

    @Override
    public boolean assignWorkerInternal(UUID playerId, UUID workerId, UUID nodeId) {
        WorkerInstance worker = workerCache.get(workerId);
        if (worker != null) {
            worker.setAssignedNodeId(nodeId);
            saveWorkerImmediate(worker); // State change: write immediately
            return true;
        }
        return false;
    }

    @Override
    public boolean unassignWorker(Player player, UUID workerId) {
        WorkerInstance worker = workerCache.get(workerId);
        if (worker == null || (!worker.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("branzidle.admin"))) {
            player.sendMessage("§cYou do not own this worker!");
            return false;
        }

        if (worker.getAssignedNodeId() == null) {
            player.sendMessage("§cWorker is not currently assigned to any node!");
            return false;
        }

        worker.setAssignedNodeId(null);
        saveWorkerImmediate(worker); // Assignment change: write immediately
        player.sendMessage("§eUnassigned worker from node.");
        return true;
    }

    @Override
    public void updateWorker(WorkerInstance worker) {
        if (worker != null) {
            workerCache.put(worker.getWorkerId(), worker);
            dirtyWorkers.add(worker.getWorkerId()); // Flags for periodic flush
        }
    }

    @Override
    public void saveWorkerImmediate(WorkerInstance worker) {
        if (worker != null) {
            workerCache.put(worker.getWorkerId(), worker);
            dirtyWorkers.remove(worker.getWorkerId());
            saveQueue.queueTask(() -> repository.save(worker));
        }
    }

    @Override
    public void deleteWorker(UUID workerId) {
        workerCache.remove(workerId);
        dirtyWorkers.remove(workerId);
        saveQueue.queueTask(() -> repository.deleteById(workerId));
    }

    @Override
    public void flushDirtyWorkers() {
        if (dirtyWorkers.isEmpty()) return;

        List<UUID> toSave = new ArrayList<>(dirtyWorkers);
        dirtyWorkers.clear();

        plugin.getLogger().info("[WorkerService] Flushing " + toSave.size() + " modified workers to database...");
        for (UUID id : toSave) {
            WorkerInstance w = workerCache.get(id);
            if (w != null) {
                repository.save(w);
            }
        }
    }

    @Override
    public boolean fuseWorkers(Player player, UUID targetId, UUID ingredientId1, UUID ingredientId2) {
        WorkerInstance target = workerCache.get(targetId);
        WorkerInstance w1 = workerCache.get(ingredientId1);
        WorkerInstance w2 = workerCache.get(ingredientId2);

        if (target == null || w1 == null || w2 == null) {
            player.sendMessage("§cOne or more workers could not be found!");
            return false;
        }

        return fusionService.fuseWorkers(player, target, w1, w2);
    }

    @Override
    public WorkerBiographyService getBiographyService() {
        return biographyService;
    }

    @Override
    public WorkerFusionService getFusionService() {
        return fusionService;
    }

    @Override
    public WorkerStats getEffectiveStats(WorkerInstance worker) {
        Optional<WorkerRegistry.WorkerTemplate> tplOpt = registryManager.getWorkerRegistry().getTemplate(worker.getTemplateId());
        if (tplOpt.isEmpty()) {
            return new WorkerStats(0.0, 0.0, 0.0);
        }

        WorkerRegistry.WorkerTemplate tpl = tplOpt.get();
        int lvl = Math.max(1, worker.getLevel());
        double speed = tpl.speedBonus() + (lvl - 1) * tpl.growthPerLevel() * worker.getSpeedPotential();
        double yield = tpl.yieldBonus() + (lvl - 1) * tpl.growthPerLevel() * worker.getYieldPotential();
        double rare = tpl.rareDropBonus() + (lvl - 1) * (tpl.growthPerLevel() / 2.0) * worker.getRarePotential();
        return new WorkerStats(speed, yield, rare);
    }
}
