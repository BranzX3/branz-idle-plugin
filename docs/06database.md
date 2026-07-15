# 06 - Database.md

# Branz.Idle Database Schema & Persistence

---

# 1. Database Philosophy & Separation of Concerns

The database layer (`SQLite` / `MySQL`) is strictly responsible for storing **runtime player state and instance data**.

All static game definitions (`worker_template`, `resource`, `drop_table`, `node_style`, `gacha_pool`) are **never** stored in the relational database. They reside inside YAML configuration files and are loaded into memory `Registries` upon startup. Database records reference these templates via lightweight string keys (`template_id = 'miner_t1'`).

---

# 2. Complete Relational Schema (DDL)

## 2.1 Players Table (`players`)
Stores overarching player profile and currency balances.

```sql
CREATE TABLE IF NOT EXISTS players (
    player_id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    coins DOUBLE PRECISION DEFAULT 0.0,
    diamonds BIGINT DEFAULT 0,
    onboarding_completed BOOLEAN DEFAULT FALSE,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_players_username ON players(username);
```

---

## 2.2 Territory Chunks Table (`territory_chunks`)
Tracks claimed 16x16 chunk coordinates inside the dedicated `idle_world`.

```sql
CREATE TABLE IF NOT EXISTS territory_chunks (
    chunk_x INT NOT NULL,
    chunk_z INT NOT NULL,
    owner_id VARCHAR(36) NOT NULL,
    chunk_type VARCHAR(32) NOT NULL DEFAULT 'RESIDENTIAL',
    node_id VARCHAR(36) NULL,
    claimed_at BIGINT NOT NULL,
    PRIMARY KEY (chunk_x, chunk_z),
    CONSTRAINT fk_territory_owner FOREIGN KEY (owner_id) REFERENCES players(player_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_territory_owner ON territory_chunks(owner_id);
CREATE INDEX IF NOT EXISTS idx_territory_node ON territory_chunks(node_id);
```

* **Note**: `(chunk_x, chunk_z)` is the primary key since chunks in the dedicated `idle_world` are unique. If multi-world support is enabled in the future, `world_name VARCHAR(64)` will be added to the composite primary key.

---

## 2.3 Production Nodes Table (`production_nodes`)
Stores active production facility instances placed on claimed territory.

```sql
CREATE TABLE IF NOT EXISTS production_nodes (
    node_id VARCHAR(36) PRIMARY KEY,
    owner_id VARCHAR(36) NOT NULL,
    node_type VARCHAR(32) NOT NULL,
    level INT DEFAULT 1,
    size_chunks INT DEFAULT 1,
    style_id VARCHAR(64) NOT NULL,
    last_calculated_time BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    CONSTRAINT fk_nodes_owner FOREIGN KEY (owner_id) REFERENCES players(player_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_nodes_owner ON production_nodes(owner_id);
```
* **`size_chunks`**: `1` for `1x1` single-chunk facilities (Lv 1-4), `4` for `2x2` multi-chunk facilities (Lv 5+).
* **`style_id`**: String reference to the YAML schematic name (e.g., `mining_t1_default`).

---

## 2.4 Node Storage Buffer Table (`node_storage`)
Stores resources currently buffered inside a production node before player collection.

```sql
CREATE TABLE IF NOT EXISTS node_storage (
    node_id VARCHAR(36) NOT NULL,
    resource_key VARCHAR(64) NOT NULL,
    quantity BIGINT DEFAULT 0,
    PRIMARY KEY (node_id, resource_key),
    CONSTRAINT fk_storage_node FOREIGN KEY (node_id) REFERENCES production_nodes(node_id) ON DELETE CASCADE
);
```

---

## 2.5 Player Resource Wallet Table (`player_resource`)
Stores the player's collected virtual resources across all nodes (prevents inventory clutter).

```sql
CREATE TABLE IF NOT EXISTS player_resource (
    player_id VARCHAR(36) NOT NULL,
    resource_key VARCHAR(64) NOT NULL,
    quantity BIGINT DEFAULT 0,
    PRIMARY KEY (player_id, resource_key),
    CONSTRAINT fk_wallet_player FOREIGN KEY (player_id) REFERENCES players(player_id) ON DELETE CASCADE
);
```

---

## 2.6 Node Exploration Table (`node_exploration`)
Tracks **per-node** exploration progression.

```sql
CREATE TABLE IF NOT EXISTS node_exploration (
    node_id VARCHAR(36) PRIMARY KEY,
    exploration_level INT DEFAULT 1,
    experience BIGINT DEFAULT 0,
    CONSTRAINT fk_exploration_node FOREIGN KEY (node_id) REFERENCES production_nodes(node_id) ON DELETE CASCADE
);
```

---

## 2.7 Worker Instances Table (`worker_instances`)
Stores individual worker instances owned by players.

```sql
CREATE TABLE IF NOT EXISTS worker_instances (
    worker_id VARCHAR(36) PRIMARY KEY,
    owner_id VARCHAR(36) NOT NULL,
    template_id VARCHAR(64) NOT NULL,
    assigned_node_id VARCHAR(36) NULL,
    level INT DEFAULT 1,
    experience BIGINT DEFAULT 0,
    CONSTRAINT fk_workers_owner FOREIGN KEY (owner_id) REFERENCES players(player_id) ON DELETE CASCADE,
    CONSTRAINT fk_workers_node FOREIGN KEY (assigned_node_id) REFERENCES production_nodes(node_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_workers_owner ON worker_instances(owner_id);
CREATE INDEX IF NOT EXISTS idx_workers_node ON worker_instances(assigned_node_id);
```
* **`template_id`**: Reference to `workers.yml` definition (`miner_rare_1`).
* **`assigned_node_id`**: If `NULL`, the worker is idle in the player's collection inventory.

---

# 3. Connection & Transaction Strategy

## 3.1 HikariCP Connection Pooling
* Minimum idle connections: `2` (SQLite) or `5` (MySQL).
* Maximum pool size: `10` (customizable in `config.yml`).
* Connection timeout: `5,000` ms.

## 3.2 Async Save Queue (`SaveQueueService`)
To eliminate database locks and write lag:
1. When a player collects resources or upgrades a node, the change is written immediately to `Caffeine Cache` (`Sync`).
2. A dirty flag or data payload is pushed to the `SaveQueueService` (`ConcurrentLinkedQueue`).
3. A scheduled background task flushes dirty records in batch every `5 seconds` using JDBC `addBatch()` and `executeBatch()` inside a single transaction (`Connection.setAutoCommit(false)`).

---

# Document References

Next document:

**07EventSystem.md**
