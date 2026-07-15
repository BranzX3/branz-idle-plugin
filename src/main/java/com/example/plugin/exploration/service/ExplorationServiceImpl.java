package com.example.plugin.exploration.service;

import com.example.plugin.config.DropTableRegistry;
import com.example.plugin.config.RegistryManager;
import com.example.plugin.database.SaveQueueService;
import com.example.plugin.exploration.model.NodeExploration;
import com.example.plugin.exploration.repository.ExplorationRepository;
import com.example.plugin.node.model.NodeType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of ExplorationService using ConcurrentHashMap caching and async persistence.
 */
public class ExplorationServiceImpl implements ExplorationService {

    private final JavaPlugin plugin;
    private final ExplorationRepository repository;
    private final SaveQueueService saveQueue;
    private final RegistryManager registryManager;
    private final Map<UUID, NodeExploration> cache = new ConcurrentHashMap<>();

    public ExplorationServiceImpl(
        JavaPlugin plugin,
        ExplorationRepository repository,
        SaveQueueService saveQueue,
        RegistryManager registryManager
    ) {
        this.plugin = plugin;
        this.repository = repository;
        this.saveQueue = saveQueue;
        this.registryManager = registryManager;
    }

    @Override
    public void initialize() {
        cache.clear();
        for (NodeExploration e : repository.loadAll()) {
            cache.put(e.getNodeId(), e);
        }
        plugin.getLogger().info("[ExplorationService] Cached " + cache.size() + " node exploration records.");
    }

    @Override
    public NodeExploration getExploration(UUID nodeId) {
        return cache.computeIfAbsent(nodeId, k -> new NodeExploration(nodeId, 1, 0L));
    }

    @Override
    public boolean addExplorationExp(UUID nodeId, long amount) {
        NodeExploration exp = getExploration(nodeId);
        boolean leveledUp = exp.addExperience(amount);
        saveQueue.queueTask(() -> repository.save(exp));
        return leveledUp;
    }

    @Override
    public List<DropTableRegistry.DropEntry> getEligibleRareDrops(UUID nodeId, NodeType nodeType) {
        NodeExploration exp = getExploration(nodeId);
        int currentLevel = exp.getExplorationLevel();

        List<DropTableRegistry.DropEntry> eligible = new ArrayList<>();
        String prefix = nodeType.name().toLowerCase();

        for (DropTableRegistry.DropTableDefinition def : registryManager.getDropTableRegistry().getAllDropTables().values()) {
            if (def.tableKey().startsWith(prefix) && def.minExplorationLevel() <= currentLevel) {
                eligible.addAll(def.entries());
            }
        }
        return eligible;
    }
}
