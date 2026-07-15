package com.example.plugin.storage.repository;

import com.example.plugin.database.DatabaseManager;
import com.example.plugin.database.Repository;
import com.example.plugin.storage.model.NodeStorage;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC repository managing NodeStorage persistence across SQLite and MySQL.
 */
public class StorageRepository implements Repository<NodeStorage, UUID> {

    private final JavaPlugin plugin;
    private final DatabaseManager dbManager;
    private final boolean isMySql;

    public StorageRepository(JavaPlugin plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.isMySql = "mysql".equalsIgnoreCase(plugin.getConfig().getString("database.type", "sqlite"));
    }

    public List<NodeStorage> loadAll() {
        Map<UUID, Map<String, Long>> map = new HashMap<>();
        String sql = "SELECT node_id, resource_key, quantity FROM node_storage";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID nodeId = UUID.fromString(rs.getString("node_id"));
                String res = rs.getString("resource_key");
                long qty = rs.getLong("quantity");
                map.computeIfAbsent(nodeId, k -> new HashMap<>()).put(res, qty);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[StorageRepository] Error loading all node storage: " + e.getMessage());
            e.printStackTrace();
        }

        List<NodeStorage> list = new ArrayList<>();
        for (Map.Entry<UUID, Map<String, Long>> entry : map.entrySet()) {
            list.add(new NodeStorage(entry.getKey(), entry.getValue()));
        }
        return list;
    }

    @Override
    public Optional<NodeStorage> findById(UUID nodeId) {
        Map<String, Long> resources = new HashMap<>();
        String sql = "SELECT resource_key, quantity FROM node_storage WHERE node_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nodeId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resources.put(rs.getString("resource_key"), rs.getLong("quantity"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[StorageRepository] Error finding node storage: " + e.getMessage());
            e.printStackTrace();
        }
        return resources.isEmpty() ? Optional.empty() : Optional.of(new NodeStorage(nodeId, resources));
    }

    @Override
    public void save(NodeStorage entity) {
        String sql = isMySql
            ? """
              INSERT INTO node_storage (node_id, resource_key, quantity)
              VALUES (?, ?, ?)
              ON DUPLICATE KEY UPDATE
                quantity = VALUES(quantity)
              """
            : """
              INSERT INTO node_storage (node_id, resource_key, quantity)
              VALUES (?, ?, ?)
              ON CONFLICT(node_id, resource_key) DO UPDATE SET
                quantity = excluded.quantity
              """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Map.Entry<String, Long> entry : entity.getResourceQuantities().entrySet()) {
                ps.setString(1, entity.getNodeId().toString());
                ps.setString(2, entry.getKey());
                ps.setLong(3, entry.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            plugin.getLogger().severe("[StorageRepository] Error saving node storage: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void deleteById(UUID nodeId) {
        String sql = "DELETE FROM node_storage WHERE node_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nodeId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[StorageRepository] Error deleting node storage: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
