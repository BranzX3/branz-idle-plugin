package com.example.plugin.node.repository;

import com.example.plugin.database.DatabaseManager;
import com.example.plugin.database.Repository;
import com.example.plugin.node.model.NodeType;
import com.example.plugin.node.model.ProductionNode;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC repository managing ProductionNode persistence across SQLite and MySQL.
 */
public class NodeRepository implements Repository<ProductionNode, UUID> {

    private final JavaPlugin plugin;
    private final DatabaseManager dbManager;
    private final boolean isMySql;

    public NodeRepository(JavaPlugin plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.isMySql = "mysql".equalsIgnoreCase(plugin.getConfig().getString("database.type", "sqlite"));
    }

    public List<ProductionNode> loadAll() {
        List<ProductionNode> nodes = new ArrayList<>();
        String sql = "SELECT * FROM production_nodes";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                nodes.add(mapRow(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[NodeRepository] Error loading all production nodes: " + e.getMessage());
            e.printStackTrace();
        }
        return nodes;
    }

    @Override
    public Optional<ProductionNode> findById(UUID nodeId) {
        String sql = "SELECT * FROM production_nodes WHERE node_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nodeId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[NodeRepository] Error finding production node: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<ProductionNode> findByOwner(UUID ownerId) {
        List<ProductionNode> nodes = new ArrayList<>();
        String sql = "SELECT * FROM production_nodes WHERE owner_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ownerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    nodes.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[NodeRepository] Error finding player production nodes: " + e.getMessage());
            e.printStackTrace();
        }
        return nodes;
    }

    @Override
    public void save(ProductionNode entity) {
        String sql = isMySql
            ? """
              INSERT INTO production_nodes (node_id, owner_id, node_type, level, size_chunks, style_id, last_calculated_time, created_at, active_event, event_progress)
              VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
              ON DUPLICATE KEY UPDATE
                level = VALUES(level),
                size_chunks = VALUES(size_chunks),
                style_id = VALUES(style_id),
                last_calculated_time = VALUES(last_calculated_time),
                active_event = VALUES(active_event),
                event_progress = VALUES(event_progress)
              """
            : """
              INSERT INTO production_nodes (node_id, owner_id, node_type, level, size_chunks, style_id, last_calculated_time, created_at, active_event, event_progress)
              VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
              ON CONFLICT(node_id) DO UPDATE SET
                level = excluded.level,
                size_chunks = excluded.size_chunks,
                style_id = excluded.style_id,
                last_calculated_time = excluded.last_calculated_time,
                active_event = excluded.active_event,
                event_progress = excluded.event_progress
              """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entity.getNodeId().toString());
            ps.setString(2, entity.getOwnerId().toString());
            ps.setString(3, entity.getNodeType().name());
            ps.setInt(4, entity.getLevel());
            ps.setInt(5, entity.getSizeChunks());
            ps.setString(6, entity.getStyleId());
            ps.setLong(7, entity.getLastCalculatedTime());
            ps.setLong(8, entity.getCreatedAt());
            ps.setString(9, entity.getActiveEventKey());
            ps.setInt(10, entity.getEventProgress());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[NodeRepository] Error saving production node: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void deleteById(UUID nodeId) {
        String sql = "DELETE FROM production_nodes WHERE node_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nodeId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[NodeRepository] Error deleting production node: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private ProductionNode mapRow(ResultSet rs) throws SQLException {
        UUID nodeId = UUID.fromString(rs.getString("node_id"));
        UUID ownerId = UUID.fromString(rs.getString("owner_id"));
        NodeType type;
        try {
            type = NodeType.valueOf(rs.getString("node_type").toUpperCase());
        } catch (Exception e) {
            type = NodeType.MINING;
        }
        int level = rs.getInt("level");
        int size = rs.getInt("size_chunks");
        String style = rs.getString("style_id");
        long lastCalc = rs.getLong("last_calculated_time");
        long createdAt = rs.getLong("created_at");
        String activeEvent = rs.getString("active_event");
        int eventProgress = rs.getInt("event_progress");
        return new ProductionNode(nodeId, ownerId, type, level, size, style, lastCalc, createdAt, activeEvent, eventProgress);
    }
}
