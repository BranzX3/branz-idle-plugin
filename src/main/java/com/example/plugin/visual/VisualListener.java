package com.example.plugin.visual;

import com.example.plugin.visual.service.VisualService;
import com.example.plugin.node.model.ProductionNode;
import com.example.plugin.node.service.NodeService;
import com.example.plugin.territory.model.ChunkClaim;
import com.example.plugin.territory.service.TerritoryService;
import com.example.plugin.worker.model.WorkerInstance;
import com.example.plugin.worker.service.WorkerService;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VisualListener implements Listener {

    private final JavaPlugin plugin;
    private final VisualService visualService;
    private final NodeService nodeService;
    private final WorkerService workerService;
    private final TerritoryService territoryService;

    // Track active shown workers to avoid spawning them repeatedly
    private final Map<UUID, Set<UUID>> playerActiveWorkerNpcs = new ConcurrentHashMap<>();

    public VisualListener(
        JavaPlugin plugin,
        VisualService visualService,
        NodeService nodeService,
        WorkerService workerService,
        TerritoryService territoryService
    ) {
        this.plugin = plugin;
        this.visualService = visualService;
        this.nodeService = nodeService;
        this.workerService = workerService;
        this.territoryService = territoryService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                updateVisualsForPlayer(player, player.getLocation().getChunk());
            }
        }, 60L); // Allow some ticks after join
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        Set<UUID> spawned = playerActiveWorkerNpcs.remove(playerId);
        if (spawned != null) {
            for (UUID workerId : spawned) {
                visualService.hideWorker(workerId);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        // Debounce to chunk entry transitions
        if (from.getChunk().getX() == to.getChunk().getX() && from.getChunk().getZ() == to.getChunk().getZ()) {
            return;
        }

        updateVisualsForPlayer(event.getPlayer(), to.getChunk());
    }

    public void updateVisualsForPlayer(Player player, Chunk currentChunk) {
        if (!territoryService.isDedicatedWorld(player.getWorld())) {
            // Hide all workers if player leaves the dedicated idle world
            hideAllWorkersForPlayer(player.getUniqueId());
            return;
        }

        UUID playerId = player.getUniqueId();
        List<ProductionNode> nodes = nodeService.getPlayerNodes(playerId);
        List<ChunkClaim> claims = territoryService.getPlayerClaims(playerId);

        Set<UUID> activeWorkers = playerActiveWorkerNpcs.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet());
        Set<UUID> workersToKeep = new HashSet<>();

        for (ProductionNode node : nodes) {
            // Find the chunk coordinates of this node
            Optional<ChunkClaim> nodeClaimOpt = claims.stream()
                .filter(c -> node.getNodeId().equals(c.getNodeId()))
                .findFirst();

            if (nodeClaimOpt.isPresent()) {
                ChunkClaim claim = nodeClaimOpt.get();
                int nx = claim.getChunkX();
                int nz = claim.getChunkZ();

                // Check distance in chunks (4 chunks = 64 blocks range)
                int dist = Math.max(Math.abs(currentChunk.getX() - nx), Math.abs(currentChunk.getZ() - nz));
                List<WorkerInstance> workers = workerService.getNodeWorkers(node.getNodeId());

                if (dist <= 4) {
                    // Spawn workers for this node if they aren't already active
                    World world = player.getWorld();
                    double cx = (nx * 16) + 8.5;
                    double cz = (nz * 16) + 8.5;
                    int cy = world.getHighestBlockYAt((int)cx, (int)cz);
                    Location spawnLoc = new Location(world, cx, cy + 1.0, cz);

                    for (WorkerInstance worker : workers) {
                        workersToKeep.add(worker.getWorkerId());
                        if (!activeWorkers.contains(worker.getWorkerId())) {
                            visualService.showWorker(worker, spawnLoc);
                            activeWorkers.add(worker.getWorkerId());
                        }
                    }
                }
            }
        }

        // Hide workers that are no longer in range
        Iterator<UUID> it = activeWorkers.iterator();
        while (it.hasNext()) {
            UUID workerId = it.next();
            if (!workersToKeep.contains(workerId)) {
                visualService.hideWorker(workerId);
                it.remove();
            }
        }
    }

    private void hideAllWorkersForPlayer(UUID playerId) {
        Set<UUID> spawned = playerActiveWorkerNpcs.remove(playerId);
        if (spawned != null) {
            for (UUID workerId : spawned) {
                visualService.hideWorker(workerId);
            }
        }
    }
}
