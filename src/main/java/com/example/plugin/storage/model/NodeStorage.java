package com.example.plugin.storage.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Domain entity representing stored resource yields buffered inside a production node.
 */
public class NodeStorage {

    private final UUID nodeId;
    private final Map<String, Long> resourceQuantities;

    public NodeStorage(UUID nodeId, Map<String, Long> resourceQuantities) {
        this.nodeId = nodeId;
        this.resourceQuantities = resourceQuantities != null ? new HashMap<>(resourceQuantities) : new HashMap<>();
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public Map<String, Long> getResourceQuantities() {
        return Collections.unmodifiableMap(resourceQuantities);
    }

    public synchronized long getQuantity(String resourceKey) {
        return resourceQuantities.getOrDefault(resourceKey, 0L);
    }

    public synchronized long addResource(String resourceKey, long amount, long maxCapacity) {
        long current = getQuantity(resourceKey);
        long newAmount = current + amount;
        long actualAdded = amount;
        if (newAmount > maxCapacity) {
            actualAdded = Math.max(0, maxCapacity - current);
            newAmount = maxCapacity;
        }
        resourceQuantities.put(resourceKey, newAmount);
        return actualAdded;
    }

    public synchronized void clear() {
        resourceQuantities.clear();
    }
}
