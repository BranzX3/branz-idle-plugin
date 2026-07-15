package com.example.plugin.storage.service;

import com.example.plugin.storage.model.NodeStorage;
import com.example.plugin.storage.model.PlayerResourceWallet;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Service interface governing node storage buffers, collection transfers, and virtual player wallets.
 */
public interface StorageService {

    /**
     * Initializes storage and wallet caches from database during startup.
     */
    void initialize();

    /**
     * Retrieves or creates the storage buffer for a given production node.
     */
    NodeStorage getNodeStorage(UUID nodeId);

    /**
     * Retrieves or creates the virtual resource wallet for a given player.
     */
    PlayerResourceWallet getPlayerWallet(UUID playerId);

    /**
     * Adds resource yield into a node's storage buffer up to its maximum capacity.
     *
     * @return the actual quantity added before reaching capacity cap
     */
    long addResourceToNode(UUID nodeId, String resourceKey, long amount, long maxCapacity);

    /**
     * Collects all buffered resources from a node into the player's virtual wallet.
     * Bypasses item inventory limits to support large-scale idle yield collection.
     */
    boolean collectFromNode(Player player, UUID nodeId);

    /**
     * Attempts to spend a specific resource quantity from a player's virtual wallet.
     */
    boolean spendResource(UUID playerId, String resourceKey, long amount);

    /**
     * Adds resources directly to a player's virtual wallet.
     */
    void addResourceToWallet(UUID playerId, String resourceKey, long amount);
}
