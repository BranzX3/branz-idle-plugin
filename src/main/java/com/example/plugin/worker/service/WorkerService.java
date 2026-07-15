package com.example.plugin.worker.service;

import com.example.plugin.worker.model.WorkerInstance;
import com.example.plugin.worker.model.WorkerStats;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface governing worker instances, node assignments, and level-dependent stat bonuses.
 */
public interface WorkerService {

    /**
     * Initializes worker cache from database during startup.
     */
    void initialize();

    /**
     * Retrieves a worker instance by its UUID.
     */
    Optional<WorkerInstance> getWorker(UUID workerId);

    /**
     * Retrieves all workers owned by a player.
     */
    List<WorkerInstance> getPlayerWorkers(UUID ownerId);

    /**
     * Retrieves all workers currently assigned to a specific node.
     */
    List<WorkerInstance> getNodeWorkers(UUID nodeId);

    /**
     * Retrieves all worker instances across the server.
     */
    List<WorkerInstance> getAllWorkers();

    /**
     * Spawns a new worker instance from a template and persists it.
     */
    WorkerInstance spawnWorker(UUID ownerId, String templateId);

    /**
     * Attempts to assign a worker to a production node for a player during gameplay.
     * Validates worker limit slots of the node and ownership.
     */
    boolean assignWorker(Player player, UUID workerId, UUID nodeId);

    /**
     * Internal worker assignment for onboarding and gacha workflows.
     */
    boolean assignWorkerInternal(UUID playerId, UUID workerId, UUID nodeId);

    /**
     * Unassigns a worker from its current node.
     */
    boolean unassignWorker(Player player, UUID workerId);

    /**
     * Updates a worker instance in memory and queues an async database save.
     */
    void updateWorker(WorkerInstance worker);

    /**
     * Computes the effective stats (Speed, Yield, Rare Drop) scaled by the worker's current level.
     */
    WorkerStats getEffectiveStats(WorkerInstance worker);
}
