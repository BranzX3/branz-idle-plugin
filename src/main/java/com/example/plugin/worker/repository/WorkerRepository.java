package com.example.plugin.worker.repository;

import com.example.plugin.database.DatabaseManager;
import com.example.plugin.database.Repository;
import com.example.plugin.worker.model.WorkerInstance;
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
 * JDBC repository managing WorkerInstance persistence across SQLite and MySQL.
 */
public class WorkerRepository implements Repository<WorkerInstance, UUID> {

    private final JavaPlugin plugin;
    private final DatabaseManager dbManager;
    private final boolean isMySql;

    public WorkerRepository(JavaPlugin plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.isMySql = "mysql".equalsIgnoreCase(plugin.getConfig().getString("database.type", "sqlite"));
    }

    public List<WorkerInstance> loadAll() {
        List<WorkerInstance> workers = new ArrayList<>();
        String sql = "SELECT * FROM worker_instances";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                workers.add(mapRow(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[WorkerRepository] Error loading all worker instances: " + e.getMessage());
            e.printStackTrace();
        }
        return workers;
    }

    @Override
    public Optional<WorkerInstance> findById(UUID workerId) {
        String sql = "SELECT * FROM worker_instances WHERE worker_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, workerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[WorkerRepository] Error finding worker instance: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<WorkerInstance> findByOwner(UUID ownerId) {
        List<WorkerInstance> workers = new ArrayList<>();
        String sql = "SELECT * FROM worker_instances WHERE owner_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ownerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    workers.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[WorkerRepository] Error finding player workers: " + e.getMessage());
            e.printStackTrace();
        }
        return workers;
    }

    public List<WorkerInstance> findByNode(UUID nodeId) {
        List<WorkerInstance> workers = new ArrayList<>();
        String sql = "SELECT * FROM worker_instances WHERE assigned_node_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nodeId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    workers.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[WorkerRepository] Error finding node workers: " + e.getMessage());
            e.printStackTrace();
        }
        return workers;
    }

    @Override
    public void save(WorkerInstance entity) {
        String sql = isMySql
            ? """
              INSERT INTO worker_instances (worker_id, owner_id, template_id, assigned_node_id, level, experience)
              VALUES (?, ?, ?, ?, ?, ?)
              ON DUPLICATE KEY UPDATE
                assigned_node_id = VALUES(assigned_node_id),
                level = VALUES(level),
                experience = VALUES(experience)
              """
            : """
              INSERT INTO worker_instances (worker_id, owner_id, template_id, assigned_node_id, level, experience)
              VALUES (?, ?, ?, ?, ?, ?)
              ON CONFLICT(worker_id) DO UPDATE SET
                assigned_node_id = excluded.assigned_node_id,
                level = excluded.level,
                experience = excluded.experience
              """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entity.getWorkerId().toString());
            ps.setString(2, entity.getOwnerId().toString());
            ps.setString(3, entity.getTemplateId());
            if (entity.getAssignedNodeId() != null) {
                ps.setString(4, entity.getAssignedNodeId().toString());
            } else {
                ps.setNull(4, java.sql.Types.VARCHAR);
            }
            ps.setInt(5, entity.getLevel());
            ps.setLong(6, entity.getExperience());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[WorkerRepository] Error saving worker instance: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void deleteById(UUID workerId) {
        String sql = "DELETE FROM worker_instances WHERE worker_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, workerId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[WorkerRepository] Error deleting worker instance: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private WorkerInstance mapRow(ResultSet rs) throws SQLException {
        UUID workerId = UUID.fromString(rs.getString("worker_id"));
        UUID ownerId = UUID.fromString(rs.getString("owner_id"));
        String templateId = rs.getString("template_id");
        String nodeStr = rs.getString("assigned_node_id");
        UUID nodeId = nodeStr != null && !nodeStr.isEmpty() ? UUID.fromString(nodeStr) : null;
        int level = rs.getInt("level");
        long exp = rs.getLong("experience");
        return new WorkerInstance(workerId, ownerId, templateId, nodeId, level, exp);
    }
}
