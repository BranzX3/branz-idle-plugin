package com.example.plugin.worker.model;

import java.util.UUID;

/**
 * Domain entity representing an individual worker instance owned by a player.
 */
public class WorkerInstance {

    private final UUID workerId;
    private final UUID ownerId;
    private final String templateId;
    private UUID assignedNodeId;
    private int level;
    private long experience;

    public WorkerInstance(
        UUID workerId,
        UUID ownerId,
        String templateId,
        UUID assignedNodeId,
        int level,
        long experience
    ) {
        this.workerId = workerId;
        this.ownerId = ownerId;
        this.templateId = templateId;
        this.assignedNodeId = assignedNodeId;
        this.level = level;
        this.experience = experience;
    }

    public UUID getWorkerId() {
        return workerId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getTemplateId() {
        return templateId;
    }

    public UUID getAssignedNodeId() {
        return assignedNodeId;
    }

    public void setAssignedNodeId(UUID assignedNodeId) {
        this.assignedNodeId = assignedNodeId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public long getExperience() {
        return experience;
    }

    public void setExperience(long experience) {
        this.experience = experience;
    }

    public synchronized boolean addExperience(long amount) {
        if (amount <= 0) return false;
        this.experience += amount;
        boolean leveledUp = false;
        long required = (long) level * 500L;
        while (this.experience >= required) {
            this.experience -= required;
            this.level++;
            leveledUp = true;
            required = (long) level * 500L;
        }
        return leveledUp;
    }
}
