package com.example.plugin.territory.service;

import com.example.plugin.database.SaveQueueService;
import com.example.plugin.territory.event.TerritoryClaimedEvent;
import com.example.plugin.territory.model.ChunkClaim;
import com.example.plugin.territory.model.ChunkCoord;
import com.example.plugin.territory.model.ChunkType;
import com.example.plugin.territory.repository.TerritoryRepository;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of TerritoryService using in-memory ConcurrentHashMap and async database writes.
 */
public class TerritoryServiceImpl implements TerritoryService {

    private final JavaPlugin plugin;
    private final TerritoryRepository repository;
    private final SaveQueueService saveQueue;
    private final Map<Long, ChunkClaim> territoryCache = new ConcurrentHashMap<>();
    private final String dedicatedWorldName;

    public TerritoryServiceImpl(JavaPlugin plugin, TerritoryRepository repository, SaveQueueService saveQueue) {
        this.plugin = plugin;
        this.repository = repository;
        this.saveQueue = saveQueue;
        this.dedicatedWorldName = plugin.getConfig().getString("territory.dedicated_world_name", "idle_world");
    }

    @Override
    public void initialize() {
        territoryCache.clear();
        List<ChunkClaim> claims = repository.loadAll();
        for (ChunkClaim claim : claims) {
            long packed = ChunkCoord.toPackedLong(claim.getChunkX(), claim.getChunkZ());
            territoryCache.put(packed, claim);
        }
        plugin.getLogger().info("[TerritoryService] Cached " + territoryCache.size() + " territory chunk claims.");
    }

    @Override
    public Optional<ChunkClaim> getClaim(int chunkX, int chunkZ) {
        long packed = ChunkCoord.toPackedLong(chunkX, chunkZ);
        return Optional.ofNullable(territoryCache.get(packed));
    }

    @Override
    public List<ChunkClaim> getPlayerClaims(UUID playerId) {
        List<ChunkClaim> list = new ArrayList<>();
        for (ChunkClaim claim : territoryCache.values()) {
            if (claim.getOwnerId().equals(playerId)) {
                list.add(claim);
            }
        }
        return list;
    }

    @Override
    public boolean claimChunk(Player player, int chunkX, int chunkZ, ChunkType type) {
        if (!isDedicatedWorld(player.getWorld())) {
            player.sendMessage("§cYou can only claim territory in the dedicated Idle world!");
            return false;
        }

        long packed = ChunkCoord.toPackedLong(chunkX, chunkZ);
        if (territoryCache.containsKey(packed)) {
            player.sendMessage("§cThis chunk is already claimed!");
            return false;
        }

        UUID playerId = player.getUniqueId();
        List<ChunkClaim> existing = getPlayerClaims(playerId);
        if (!existing.isEmpty() && !hasAdjacentClaim(playerId, chunkX, chunkZ)) {
            player.sendMessage("§cNew claims must be directly adjacent (N, S, E, W) to your existing territory!");
            return false;
        }

        ChunkClaim claim = new ChunkClaim(chunkX, chunkZ, playerId, type, null, System.currentTimeMillis());
        territoryCache.put(packed, claim);
        saveQueue.queueTask(() -> repository.save(claim));

        Bukkit.getScheduler().runTask(plugin, () ->
            Bukkit.getPluginManager().callEvent(new TerritoryClaimedEvent(player, claim)));

        player.sendMessage("§aSuccessfully claimed territory chunk at (" + chunkX + ", " + chunkZ + ") as " + type.name() + "!");
        return true;
    }

    @Override
    public boolean claimChunkInternal(UUID playerId, int chunkX, int chunkZ, ChunkType type) {
        long packed = ChunkCoord.toPackedLong(chunkX, chunkZ);
        if (territoryCache.containsKey(packed)) {
            return false;
        }

        ChunkClaim claim = new ChunkClaim(chunkX, chunkZ, playerId, type, null, System.currentTimeMillis());
        territoryCache.put(packed, claim);
        saveQueue.queueTask(() -> repository.save(claim));
        return true;
    }

    @Override
    public boolean unclaimChunk(Player player, int chunkX, int chunkZ) {
        long packed = ChunkCoord.toPackedLong(chunkX, chunkZ);
        ChunkClaim claim = territoryCache.get(packed);
        if (claim == null) {
            player.sendMessage("§cThis chunk is not claimed!");
            return false;
        }

        if (!claim.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("branzidle.admin")) {
            player.sendMessage("§cYou do not own this territory chunk!");
            return false;
        }

        territoryCache.remove(packed);
        saveQueue.queueTask(() -> repository.deleteById(new ChunkCoord(chunkX, chunkZ)));
        player.sendMessage("§eUnclaimed territory chunk at (" + chunkX + ", " + chunkZ + ").");
        return true;
    }

    @Override
    public boolean hasAdjacentClaim(UUID playerId, int chunkX, int chunkZ) {
        int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
        for (int[] dir : dirs) {
            int nx = chunkX + dir[0];
            int nz = chunkZ + dir[1];
            ChunkClaim adj = territoryCache.get(ChunkCoord.toPackedLong(nx, nz));
            if (adj != null && adj.getOwnerId().equals(playerId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean is2x2AreaAvailable(int originX, int originZ) {
        for (int dx = 0; dx < 2; dx++) {
            for (int dz = 0; dz < 2; dz++) {
                if (territoryCache.containsKey(ChunkCoord.toPackedLong(originX + dx, originZ + dz))) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void updateClaim(ChunkClaim claim) {
        if (claim != null) {
            long packed = ChunkCoord.toPackedLong(claim.getChunkX(), claim.getChunkZ());
            territoryCache.put(packed, claim);
            saveQueue.queueTask(() -> repository.save(claim));
        }
    }

    @Override
    public boolean isDedicatedWorld(World world) {
        return world != null && world.getName().equalsIgnoreCase(dedicatedWorldName);
    }
}
