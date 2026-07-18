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

    private String activeEventKey;
    private int eventProgress;
    
    // Decoupled storage level
    private int storageLevel;

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
        this.activeEventKey = null;
        this.eventProgress = 0;
        this.storageLevel = 1;
    }

    public ProductionNode(
        UUID nodeId,
        UUID ownerId,
        NodeType nodeType,
        int level,
        int sizeChunks,
        String styleId,
        long lastCalculatedTime,
        long createdAt,
        String activeEventKey,
        int eventProgress,
        int storageLevel
    ) {
        this.nodeId = nodeId;
        this.ownerId = ownerId;
        this.nodeType = nodeType;
        this.level = level;
        this.sizeChunks = sizeChunks;
        this.styleId = styleId != null ? styleId : "default";
        this.lastCalculatedTime = lastCalculatedTime;
        this.createdAt = createdAt;
        this.activeEventKey = activeEventKey;
        this.eventProgress = eventProgress;
        this.storageLevel = storageLevel > 0 ? storageLevel : 1;
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

    public String getActiveEventKey() {
        return activeEventKey;
    }

    public void setActiveEventKey(String activeEventKey) {
        this.activeEventKey = activeEventKey;
    }

    public int getEventProgress() {
        return eventProgress;
    }

    public void setEventProgress(int eventProgress) {
        this.eventProgress = eventProgress;
    }

    public int getStorageLevel() {
        return storageLevel;
    }

    public void setStorageLevel(int storageLevel) {
        this.storageLevel = storageLevel;
    }
}
