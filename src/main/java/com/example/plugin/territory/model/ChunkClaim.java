package com.example.plugin.territory.model;

import java.util.UUID;

/**
 * Domain model representing a claimed chunk of territory within the dedicated idle world.
 */
public class ChunkClaim {

    private final int chunkX;
    private final int chunkZ;
    private final UUID ownerId;
    private ChunkType chunkType;
    private UUID nodeId;
    private final long claimedAt;

    public ChunkClaim(
        int chunkX,
        int chunkZ,
        UUID ownerId,
        ChunkType chunkType,
        UUID nodeId,
        long claimedAt
    ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.ownerId = ownerId;
        this.chunkType = chunkType;
        this.nodeId = nodeId;
        this.claimedAt = claimedAt;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public ChunkType getChunkType() {
        return chunkType;
    }

    public void setChunkType(ChunkType chunkType) {
        this.chunkType = chunkType;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public long getClaimedAt() {
        return claimedAt;
    }

    public ChunkCoord toCoord() {
        return new ChunkCoord(chunkX, chunkZ);
    }
}
