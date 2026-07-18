package com.example.plugin.economy.repository;

import com.example.plugin.database.DatabaseManager;
import com.example.plugin.database.Repository;
import com.example.plugin.economy.model.PlayerProfile;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC repository managing PlayerProfile persistence across SQLite and MySQL.
 */
public class PlayerRepository implements Repository<PlayerProfile, UUID> {

    private final JavaPlugin plugin;
    private final DatabaseManager dbManager;
    private final boolean isMySql;

    public PlayerRepository(JavaPlugin plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.isMySql = "mysql".equalsIgnoreCase(plugin.getConfig().getString("database.type", "sqlite"));
    }

    @Override
    public Optional<PlayerProfile> findById(UUID playerId) {
        String sql = "SELECT * FROM players WHERE player_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PlayerProfile profile = new PlayerProfile(
                        UUID.fromString(rs.getString("player_id")),
                        rs.getString("username"),
                        rs.getDouble("coins"),
                        rs.getLong("diamonds"),
                        rs.getBoolean("onboarding_completed"),
                        rs.getLong("created_at"),
                        rs.getLong("updated_at"),
                        rs.getInt("unlocked_slots")
                    );
                    return Optional.of(profile);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[PlayerRepository] Error loading player profile: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public void save(PlayerProfile entity) {
        String sql = isMySql
            ? """
              INSERT INTO players (player_id, username, coins, diamonds, onboarding_completed, created_at, updated_at, unlocked_slots)
              VALUES (?, ?, ?, ?, ?, ?, ?, ?)
              ON DUPLICATE KEY UPDATE
                username = VALUES(username),
                coins = VALUES(coins),
                diamonds = VALUES(diamonds),
                onboarding_completed = VALUES(onboarding_completed),
                updated_at = VALUES(updated_at),
                unlocked_slots = VALUES(unlocked_slots)
              """
            : """
              INSERT INTO players (player_id, username, coins, diamonds, onboarding_completed, created_at, updated_at, unlocked_slots)
              VALUES (?, ?, ?, ?, ?, ?, ?, ?)
              ON CONFLICT(player_id) DO UPDATE SET
                username = excluded.username,
                coins = excluded.coins,
                diamonds = excluded.diamonds,
                onboarding_completed = excluded.onboarding_completed,
                updated_at = excluded.updated_at,
                unlocked_slots = excluded.unlocked_slots
              """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entity.getPlayerId().toString());
            ps.setString(2, entity.getUsername());
            ps.setDouble(3, entity.getCoins());
            ps.setLong(4, entity.getDiamonds());
            ps.setBoolean(5, entity.isOnboardingCompleted());
            ps.setLong(6, entity.getCreatedAt());
            ps.setLong(7, entity.getUpdatedAt());
            ps.setInt(8, entity.getUnlockedSlots());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[PlayerRepository] Error saving player profile: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void deleteById(UUID playerId) {
        String sql = "DELETE FROM players WHERE player_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[PlayerRepository] Error deleting player profile: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
