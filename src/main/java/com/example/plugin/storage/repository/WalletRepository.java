package com.example.plugin.storage.repository;

import com.example.plugin.database.DatabaseManager;
import com.example.plugin.database.Repository;
import com.example.plugin.storage.model.PlayerResourceWallet;
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
 * JDBC repository managing PlayerResourceWallet persistence across SQLite and MySQL.
 */
public class WalletRepository implements Repository<PlayerResourceWallet, UUID> {

    private final JavaPlugin plugin;
    private final DatabaseManager dbManager;
    private final boolean isMySql;

    public WalletRepository(JavaPlugin plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.isMySql = "mysql".equalsIgnoreCase(plugin.getConfig().getString("database.type", "sqlite"));
    }

    public List<PlayerResourceWallet> loadAll() {
        Map<UUID, Map<String, Long>> map = new HashMap<>();
        String sql = "SELECT player_id, resource_key, quantity FROM player_resource";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID playerId = UUID.fromString(rs.getString("player_id"));
                String res = rs.getString("resource_key");
                long amt = rs.getLong("quantity");
                map.computeIfAbsent(playerId, k -> new HashMap<>()).put(res, amt);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[WalletRepository] Error loading all player wallets: " + e.getMessage());
            e.printStackTrace();
        }

        List<PlayerResourceWallet> list = new ArrayList<>();
        for (Map.Entry<UUID, Map<String, Long>> entry : map.entrySet()) {
            list.add(new PlayerResourceWallet(entry.getKey(), entry.getValue()));
        }
        return list;
    }

    @Override
    public Optional<PlayerResourceWallet> findById(UUID playerId) {
        Map<String, Long> resources = new HashMap<>();
        String sql = "SELECT resource_key, quantity FROM player_resource WHERE player_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resources.put(rs.getString("resource_key"), rs.getLong("quantity"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[WalletRepository] Error finding player wallet: " + e.getMessage());
            e.printStackTrace();
        }
        return resources.isEmpty() ? Optional.empty() : Optional.of(new PlayerResourceWallet(playerId, resources));
    }

    @Override
    public void save(PlayerResourceWallet entity) {
        String sql = isMySql
            ? """
              INSERT INTO player_resource (player_id, resource_key, quantity)
              VALUES (?, ?, ?)
              ON DUPLICATE KEY UPDATE quantity = VALUES(quantity)
              """
            : """
              INSERT INTO player_resource (player_id, resource_key, quantity)
              VALUES (?, ?, ?)
              ON CONFLICT(player_id, resource_key) DO UPDATE SET quantity = excluded.quantity
              """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Map.Entry<String, Long> entry : entity.getResourceQuantities().entrySet()) {
                ps.setString(1, entity.getPlayerId().toString());
                ps.setString(2, entry.getKey());
                ps.setLong(3, entry.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            plugin.getLogger().severe("[WalletRepository] Error saving player wallet: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void deleteById(UUID playerId) {
        String sql = "DELETE FROM player_resource WHERE player_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[WalletRepository] Error deleting player wallet: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
