package com.example.plugin.territory.repository;

import com.example.plugin.database.DatabaseManager;
import com.example.plugin.database.Repository;
import com.example.plugin.territory.model.ChunkClaim;
import com.example.plugin.territory.model.ChunkCoord;
import com.example.plugin.territory.model.ChunkType;
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
 * JDBC repository managing ChunkClaim persistence across SQLite and MySQL.
 */
public class TerritoryRepository implements Repository<ChunkClaim, ChunkCoord> {

    private final JavaPlugin plugin;
    private final DatabaseManager dbManager;
    private final boolean isMySql;

    public TerritoryRepository(JavaPlugin plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.isMySql = "mysql".equalsIgnoreCase(plugin.getConfig().getString("database.type", "sqlite"));
    }

    /**
     * Loads all territory chunks from the database into memory during startup.
     *
     * @return list of all claimed chunks
     */
    public List<ChunkClaim> loadAll() {
        List<ChunkClaim> claims = new ArrayList<>();
        String sql = "SELECT * FROM territory_chunks";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                claims.add(mapRow(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[TerritoryRepository] Error loading all territory claims: " + e.getMessage());
            e.printStackTrace();
        }
        return claims;
    }

    @Override
    public Optional<ChunkClaim> findById(ChunkCoord coord) {
        String sql = "SELECT * FROM territory_chunks WHERE chunk_x = ? AND chunk_z = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, coord.getX());
            ps.setInt(2, coord.getZ());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[TerritoryRepository] Error finding territory claim: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<ChunkClaim> findByOwner(UUID ownerId) {
        List<ChunkClaim> claims = new ArrayList<>();
        String sql = "SELECT * FROM territory_chunks WHERE owner_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ownerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    claims.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[TerritoryRepository] Error finding player claims: " + e.getMessage());
            e.printStackTrace();
        }
        return claims;
    }

    @Override
    public void save(ChunkClaim entity) {
        String sql = isMySql
            ? """
              INSERT INTO territory_chunks (chunk_x, chunk_z, owner_id, chunk_type, node_id, claimed_at)
              VALUES (?, ?, ?, ?, ?, ?)
              ON DUPLICATE KEY UPDATE
                owner_id = VALUES(owner_id),
                chunk_type = VALUES(chunk_type),
                node_id = VALUES(node_id),
                claimed_at = VALUES(claimed_at)
              """
            : """
              INSERT INTO territory_chunks (chunk_x, chunk_z, owner_id, chunk_type, node_id, claimed_at)
              VALUES (?, ?, ?, ?, ?, ?)
              ON CONFLICT(chunk_x, chunk_z) DO UPDATE SET
                owner_id = excluded.owner_id,
                chunk_type = excluded.chunk_type,
                node_id = excluded.node_id,
                claimed_at = excluded.claimed_at
              """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entity.getChunkX());
            ps.setInt(2, entity.getChunkZ());
            ps.setString(3, entity.getOwnerId().toString());
            ps.setString(4, entity.getChunkType().name());
            if (entity.getNodeId() != null) {
                ps.setString(5, entity.getNodeId().toString());
            } else {
                ps.setNull(5, java.sql.Types.VARCHAR);
            }
            ps.setLong(6, entity.getClaimedAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[TerritoryRepository] Error saving territory claim: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void deleteById(ChunkCoord coord) {
        String sql = "DELETE FROM territory_chunks WHERE chunk_x = ? AND chunk_z = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, coord.getX());
            ps.setInt(2, coord.getZ());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[TerritoryRepository] Error deleting territory claim: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private ChunkClaim mapRow(ResultSet rs) throws SQLException {
        int x = rs.getInt("chunk_x");
        int z = rs.getInt("chunk_z");
        UUID ownerId = UUID.fromString(rs.getString("owner_id"));
        ChunkType type;
        try {
            type = ChunkType.valueOf(rs.getString("chunk_type").toUpperCase());
        } catch (Exception e) {
            type = ChunkType.RESIDENTIAL;
        }
        String nodeStr = rs.getString("node_id");
        UUID nodeId = nodeStr != null && !nodeStr.isEmpty() ? UUID.fromString(nodeStr) : null;
        long claimedAt = rs.getLong("claimed_at");
        return new ChunkClaim(x, z, ownerId, type, nodeId, claimedAt);
    }
}
