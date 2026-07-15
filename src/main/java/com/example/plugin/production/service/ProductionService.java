package com.example.plugin.production.service;

import com.example.plugin.node.model.ProductionNode;
import org.bukkit.entity.Player;

/**
 * Service interface governing active real-time production ticks and offline progression delta calculation.
 */
public interface ProductionService {

    /**
     * Executes one active production simulation tick across all registered nodes in the world.
     */
    void tickAllActiveNodes();

    /**
     * Calculates and applies offline production catch-up yield for all nodes owned by a returning player.
     * Enforces the maximum offline time limit configured in config.yml.
     */
    void calculateOfflineDelta(Player player);

    /**
     * Simulates production yield for a specific node over a defined duration in seconds.
     *
     * @return true if yield was added, false if duration too small or storage full
     */
    boolean simulateNodeProduction(ProductionNode node, long durationSeconds);
}
