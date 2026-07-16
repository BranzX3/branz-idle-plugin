package com.example.plugin.exploration.service;

import com.example.plugin.config.DropTableRegistry;
import com.example.plugin.exploration.model.NodeExploration;
import com.example.plugin.node.model.NodeType;

import java.util.List;
import java.util.UUID;

/**
 * Service interface governing node exploration mastery, XP progression, and rare drop filtering.
 */
public interface ExplorationService {

    /**
     * Initializes exploration records from database during startup.
     */
    void initialize();

    /**
     * Retrieves or creates the exploration record for a given production node.
     */
    NodeExploration getExploration(UUID nodeId);

    /**
     * Adds exploration XP to a node from production cycles or worker activities.
     *
     * @return true if the node leveled up one or more times
     */
    boolean addExplorationExp(UUID nodeId, long amount);

    /**
     * Directly forces/sets the exploration level of a node and resets its experience to 0.
     */
    void setExplorationLevel(UUID nodeId, int level);

    /**
     * Retrieves all rare drop table entries eligible for a node's current exploration mastery level.
     */
    List<DropTableRegistry.DropEntry> getEligibleRareDrops(UUID nodeId, NodeType nodeType);

    /**
     * Deletes the exploration record for the given node from memory and database.
     */
    void deleteExploration(UUID nodeId);

    /**
     * Retrieves the required XP to level up from the given level.
     */
    long getRequiredExperience(int level);

    /**
     * Retrieves the maximum level allowed for exploration.
     */
    int getMaxLevel();
}
