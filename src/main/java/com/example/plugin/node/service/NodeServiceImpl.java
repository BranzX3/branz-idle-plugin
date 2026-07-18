package com.example.plugin.node.service;

import com.example.plugin.config.NodeRegistry;
import com.example.plugin.config.RegistryManager;
import com.example.plugin.database.SaveQueueService;
import com.example.plugin.economy.service.EconomyService;
import com.example.plugin.node.model.NodeType;
import com.example.plugin.node.model.ProductionNode;
import com.example.plugin.node.repository.NodeRepository;
import com.example.plugin.territory.model.ChunkClaim;
import com.example.plugin.territory.model.ChunkType;
import com.example.plugin.territory.service.TerritoryService;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of NodeService using ConcurrentHashMap caching and async persistence.
 */
public class NodeServiceImpl implements NodeService {

    private final JavaPlugin plugin;
    private final NodeRepository repository;
    private final SaveQueueService saveQueue;
    private final RegistryManager registryManager;
    private final EconomyService economyService;
    private final TerritoryService territoryService;
    private final Map<UUID, ProductionNode> nodeCache = new ConcurrentHashMap<>();

    public NodeServiceImpl(
        JavaPlugin plugin,
        NodeRepository repository,
        SaveQueueService saveQueue,
        RegistryManager registryManager,
        EconomyService economyService,
        TerritoryService territoryService
    ) {
        this.plugin = plugin;
        this.repository = repository;
        this.saveQueue = saveQueue;
        this.registryManager = registryManager;
        this.economyService = economyService;
        this.territoryService = territoryService;
    }

    @Override
    public void initialize() {
        nodeCache.clear();
        for (ProductionNode n : repository.loadAll()) {
            nodeCache.put(n.getNodeId(), n);
        }
        plugin.getLogger().info("[NodeService] Cached " + nodeCache.size() + " production nodes.");
    }

    @Override
    public Optional<ProductionNode> getNode(UUID nodeId) {
        return Optional.ofNullable(nodeCache.get(nodeId));
    }

    @Override
    public List<ProductionNode> getPlayerNodes(UUID ownerId) {
        List<ProductionNode> list = new ArrayList<>();
        for (ProductionNode n : nodeCache.values()) {
            if (n.getOwnerId().equals(ownerId)) {
                list.add(n);
            }
        }
        return list;
    }

    @Override
    public List<ProductionNode> getAllNodes() {
        return new ArrayList<>(nodeCache.values());
    }

    @Override
    public ProductionNode createNode(UUID ownerId, NodeType type, int originChunkX, int originChunkZ) {
        UUID nodeId = UUID.randomUUID();
        long now = System.currentTimeMillis();
        ProductionNode node = new ProductionNode(nodeId, ownerId, type, 1, 1, "default", now, now);

        nodeCache.put(nodeId, node);
        saveQueue.queueTask(() -> repository.save(node));

        Optional<ChunkClaim> claimOpt = territoryService.getClaim(originChunkX, originChunkZ);
        if (claimOpt.isPresent()) {
            ChunkClaim claim = claimOpt.get();
            claim.setNodeId(nodeId);
            claim.setChunkType(ChunkType.PRODUCTION);
            territoryService.updateClaim(claim);
        }

        // Paste schematic for the new node
        triggerSchematicPaste(node, originChunkX, originChunkZ);

        return node;
    }

    @Override
    public boolean upgradeNode(Player player, UUID nodeId) {
        ProductionNode node = nodeCache.get(nodeId);
        if (node == null) {
            player.sendMessage("§cProduction node not found!");
            return false;
        }

        if (!node.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("branzidle.admin")) {
            player.sendMessage("§cYou do not own this production node!");
            return false;
        }

        int nextLevel = node.getLevel() + 1;
        String tierKey = node.getNodeType().name().toLowerCase() + "_lv" + nextLevel;
        Optional<NodeRegistry.NodeDefinition> defOpt = registryManager.getNodeRegistry().getNodeDefinition(tierKey);

        if (defOpt.isEmpty()) {
            player.sendMessage("§cThis node has reached its maximum level tier!");
            return false;
        }

        NodeRegistry.NodeDefinition def = defOpt.get();
        Map<String, Long> costs = def.upgradeCost();

        // Check economy coin/diamond requirements
        long requiredCoins = costs.getOrDefault("coins", 0L);
        long requiredDiamonds = costs.getOrDefault("diamonds", 0L);

        if (requiredCoins > 0 && economyService.getProfile(player.getUniqueId()).map(p -> p.getCoins() < requiredCoins).orElse(true)) {
            player.sendMessage("§cInsufficient Coins to upgrade node! Need: " + requiredCoins);
            return false;
        }
        if (requiredDiamonds > 0 && economyService.getProfile(player.getUniqueId()).map(p -> p.getDiamonds() < requiredDiamonds).orElse(true)) {
            player.sendMessage("§cInsufficient Diamonds to upgrade node! Need: " + requiredDiamonds);
            return false;
        }

        // Handle spatial expansion if tier requires 4 chunks (2x2) vs 1 chunk
        if (def.sizeChunks() == 4 && node.getSizeChunks() == 1) {
            // Find origin chunk
            ChunkClaim originClaim = null;
            for (ChunkClaim c : territoryService.getPlayerClaims(player.getUniqueId())) {
                if (nodeId.equals(c.getNodeId())) {
                    originClaim = c;
                    break;
                }
            }

            if (originClaim != null) {
                int ox = originClaim.getChunkX();
                int oz = originClaim.getChunkZ();
                // Ensure origin, (ox+1, oz), (ox, oz+1), (ox+1, oz+1) are either owned by player or available
                if (!territoryService.is2x2AreaAvailable(ox, oz)) {
                    // Check if player owns them all
                    boolean allOwnedByMe = check2x2Ownership(player.getUniqueId(), ox, oz);
                    if (!allOwnedByMe) {
                        player.sendMessage("§cCannot expand node to 2x2! Adjacent chunks East and South are obstructed or owned by other players.");
                        return false;
                    }
                } else {
                    // Automatically claim the remaining 3 chunks as PRODUCTION
                    territoryService.claimChunkInternal(player.getUniqueId(), ox + 1, oz, ChunkType.PRODUCTION);
                    territoryService.claimChunkInternal(player.getUniqueId(), ox, oz + 1, ChunkType.PRODUCTION);
                    territoryService.claimChunkInternal(player.getUniqueId(), ox + 1, oz + 1, ChunkType.PRODUCTION);
                }
            }
        }

        // Deduct currency
        if (requiredCoins > 0) economyService.removeCoins(player.getUniqueId(), requiredCoins);
        if (requiredDiamonds > 0) economyService.removeDiamonds(player.getUniqueId(), requiredDiamonds);

        // Apply level up
        node.setLevel(nextLevel);
        if (def.sizeChunks() > node.getSizeChunks()) {
            node.setSizeChunks(def.sizeChunks());
        }
        updateNode(node);

        // Paste schematic for the upgraded level
        ChunkClaim originClaim = null;
        for (ChunkClaim c : territoryService.getPlayerClaims(player.getUniqueId())) {
            if (node.getNodeId().equals(c.getNodeId())) {
                originClaim = c;
                break;
            }
        }
        if (originClaim != null) {
            triggerSchematicPaste(node, originClaim.getChunkX(), originClaim.getChunkZ());
        }

        player.sendMessage("§aSuccessfully upgraded production node to Level " + nextLevel + "!");
        return true;
    }

    @Override
    public void updateNode(ProductionNode node) {
        if (node != null) {
            nodeCache.put(node.getNodeId(), node);
            saveQueue.queueTask(() -> repository.save(node));
        }
    }

    @Override
    public boolean upgradeNodeStorage(Player player, UUID nodeId) {
        ProductionNode node = nodeCache.get(nodeId);
        if (node == null) {
            player.sendMessage("§cProduction node not found!");
            return false;
        }

        if (!node.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("branzidle.admin")) {
            player.sendMessage("§cYou do not own this production node!");
            return false;
        }

        int currentStorageLvl = node.getStorageLevel();
        long cost = 1000L * currentStorageLvl;

        double coinsVal = economyService.getProfile(player.getUniqueId()).map(com.example.plugin.economy.model.PlayerProfile::getCoins).orElse(0.0);
        if ((long) coinsVal < cost) {
            player.sendMessage("§cInsufficient Coins to upgrade storage! Need: " + cost);
            return false;
        }

        economyService.removeCoins(player.getUniqueId(), cost);
        node.setStorageLevel(currentStorageLvl + 1);
        updateNode(node);

        player.sendMessage("§aSuccessfully upgraded node storage to Level " + (currentStorageLvl + 1) + "!");
        return true;
    }

    @Override
    public void deleteNode(UUID nodeId) {
        nodeCache.remove(nodeId);
        saveQueue.queueTask(() -> repository.deleteById(nodeId));

        // Lazily clean up associated exploration and storage data to avoid circular initialization dependencies
        com.example.plugin.bootstrap.BranzIdlePlugin idlePlugin = org.bukkit.plugin.java.JavaPlugin.getPlugin(com.example.plugin.bootstrap.BranzIdlePlugin.class);
        com.example.plugin.bootstrap.ServiceRegistry registry = idlePlugin.getServiceRegistry();
        if (registry != null) {
            registry.getService(com.example.plugin.exploration.service.ExplorationService.class)
                .ifPresent(service -> service.deleteExploration(nodeId));
            registry.getService(com.example.plugin.storage.service.StorageService.class)
                .ifPresent(service -> service.deleteNodeStorage(nodeId));
        }
    }

    private boolean check2x2Ownership(UUID playerId, int ox, int oz) {
        for (int dx = 0; dx < 2; dx++) {
            for (int dz = 0; dz < 2; dz++) {
                Optional<ChunkClaim> c = territoryService.getClaim(ox + dx, oz + dz);
                if (c.isEmpty() || !c.get().getOwnerId().equals(playerId)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void triggerSchematicPaste(ProductionNode node, int chunkX, int chunkZ) {
        String tierKey = node.getTierKey();
        Optional<NodeRegistry.NodeDefinition> defOpt = registryManager.getNodeRegistry().getNodeDefinition(tierKey);
        if (defOpt.isEmpty()) return;
        String schematicFile = defOpt.get().schematic();

        try {
            com.example.plugin.bootstrap.BranzIdlePlugin idlePlugin = org.bukkit.plugin.java.JavaPlugin.getPlugin(com.example.plugin.bootstrap.BranzIdlePlugin.class);
            Optional<com.example.plugin.integration.provider.StructureProvider> spOpt = idlePlugin.getServiceRegistry().getService(com.example.plugin.integration.provider.StructureProvider.class);
            if (spOpt.isPresent()) {
                org.bukkit.World world = org.bukkit.Bukkit.getWorld(plugin.getConfig().getString("territory.dedicated_world_name", "idle_world"));
                if (world != null) {
                    double cx = (chunkX * 16) + 8.0;
                    double cz = (chunkZ * 16) + 8.0;
                    double cy = world.getHighestBlockYAt((int) cx, (int) cz);
                    if (cy <= 0) {
                        cy = 64; // default Y level
                    }
                    org.bukkit.Location loc = new org.bukkit.Location(world, cx, cy, cz);

                    // Paste schematic asynchronously to prevent lag
                    org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        boolean success = spOpt.get().pasteSchematic(schematicFile, -1, loc);
                        if (success) {
                            plugin.getLogger().info("[NodeService] Pasted schematic " + schematicFile + " for node " + node.getNodeId() + " (Level " + node.getLevel() + ") at " + loc);
                        } else {
                            plugin.getLogger().warning("[NodeService] Failed to paste schematic " + schematicFile + " for node " + node.getNodeId() + " (Level " + node.getLevel() + ")");
                        }
                    });
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[NodeService] Exception while triggering schematic paste: " + e.getMessage());
        }
    }
}
