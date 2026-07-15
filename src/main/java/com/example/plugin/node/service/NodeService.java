package com.example.plugin.node.service;

import com.example.plugin.node.model.NodeType;
import com.example.plugin.node.model.ProductionNode;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface governing production nodes, upgrades, and territory assignment.
 */
public interface NodeService {

    /**
     * Initializes node cache from database during startup.
     */
    void initialize();

    /**
     * Retrieves a production node by its UUID.
     */
    Optional<ProductionNode> getNode(UUID nodeId);

    /**
     * Retrieves all nodes owned by a player.
     */
    List<ProductionNode> getPlayerNodes(UUID ownerId);

    /**
     * Retrieves all active nodes currently registered in memory.
     */
    List<ProductionNode> getAllNodes();

    /**
     * Spawns a new Level 1 production node at the specified chunk coordinate.
     */
    ProductionNode createNode(UUID ownerId, NodeType type, int originChunkX, int originChunkZ);

    /**
     * Attempts to upgrade an existing production node to its next level tier.
     * Checks upgrade costs via NodeRegistry and expands spatial footprint if reaching Level 5.
     */
    boolean upgradeNode(Player player, UUID nodeId);

    /**
     * Updates an existing node in memory and queues a database save.
     */
    void updateNode(ProductionNode node);

    /**
     * Deletes an existing production node.
     */
    void deleteNode(UUID nodeId);
}
