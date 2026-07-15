package com.example.plugin.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages HikariCP connection pooling and DDL schema migrations.
 * Supports both SQLite (file-based) and MySQL.
 */
public class DatabaseManager {

    private final JavaPlugin plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Initializes the connection pool and creates tables if they do not exist.
     */
    public void initialize() {
        FileConfiguration config = plugin.getConfig();
        String dbType = config.getString("database.type", "sqlite").toLowerCase();

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("BranzIdle-Pool");

        if ("mysql".equals(dbType)) {
            String host = config.getString("database.mysql.host", "localhost");
            int port = config.getInt("database.mysql.port", 3306);
            String dbName = config.getString("database.mysql.database", "branz_idle");
            String username = config.getString("database.mysql.username", "root");
            String password = config.getString("database.mysql.password", "password");
            int poolSize = config.getInt("database.mysql.pool_size", 10);

            hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false&characterEncoding=utf8");
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password);
            hikariConfig.setMaximumPoolSize(poolSize);
            hikariConfig.setMinimumIdle(Math.max(2, poolSize / 2));
            hikariConfig.setConnectionTimeout(5000);
        } else {
            // Default to SQLite
            File dbFile = new File(plugin.getDataFolder(), config.getString("database.sqlite.file", "branz_idle.db"));
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            hikariConfig.setMaximumPoolSize(10);
            hikariConfig.setMinimumIdle(2);
            hikariConfig.setConnectionTimeout(5000);
            hikariConfig.setConnectionTestQuery("SELECT 1");
        }

        this.dataSource = new HikariDataSource(hikariConfig);
        initTables();
        plugin.getLogger().info("[DatabaseManager] Successfully initialized " + dbType.toUpperCase() + " connection pool.");
    }

    /**
     * Retrieves a database connection from the HikariCP pool.
     *
     * @return active SQL Connection
     * @throws SQLException if a connection cannot be obtained
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Database connection pool is not initialized or closed.");
        }
        return dataSource.getConnection();
    }

    /**
     * Closes the connection pool during plugin shutdown.
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("[DatabaseManager] Connection pool closed successfully.");
        }
    }

    private void initTables() {
        String[] schemas = {
            // 1. Players Table
            """
            CREATE TABLE IF NOT EXISTS players (
                player_id VARCHAR(36) PRIMARY KEY,
                username VARCHAR(64) NOT NULL,
                coins DOUBLE PRECISION DEFAULT 0.0,
                diamonds BIGINT DEFAULT 0,
                onboarding_completed BOOLEAN DEFAULT FALSE,
                created_at BIGINT NOT NULL,
                updated_at BIGINT NOT NULL
            );
            """,
            // 2. Territory Chunks Table
            """
            CREATE TABLE IF NOT EXISTS territory_chunks (
                chunk_x INT NOT NULL,
                chunk_z INT NOT NULL,
                owner_id VARCHAR(36) NOT NULL,
                chunk_type VARCHAR(32) NOT NULL DEFAULT 'RESIDENTIAL',
                node_id VARCHAR(36) NULL,
                claimed_at BIGINT NOT NULL,
                PRIMARY KEY (chunk_x, chunk_z)
            );
            """,
            // 3. Production Nodes Table
            """
            CREATE TABLE IF NOT EXISTS production_nodes (
                node_id VARCHAR(36) PRIMARY KEY,
                owner_id VARCHAR(36) NOT NULL,
                node_type VARCHAR(32) NOT NULL,
                level INT DEFAULT 1,
                size_chunks INT DEFAULT 1,
                style_id VARCHAR(64) NOT NULL,
                last_calculated_time BIGINT NOT NULL,
                created_at BIGINT NOT NULL
            );
            """,
            // 4. Node Storage Table
            """
            CREATE TABLE IF NOT EXISTS node_storage (
                node_id VARCHAR(36) NOT NULL,
                resource_key VARCHAR(64) NOT NULL,
                quantity BIGINT DEFAULT 0,
                PRIMARY KEY (node_id, resource_key)
            );
            """,
            // 5. Player Resource Wallet Table
            """
            CREATE TABLE IF NOT EXISTS player_resource (
                player_id VARCHAR(36) NOT NULL,
                resource_key VARCHAR(64) NOT NULL,
                quantity BIGINT DEFAULT 0,
                PRIMARY KEY (player_id, resource_key)
            );
            """,
            // 6. Node Exploration Table
            """
            CREATE TABLE IF NOT EXISTS node_exploration (
                node_id VARCHAR(36) PRIMARY KEY,
                exploration_level INT DEFAULT 1,
                experience BIGINT DEFAULT 0
            );
            """,
            // 7. Worker Instances Table
            """
            CREATE TABLE IF NOT EXISTS worker_instances (
                worker_id VARCHAR(36) PRIMARY KEY,
                owner_id VARCHAR(36) NOT NULL,
                template_id VARCHAR(64) NOT NULL,
                assigned_node_id VARCHAR(36) NULL,
                level INT DEFAULT 1,
                experience BIGINT DEFAULT 0
            );
            """
        };

        String[] indexes = {
            "CREATE INDEX IF NOT EXISTS idx_players_username ON players(username);",
            "CREATE INDEX IF NOT EXISTS idx_territory_owner ON territory_chunks(owner_id);",
            "CREATE INDEX IF NOT EXISTS idx_territory_node ON territory_chunks(node_id);",
            "CREATE INDEX IF NOT EXISTS idx_nodes_owner ON production_nodes(owner_id);",
            "CREATE INDEX IF NOT EXISTS idx_workers_owner ON worker_instances(owner_id);",
            "CREATE INDEX IF NOT EXISTS idx_workers_node ON worker_instances(assigned_node_id);"
        };

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            for (String schema : schemas) {
                stmt.execute(schema);
            }
            for (String index : indexes) {
                stmt.execute(index);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[DatabaseManager] Failed to execute DDL schema initialization: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
