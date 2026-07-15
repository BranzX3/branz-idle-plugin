package com.example.plugin.territory.service;

import com.example.plugin.territory.model.ChunkClaim;
import com.example.plugin.territory.model.ChunkType;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface managing territory claims, spatial queries, and land expansion rules.
 */
public interface TerritoryService {

    /**
     * Initializes territory cache from database during plugin startup.
     */
    void initialize();

    /**
     * Retrieves a claim at the specified chunk coordinates.
     */
    Optional<ChunkClaim> getClaim(int chunkX, int chunkZ);

    /**
     * Retrieves all claims owned by a player.
     */
    List<ChunkClaim> getPlayerClaims(UUID playerId);

    /**
     * Attempts to claim a chunk for a player during normal gameplay.
     * Checks adjacency constraints and economy requirements.
     */
    boolean claimChunk(Player player, int chunkX, int chunkZ, ChunkType type);

    /**
     * Internal claim method for onboarding and system overrides.
     * Bypasses economy and adjacency constraints.
     */
    boolean claimChunkInternal(UUID playerId, int chunkX, int chunkZ, ChunkType type);

    /**
     * Attempts to unclaim a chunk owned by a player.
     */
    boolean unclaimChunk(Player player, int chunkX, int chunkZ);

    /**
     * Internal unclaim method for base resets and system overrides.
     * Bypasses player permission checks and messaging.
     */
    boolean unclaimChunkInternal(int chunkX, int chunkZ);

    /**
     * Checks if the player owns at least one directly adjacent chunk (N, S, E, W).
     */
    boolean hasAdjacentClaim(UUID playerId, int chunkX, int chunkZ);

    /**
     * Checks if a 2x2 chunk square starting at the origin (X, Z) is completely unclaimed.
     */
    boolean is2x2AreaAvailable(int originX, int originZ);

    /**
     * Updates an existing claim in cache and queues a database save.
     */
    void updateClaim(ChunkClaim claim);

    /**
     * Checks whether the given world matches the configured dedicated idle world.
     */
    boolean isDedicatedWorld(World world);
}
