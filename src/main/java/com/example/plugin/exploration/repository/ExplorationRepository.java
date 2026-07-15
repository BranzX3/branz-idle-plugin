package com.example.plugin.exploration.repository;

import com.example.plugin.database.DatabaseManager;
import com.example.plugin.database.Repository;
import com.example.plugin.exploration.model.NodeExploration;
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
 * JDBC repository managing NodeExploration persistence across SQLite and MySQL.
 */
public class ExplorationRepository implements Repository<NodeExploration, UUID> {

    private final JavaPlugin plugin;
    private final DatabaseManager dbManager;
    private final boolean isMySql;

    public ExplorationRepository(JavaPlugin plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.isMySql = "mysql".equalsIgnoreCase(plugin.getConfig().getString("database.type", "sqlite"));
    }

    public List<NodeExploration> loadAll() {
        List<NodeExploration> list = new ArrayList<>();
        String sql = "SELECT node_id, exploration_level, experience FROM node_exploration";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[ExplorationRepository] Error loading all exploration records: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public Optional<NodeExploration> findById(UUID nodeId) {
        String sql = "SELECT node_id, exploration_level, experience FROM node_exploration WHERE node_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nodeId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[ExplorationRepository] Error finding node exploration: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public void save(NodeExploration entity) {
        String sql = isMySql
            ? """
              INSERT INTO node_exploration (node_id, exploration_level, experience)
              VALUES (?, ?, ?)
              ON DUPLICATE KEY UPDATE
                exploration_level = VALUES(exploration_level),
                experience = VALUES(experience)
              """
            : """
              INSERT INTO node_exploration (node_id, exploration_level, experience)
              VALUES (?, ?, ?)
              ON CONFLICT(node_id) DO UPDATE SET
                exploration_level = excluded.exploration_level,
                experience = excluded.experience
              """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entity.getNodeId().toString());
            ps.setInt(2, entity.getExplorationLevel());
            ps.setLong(3, entity.getExperience());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[ExplorationRepository] Error saving node exploration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void deleteById(UUID nodeId) {
        String sql = "DELETE FROM node_exploration WHERE node_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nodeId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[ExplorationRepository] Error deleting node exploration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private NodeExploration mapRow(ResultSet rs) throws SQLException {
        UUID nodeId = UUID.fromString(rs.getString("node_id"));
        int level = rs.getInt("exploration_level");
        long exp = rs.getLong("experience");
        return new NodeExploration(nodeId, level, exp);
    }
}
