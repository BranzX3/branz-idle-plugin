package com.example.plugin.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
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
            hikariConfig.setConnectionInitSql("PRAGMA foreign_keys = ON;");
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
                PRIMARY KEY (chunk_x, chunk_z),
                CONSTRAINT fk_territory_owner FOREIGN KEY (owner_id) REFERENCES players(player_id) ON DELETE CASCADE,
                CONSTRAINT fk_territory_node FOREIGN KEY (node_id) REFERENCES production_nodes(node_id) ON DELETE SET NULL
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
                created_at BIGINT NOT NULL,
                active_event VARCHAR(64) NULL,
                event_progress INT DEFAULT 0,
                CONSTRAINT fk_nodes_owner FOREIGN KEY (owner_id) REFERENCES players(player_id) ON DELETE CASCADE
            );
            """,
            // 4. Node Storage Table
            """
            CREATE TABLE IF NOT EXISTS node_storage (
                node_id VARCHAR(36) NOT NULL,
                resource_key VARCHAR(64) NOT NULL,
                quantity BIGINT DEFAULT 0,
                PRIMARY KEY (node_id, resource_key),
                CONSTRAINT fk_storage_node FOREIGN KEY (node_id) REFERENCES production_nodes(node_id) ON DELETE CASCADE
            );
            """,
            // 5. Player Resource Wallet Table
            """
            CREATE TABLE IF NOT EXISTS player_resource (
                player_id VARCHAR(36) NOT NULL,
                resource_key VARCHAR(64) NOT NULL,
                quantity BIGINT DEFAULT 0,
                PRIMARY KEY (player_id, resource_key),
                CONSTRAINT fk_wallet_player FOREIGN KEY (player_id) REFERENCES players(player_id) ON DELETE CASCADE
            );
            """,
            // 6. Node Exploration Table
            """
            CREATE TABLE IF NOT EXISTS node_exploration (
                node_id VARCHAR(36) PRIMARY KEY,
                exploration_level INT DEFAULT 1,
                experience BIGINT DEFAULT 0,
                CONSTRAINT fk_exploration_node FOREIGN KEY (node_id) REFERENCES production_nodes(node_id) ON DELETE CASCADE
            );
            """,
            // 7. Worker Instances Table
            """
            CREATE TABLE IF NOT EXISTS worker_instances (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                worker_id VARCHAR(36) NOT NULL UNIQUE,
                owner_id VARCHAR(36) NOT NULL,
                template_id VARCHAR(64) NOT NULL,
                assigned_node_id VARCHAR(36) NULL,
                level INT DEFAULT 1,
                experience BIGINT DEFAULT 0,
                speed_potential DOUBLE DEFAULT 1.0,
                yield_potential DOUBLE DEFAULT 1.0,
                rare_potential DOUBLE DEFAULT 1.0,
                personality VARCHAR(32) DEFAULT 'SERIOUS',
                born_location VARCHAR(64) DEFAULT 'Guild',
                generation INT DEFAULT 1,
                fusion_count INT DEFAULT 0,
                seconds_worked BIGINT DEFAULT 0,
                custom_title VARCHAR(64) NULL,
                CONSTRAINT fk_workers_owner FOREIGN KEY (owner_id) REFERENCES players(player_id) ON DELETE CASCADE,
                CONSTRAINT fk_workers_node FOREIGN KEY (assigned_node_id) REFERENCES production_nodes(node_id) ON DELETE SET NULL
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
            // Alter production_nodes table to add event progression fields if they don't exist
            try {
                stmt.execute("ALTER TABLE production_nodes ADD COLUMN active_event VARCHAR(64) NULL;");
            } catch (SQLException ignored) {}
            try {
                stmt.execute("ALTER TABLE production_nodes ADD COLUMN event_progress INT DEFAULT 0;");
            } catch (SQLException ignored) {}

            // Auto-migration check for worker_instances table to have id INTEGER PRIMARY KEY AUTOINCREMENT
            boolean hasIdColumn = false;
            try (ResultSet rs = conn.getMetaData().getColumns(null, null, "worker_instances", "id")) {
                if (rs.next()) {
                    hasIdColumn = true;
                }
            } catch (SQLException e) {
                // Table might not exist yet
            }

            if (!hasIdColumn) {
                boolean tableExists = false;
                try (ResultSet rs = conn.getMetaData().getTables(null, null, "worker_instances", null)) {
                    if (rs.next()) {
                        tableExists = true;
                    }
                } catch (SQLException ignored) {}

                if (tableExists) {
                    plugin.getLogger().info("[DatabaseManager] Migrating worker_instances table to use auto-increment surrogate key...");
                    try {
                        stmt.execute("PRAGMA foreign_keys=OFF;");
                        try { stmt.execute("DROP TABLE IF EXISTS worker_instances_old;"); } catch (SQLException ignored) {}
                        stmt.execute("ALTER TABLE worker_instances RENAME TO worker_instances_old;");
                        stmt.execute("""
                            CREATE TABLE worker_instances (
                                id INTEGER PRIMARY KEY AUTOINCREMENT,
                                worker_id VARCHAR(36) NOT NULL UNIQUE,
                                owner_id VARCHAR(36) NOT NULL,
                                template_id VARCHAR(64) NOT NULL,
                                assigned_node_id VARCHAR(36) NULL,
                                level INT DEFAULT 1,
                                experience BIGINT DEFAULT 0,
                                speed_potential DOUBLE DEFAULT 1.0,
                                yield_potential DOUBLE DEFAULT 1.0,
                                rare_potential DOUBLE DEFAULT 1.0,
                                personality VARCHAR(32) DEFAULT 'SERIOUS',
                                born_location VARCHAR(64) DEFAULT 'Guild',
                                generation INT DEFAULT 1,
                                fusion_count INT DEFAULT 0,
                                seconds_worked BIGINT DEFAULT 0,
                                custom_title VARCHAR(64) NULL,
                                CONSTRAINT fk_workers_owner FOREIGN KEY (owner_id) REFERENCES players(player_id) ON DELETE CASCADE,
                                CONSTRAINT fk_workers_node FOREIGN KEY (assigned_node_id) REFERENCES production_nodes(node_id) ON DELETE SET NULL
                            );
                        """);
                        stmt.execute("""
                            INSERT INTO worker_instances (
                                id, worker_id, owner_id, template_id, assigned_node_id, level, experience,
                                speed_potential, yield_potential, rare_potential, personality, born_location,
                                generation, fusion_count, seconds_worked, custom_title
                            )
                            SELECT 
                                serial_id, worker_id, owner_id, template_id, assigned_node_id, level, experience,
                                COALESCE(speed_potential, 1.0), COALESCE(yield_potential, 1.0), COALESCE(rare_potential, 1.0),
                                COALESCE(personality, 'SERIOUS'), COALESCE(born_location, 'Guild'),
                                COALESCE(generation, 1), COALESCE(fusion_count, 0), COALESCE(seconds_worked, 0),
                                custom_title
                            FROM worker_instances_old;
                        """);
                        stmt.execute("DROP TABLE worker_instances_old;");
                        stmt.execute("PRAGMA foreign_keys=ON;");
                        plugin.getLogger().info("[DatabaseManager] worker_instances table migration completed successfully!");
                    } catch (SQLException ex) {
                        plugin.getLogger().severe("[DatabaseManager] Critical error during worker_instances migration: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            }

            // Alter worker_instances table fallback to add new properties if they don't exist
            try { stmt.execute("ALTER TABLE worker_instances ADD COLUMN speed_potential DOUBLE DEFAULT 1.0;"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE worker_instances ADD COLUMN yield_potential DOUBLE DEFAULT 1.0;"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE worker_instances ADD COLUMN rare_potential DOUBLE DEFAULT 1.0;"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE worker_instances ADD COLUMN personality VARCHAR(32) DEFAULT 'SERIOUS';"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE worker_instances ADD COLUMN born_location VARCHAR(64) DEFAULT 'Guild';"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE worker_instances ADD COLUMN generation INT DEFAULT 1;"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE worker_instances ADD COLUMN fusion_count INT DEFAULT 0;"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE worker_instances ADD COLUMN seconds_worked BIGINT DEFAULT 0;"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE worker_instances ADD COLUMN custom_title VARCHAR(64) NULL;"); } catch (SQLException ignored) {}
        } catch (SQLException e) {
            plugin.getLogger().severe("[DatabaseManager] Failed to execute DDL schema initialization: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
