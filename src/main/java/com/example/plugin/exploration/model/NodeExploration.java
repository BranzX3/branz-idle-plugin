package com.example.plugin.exploration.model;

import java.util.UUID;

/**
 * Domain entity tracking a production node's exploration mastery level and accumulated XP.
 * Higher exploration levels unlock rare drop tables.
 */
public class NodeExploration {

    private final UUID nodeId;
    private int explorationLevel;
    private long experience;

    public NodeExploration(UUID nodeId, int explorationLevel, long experience) {
        this.nodeId = nodeId;
        this.explorationLevel = explorationLevel;
        this.experience = experience;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public int getExplorationLevel() {
        return explorationLevel;
    }

    public void setExplorationLevel(int explorationLevel) {
        this.explorationLevel = explorationLevel;
    }

    public long getExperience() {
        return experience;
    }

    public void setExperience(long experience) {
        this.experience = experience;
    }

    /**
     * Adds exploration XP and checks for level-ups.
     * Formula: required XP for next level = current level * 100.
     *
     * @return true if the node leveled up one or more times
     */
    public synchronized boolean addExperience(long amount) {
        if (amount <= 0) return false;
        this.experience += amount;
        boolean leveledUp = false;
        long required = (long) explorationLevel * 100L;
        while (this.experience >= required) {
            this.experience -= required;
            this.explorationLevel++;
            leveledUp = true;
            required = (long) explorationLevel * 100L;
        }
        return leveledUp;
    }
}
