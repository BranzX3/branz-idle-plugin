package com.example.plugin.worker.service;

import com.example.plugin.config.NodeRegistry;
import com.example.plugin.config.RegistryManager;
import com.example.plugin.config.WorkerRegistry;
import com.example.plugin.database.SaveQueueService;
import com.example.plugin.node.model.ProductionNode;
import com.example.plugin.node.service.NodeService;
import com.example.plugin.worker.model.WorkerInstance;
import com.example.plugin.worker.model.WorkerStats;
import com.example.plugin.worker.repository.WorkerRepository;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of WorkerService using ConcurrentHashMap cache and async persistence.
 */
public class WorkerServiceImpl implements WorkerService {

    private final JavaPlugin plugin;
    private final WorkerRepository repository;
    private final SaveQueueService saveQueue;
    private final RegistryManager registryManager;
    private final NodeService nodeService;
    private final Map<UUID, WorkerInstance> workerCache = new ConcurrentHashMap<>();

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
    }

    @Override
    public void initialize() {
        workerCache.clear();
        for (WorkerInstance w : repository.loadAll()) {
            workerCache.put(w.getWorkerId(), w);
        }
        plugin.getLogger().info("[WorkerService] Cached " + workerCache.size() + " worker instances.");
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
        UUID workerId = UUID.randomUUID();
        WorkerInstance worker = new WorkerInstance(workerId, ownerId, templateId, null, 1, 0L);
        workerCache.put(workerId, worker);
        saveQueue.queueTask(() -> repository.save(worker));
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

        // Verify profession match or generic compatibility
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
        updateWorker(worker);
        player.sendMessage("§aAssigned worker to node successfully!");
        return true;
    }

    @Override
    public boolean assignWorkerInternal(UUID playerId, UUID workerId, UUID nodeId) {
        WorkerInstance worker = workerCache.get(workerId);
        if (worker != null) {
            worker.setAssignedNodeId(nodeId);
            updateWorker(worker);
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
        updateWorker(worker);
        player.sendMessage("§eUnassigned worker from node.");
        return true;
    }

    @Override
    public void updateWorker(WorkerInstance worker) {
        if (worker != null) {
            workerCache.put(worker.getWorkerId(), worker);
            saveQueue.queueTask(() -> repository.save(worker));
        }
    }

    @Override
    public WorkerStats getEffectiveStats(WorkerInstance worker) {
        Optional<WorkerRegistry.WorkerTemplate> tplOpt = registryManager.getWorkerRegistry().getTemplate(worker.getTemplateId());
        if (tplOpt.isEmpty()) {
            return new WorkerStats(0.0, 0.0, 0.0);
        }

        WorkerRegistry.WorkerTemplate tpl = tplOpt.get();
        int lvl = Math.max(1, worker.getLevel());
        double speed = tpl.speedBonus() + (lvl - 1) * tpl.growthPerLevel();
        double yield = tpl.yieldBonus() + (lvl - 1) * tpl.growthPerLevel();
        double rare = tpl.rareDropBonus() + (lvl - 1) * (tpl.growthPerLevel() / 2.0);
        return new WorkerStats(speed, yield, rare);
    }
}
