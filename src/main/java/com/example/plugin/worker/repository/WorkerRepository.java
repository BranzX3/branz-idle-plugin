package com.example.plugin.worker.repository;

import com.example.plugin.database.DatabaseManager;
import com.example.plugin.database.Repository;
import com.example.plugin.worker.model.WorkerInstance;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
              INSERT INTO worker_instances (
                id, worker_id, owner_id, template_id, assigned_node_id, level, experience,
                speed_potential, yield_potential, rare_potential,
                personality, born_location, generation, fusion_count, seconds_worked, custom_title
              ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
              ON DUPLICATE KEY UPDATE
                assigned_node_id = VALUES(assigned_node_id),
                level = VALUES(level),
                experience = VALUES(experience),
                speed_potential = VALUES(speed_potential),
                yield_potential = VALUES(yield_potential),
                rare_potential = VALUES(rare_potential),
                personality = VALUES(personality),
                born_location = VALUES(born_location),
                generation = VALUES(generation),
                fusion_count = VALUES(fusion_count),
                seconds_worked = VALUES(seconds_worked),
                custom_title = VALUES(custom_title)
              """
            : """
              INSERT INTO worker_instances (
                id, worker_id, owner_id, template_id, assigned_node_id, level, experience,
                speed_potential, yield_potential, rare_potential,
                personality, born_location, generation, fusion_count, seconds_worked, custom_title
              ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
              ON CONFLICT(worker_id) DO UPDATE SET
                assigned_node_id = excluded.assigned_node_id,
                level = excluded.level,
                experience = excluded.experience,
                speed_potential = excluded.speed_potential,
                yield_potential = excluded.yield_potential,
                rare_potential = excluded.rare_potential,
                personality = excluded.personality,
                born_location = excluded.born_location,
                generation = excluded.generation,
                fusion_count = excluded.fusion_count,
                seconds_worked = excluded.seconds_worked,
                custom_title = excluded.custom_title
              """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            if (entity.getSerialId() > 0) {
                ps.setInt(1, entity.getSerialId());
            } else {
                ps.setNull(1, java.sql.Types.INTEGER);
            }
            ps.setString(2, entity.getWorkerId().toString());
            ps.setString(3, entity.getOwnerId().toString());
            ps.setString(4, entity.getTemplateId());
            if (entity.getAssignedNodeId() != null) {
                ps.setString(5, entity.getAssignedNodeId().toString());
            } else {
                ps.setNull(5, java.sql.Types.VARCHAR);
            }
            ps.setInt(6, entity.getLevel());
            ps.setLong(7, entity.getExperience());
            ps.setDouble(8, entity.getSpeedPotential());
            ps.setDouble(9, entity.getYieldPotential());
            ps.setDouble(10, entity.getRarePotential());
            ps.setString(11, entity.getPersonality());
            ps.setString(12, entity.getBornLocation());
            ps.setInt(13, entity.getGeneration());
            ps.setInt(14, entity.getFusionCount());
            ps.setLong(15, entity.getSecondsWorked());
            if (entity.getCustomTitle() != null) {
                ps.setString(16, entity.getCustomTitle());
            } else {
                ps.setNull(16, java.sql.Types.VARCHAR);
            }
            
            ps.executeUpdate();

            // Retrieve and assign the auto-increment surrogate PK as the serial ID
            if (entity.getSerialId() == 0) {
                try (ResultSet gk = ps.getGeneratedKeys()) {
                    if (gk.next()) {
                        entity.setSerialId(gk.getInt(1));
                    }
                }
            }
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

        // Load fields from autoincrement surrogate PK id column
        int serialId = rs.getInt("id");
        
        double speedPotential = rs.getDouble("speed_potential");
        if (rs.wasNull()) speedPotential = 1.0;
        
        double yieldPotential = rs.getDouble("yield_potential");
        if (rs.wasNull()) yieldPotential = 1.0;
        
        double rarePotential = rs.getDouble("rare_potential");
        if (rs.wasNull()) rarePotential = 1.0;
        
        String personality = rs.getString("personality");
        if (personality == null) personality = "SERIOUS";
        
        String bornLocation = rs.getString("born_location");
        if (bornLocation == null) bornLocation = "Guild";
        
        int generation = rs.getInt("generation");
        if (rs.wasNull()) generation = 1;
        
        int fusionCount = rs.getInt("fusion_count");
        if (rs.wasNull()) fusionCount = 0;
        
        long secondsWorked = rs.getLong("seconds_worked");
        if (rs.wasNull()) secondsWorked = 0;
        
        String customTitle = rs.getString("custom_title");

        return new WorkerInstance(
            workerId, ownerId, templateId, nodeId, level, exp,
            serialId, speedPotential, yieldPotential, rarePotential,
            personality, bornLocation, generation, fusionCount, secondsWorked, customTitle
        );
    }
}
