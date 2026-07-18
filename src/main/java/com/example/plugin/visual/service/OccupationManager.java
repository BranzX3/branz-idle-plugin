package com.example.plugin.visual.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe manager governing worker coordinate reservations (occupancy checks).
 * Ensures that two workers do not pathfind to the exact same marker simultaneously.
 */
public class OccupationManager {

    // Map of nodeId -> Map of markerName -> workerId (UUID)
    private final Map<UUID, Map<String, UUID>> reservations = new ConcurrentHashMap<>();

    /**
     * Attempts to reserve a named marker for a specific worker.
     *
     * @return true if reservation was successful or already owned, false if occupied by another worker
     */
    public synchronized boolean reserve(UUID nodeId, String markerName, UUID workerId) {
        if (nodeId == null || markerName == null || workerId == null) return false;
        Map<String, UUID> nodeRes = reservations.computeIfAbsent(nodeId, k -> new ConcurrentHashMap<>());
        UUID existing = nodeRes.get(markerName);
        if (existing == null || existing.equals(workerId)) {
            nodeRes.put(markerName, workerId);
            return true;
        }
        return false;
    }

    /**
     * Releases a marker reservation.
     */
    public synchronized void release(UUID nodeId, String markerName, UUID workerId) {
        if (nodeId == null || markerName == null || workerId == null) return;
        Map<String, UUID> nodeRes = reservations.get(nodeId);
        if (nodeRes != null) {
            nodeRes.remove(markerName, workerId);
        }
    }

    /**
     * Releases all marker reservations held by a specific worker.
     */
    public synchronized void releaseAll(UUID nodeId, UUID workerId) {
        if (nodeId == null || workerId == null) return;
        Map<String, UUID> nodeRes = reservations.get(nodeId);
        if (nodeRes != null) {
            nodeRes.entrySet().removeIf(entry -> entry.getValue().equals(workerId));
        }
    }

    /**
     * Checks if a marker is occupied.
     */
    public synchronized boolean isOccupied(UUID nodeId, String markerName) {
        if (nodeId == null || markerName == null) return false;
        Map<String, UUID> nodeRes = reservations.get(nodeId);
        return nodeRes != null && nodeRes.containsKey(markerName);
    }

    /**
     * Clears all reservations (e.g. on plugin disable or reload).
     */
    public void clear() {
        reservations.clear();
    }
}
