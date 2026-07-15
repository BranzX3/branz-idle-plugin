package com.example.plugin.storage.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Domain entity representing a player's virtual wallet/warehouse for bulk resources.
 * Prevents item inventory clutter when collecting millions of resources.
 */
public class PlayerResourceWallet {

    private final UUID playerId;
    private final Map<String, Long> resourceQuantities;

    public PlayerResourceWallet(UUID playerId, Map<String, Long> resourceQuantities) {
        this.playerId = playerId;
        this.resourceQuantities = resourceQuantities != null ? new HashMap<>(resourceQuantities) : new HashMap<>();
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public Map<String, Long> getResourceQuantities() {
        return Collections.unmodifiableMap(resourceQuantities);
    }

    public synchronized long getQuantity(String resourceKey) {
        return resourceQuantities.getOrDefault(resourceKey, 0L);
    }

    public synchronized void addResource(String resourceKey, long amount) {
        if (amount > 0) {
            long current = getQuantity(resourceKey);
            resourceQuantities.put(resourceKey, current + amount);
        }
    }

    public synchronized boolean spendResource(String resourceKey, long amount) {
        long current = getQuantity(resourceKey);
        if (current >= amount && amount > 0) {
            resourceQuantities.put(resourceKey, current - amount);
            return true;
        }
        return false;
    }
}
