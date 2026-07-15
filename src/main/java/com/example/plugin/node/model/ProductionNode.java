package com.example.plugin.node.model;

import java.util.UUID;

/**
 * Domain entity representing an active resource production node.
 */
public class ProductionNode {

    private final UUID nodeId;
    private final UUID ownerId;
    private final NodeType nodeType;
    private int level;
    private int sizeChunks;
    private String styleId;
    private long lastCalculatedTime;
    private final long createdAt;

    public ProductionNode(
        UUID nodeId,
        UUID ownerId,
        NodeType nodeType,
        int level,
        int sizeChunks,
        String styleId,
        long lastCalculatedTime,
        long createdAt
    ) {
        this.nodeId = nodeId;
        this.ownerId = ownerId;
        this.nodeType = nodeType;
        this.level = level;
        this.sizeChunks = sizeChunks;
        this.styleId = styleId != null ? styleId : "default";
        this.lastCalculatedTime = lastCalculatedTime;
        this.createdAt = createdAt;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getSizeChunks() {
        return sizeChunks;
    }

    public void setSizeChunks(int sizeChunks) {
        this.sizeChunks = sizeChunks;
    }

    public String getStyleId() {
        return styleId;
    }

    public void setStyleId(String styleId) {
        this.styleId = styleId;
    }

    public long getLastCalculatedTime() {
        return lastCalculatedTime;
    }

    public void setLastCalculatedTime(long lastCalculatedTime) {
        this.lastCalculatedTime = lastCalculatedTime;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getTierKey() {
        return nodeType.name().toLowerCase() + "_lv" + level;
    }
}
