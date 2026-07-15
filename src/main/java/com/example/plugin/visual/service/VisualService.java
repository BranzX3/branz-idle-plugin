package com.example.plugin.visual.service;

import com.example.plugin.worker.model.WorkerInstance;
import org.bukkit.Location;

import java.util.UUID;

/**
 * Service interface governing visual rendering of assigned worker instances at production nodes.
 */
public interface VisualService {

    /**
     * Initializes visual rendering engine and selects the active provider based on detected plugins.
     */
    void initialize();

    /**
     * Spawns or refreshes the visual representation of a worker at the target location.
     */
    void showWorker(WorkerInstance worker, Location location);

    /**
     * Removes the visual representation of a worker from the world.
     */
    void hideWorker(UUID workerId);

    /**
     * Removes all visual worker representations across all worlds upon plugin disable or reload.
     */
    void clearAllVisuals();
}
