package com.example.plugin.production.service;

import com.example.plugin.config.DropTableRegistry;
import com.example.plugin.config.NodeRegistry;
import com.example.plugin.config.RegistryManager;
import com.example.plugin.exploration.service.ExplorationService;
import com.example.plugin.node.model.ProductionNode;
import com.example.plugin.node.service.NodeService;
import com.example.plugin.storage.service.StorageService;
import com.example.plugin.worker.model.WorkerInstance;
import com.example.plugin.worker.model.WorkerStats;
import com.example.plugin.worker.service.WorkerService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Implementation of ProductionService executing tick intervals, worker stat multipliers, rare drops, and offline catch-up.
 */
public class ProductionServiceImpl implements ProductionService {

    private final JavaPlugin plugin;
    private final NodeService nodeService;
    private final WorkerService workerService;
    private final StorageService storageService;
    private final ExplorationService explorationService;
    private final RegistryManager registryManager;
    private final long maxOfflineMillis;

    public ProductionServiceImpl(
        JavaPlugin plugin,
        NodeService nodeService,
        WorkerService workerService,
        StorageService storageService,
        ExplorationService explorationService,
        RegistryManager registryManager
    ) {
        this.plugin = plugin;
        this.nodeService = nodeService;
        this.workerService = workerService;
        this.storageService = storageService;
        this.explorationService = explorationService;
        this.registryManager = registryManager;
        long hours = plugin.getConfig().getLong("simulation.offline_max_hours", 24L);
        this.maxOfflineMillis = hours * 3600L * 1000L;
    }

    @Override
    public void tickAllActiveNodes() {
        long now = System.currentTimeMillis();
        for (ProductionNode node : nodeService.getAllNodes()) {
            long elapsedSeconds = (now - node.getLastCalculatedTime()) / 1000L;
            if (elapsedSeconds >= 5L) {
                simulateNodeProduction(node, elapsedSeconds);
            }
        }
    }

    @Override
    public void calculateOfflineDelta(Player player) {
        long now = System.currentTimeMillis();
        List<ProductionNode> nodes = nodeService.getPlayerNodes(player.getUniqueId());
        long totalResourcesProduced = 0;

        for (ProductionNode node : nodes) {
            long elapsedMillis = now - node.getLastCalculatedTime();
            if (elapsedMillis > 10_000L) { // Offline at least 10 seconds
                long effectiveMillis = Math.min(elapsedMillis, maxOfflineMillis);
                long seconds = effectiveMillis / 1000L;
                if (seconds > 0) {
                    boolean produced = simulateNodeProduction(node, seconds);
                    if (produced) {
                        totalResourcesProduced++;
                    }
                }
            }
        }

        if (totalResourcesProduced > 0) {
            player.sendMessage("§6==========================================");
            player.sendMessage("§e§lWelcome Back! Offline Progress Report:");
            player.sendMessage("§aYour assigned workers kept gathering while you were away.");
            player.sendMessage("§f+ §b" + totalResourcesProduced + " Production Nodes generated resources into their storage buffers.");
            player.sendMessage("§7Use §e/idle collect §7at your base nodes to claim your yields!");
            player.sendMessage("§6==========================================");
        }
    }

    @Override
    public boolean simulateNodeProduction(ProductionNode node, long durationSeconds) {
        if (durationSeconds <= 0) return false;

        Optional<NodeRegistry.NodeDefinition> defOpt = registryManager.getNodeRegistry().getNodeDefinition(node.getTierKey());
        if (defOpt.isEmpty()) {
            node.setLastCalculatedTime(System.currentTimeMillis());
            nodeService.updateNode(node);
            return false;
        }

        NodeRegistry.NodeDefinition def = defOpt.get();
        List<WorkerInstance> workers = workerService.getNodeWorkers(node.getNodeId());

        double totalSpeedBonus = 0.0;
        double totalYieldBonus = 0.0;
        double totalRareBonus = 0.0;

        for (WorkerInstance w : workers) {
            WorkerStats stats = workerService.getEffectiveStats(w);
            totalSpeedBonus += stats.speedBonus();
            totalYieldBonus += stats.yieldBonus();
            totalRareBonus += stats.rareDropBonus();
        }

        double baseInterval = def.baseTickSeconds();
        double effectiveInterval = Math.max(1.0, baseInterval / (1.0 + totalSpeedBonus));
        long cycles = (long) (durationSeconds / effectiveInterval);

        if (cycles <= 0) {
            return false;
        }

        // Limit maximum cycles to simulate at once to prevent server freeze
        cycles = Math.min(cycles, 5000L);

        boolean anyAdded = false;
        long maxCap = def.maxStorageCapacity();

        int explorationLevel = explorationService.getExploration(node.getNodeId()).getExplorationLevel();
        String prefix = node.getNodeType().name().toLowerCase();
        List<DropTableRegistry.DropTableDefinition> activeTables = new java.util.ArrayList<>();
        for (DropTableRegistry.DropTableDefinition tableDef : registryManager.getDropTableRegistry().getAllDropTables().values()) {
            if (tableDef.tableKey().startsWith(prefix) && tableDef.minExplorationLevel() <= explorationLevel) {
                activeTables.add(tableDef);
            }
        }

        String defaultItem = switch (node.getNodeType()) {
            case MINING -> "iron_ore";
            case LUMBER -> "oak_log";
            case FISHING -> "golden_fish";
        };

        List<DropTableRegistry.DropEntry> eligibleEntries = new java.util.ArrayList<>();
        double totalWeight = 0.0;
        for (DropTableRegistry.DropTableDefinition tableDef : activeTables) {
            for (DropTableRegistry.DropEntry entry : tableDef.entries()) {
                eligibleEntries.add(entry);
                totalWeight += entry.weight();
            }
        }

        for (long c = 0; c < cycles; c++) {
            if (eligibleEntries.isEmpty()) {
                long amount = (long) Math.round(1.0 * (1.0 + totalYieldBonus));
                if (amount > 0) {
                    long added = storageService.addResourceToNode(node.getNodeId(), defaultItem, amount, maxCap);
                    if (added > 0) {
                        anyAdded = true;
                    }
                }
            } else {
                double rand = ThreadLocalRandom.current().nextDouble() * totalWeight;
                double accum = 0.0;
                DropTableRegistry.DropEntry selected = null;
                for (DropTableRegistry.DropEntry entry : eligibleEntries) {
                    accum += entry.weight();
                    if (rand <= accum) {
                        selected = entry;
                        break;
                    }
                }
                if (selected == null) {
                    selected = eligibleEntries.get(0);
                }

                long baseQty = ThreadLocalRandom.current().nextLong(selected.minQty(), selected.maxQty() + 1);
                long amount = (long) Math.round(baseQty * (1.0 + totalYieldBonus));
                if (amount > 0) {
                    long added = storageService.addResourceToNode(node.getNodeId(), selected.resourceKey(), amount, maxCap);
                    if (added > 0) {
                        anyAdded = true;
                    }
                }
            }
        }

        // 3. Award Mastery and Worker XP
        explorationService.addExplorationExp(node.getNodeId(), cycles * 5L);
        for (WorkerInstance w : workers) {
            w.addExperience(cycles * 10L);
            workerService.updateWorker(w);
        }

        node.setLastCalculatedTime(System.currentTimeMillis());
        nodeService.updateNode(node);
        return anyAdded;
    }
}
