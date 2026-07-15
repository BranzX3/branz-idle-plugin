# 03 - Tech Stack.md

# Branz.Idle Technology Stack & Dependencies

---

# 1. Core Platform & Language

## Java 25
* **Target Version**: Java 25 (`sourceCompatibility = '25'`, `targetCompatibility = '25'`)
* **Features Leveraged**:
  * Pattern Matching for `switch` and `instanceof`
  * Records for immutable DTOs and value carriers (`WorkerStats`, `NodePosition`)
  * Sealed Classes for strict domain hierarchies (`ResourceItem`, `WorkerRarity`)
  * Virtual Threads / Modern Concurrency utilities where applicable in async thread pools

## Paper API 26.2+
* **Target Platform**: Paper API (`io.papermc.paper:paper-api:26.2.build.+`)
* **Why Paper**: High-performance async chunk loading, Folia-ready scheduling structures, modern Adventure API for rich text formatting and GUIs, and robust block/entity handling.

---

# 2. Database & Data Persistence

## SQLite / MySQL (Via JDBC & HikariCP)
* **Connection Pool**: HikariCP for high-performance, concurrent connection management.
* **Database Engine**:
  * **SQLite**: Default for out-of-the-box local server setups without external database requirements.
  * **MySQL / MariaDB**: Production recommendation for multi-instance or high-concurrency MMO servers.
* **Data Layer Style**: Pure JDBC with customized `Repository` implementations.
* **Why No ORM / Hibernate**: Relational ORMs introduce heavy memory overhead and complex lifecycle issues in Minecraft plugins. Pure JDBC with direct prepared statements ensures minimal latency and precise query control.

---

# 3. Configuration & Serialization

## Bukkit YAML API (`org.bukkit.configuration.file.YamlConfiguration`)
* **Config Engine**: Built-in Bukkit SnakeYAML wrappers (`config.yml`, `workers.yml`, `nodes.yml`, `messages.yml`).
* **Why No Jackson**: Jackson Databind and Jackson YAML add significant jar size and external dependencies. Since Paper natively provides high-performance YAML parsing (`YamlConfiguration`) and our domain definitions are structured into simple DTOs upon plugin start, Jackson is completely redundant and omitted to keep the build lightweight.

---

# 4. In-Memory Caching

## Caffeine Cache (`com.github.ben-manes.caffeine:caffeine:3.1.8`)
* **Purpose**: High-performance, concurrent in-memory caching for player data, node states, and active workers.
* **Configuration**:
  * Automatic time-based expiration for offline player caches (`expireAfterAccess(30, TimeUnit.MINUTES)`).
  * Maximum size bounding (`maximumSize(10_000)`).
  * Synchronous read-through for Bukkit main thread (`O(1)` access), paired with write-behind persistence to the asynchronous database save queue.

---

# 5. External Providers (Soft-Dependencies)

Branz.Idle relies on external plugins to enhance visualization and structure generation. All external plugins are **Soft-Dependencies** (`softdepend` in `plugin.yml`). If missing, the core gameplay simulation functions normally using internal fallback providers (`NoOpProvider`).

## 5.1 Citizens (Visual Provider)
* **Plugin**: Citizens 2 (`net.citizensnpcs:citizens-main`)
* **Role**: Spawns and animates worker NPCs inside claimed node territories when the owner is online and nearby.
* **Fallback**: If Citizens is not installed, `NoOpVisualProvider` is used. Worker stats, leveling, and production calculation continue without visual NPCs.

## 5.2 FastAsyncWorldEdit / WorldEdit (Structure Provider)
* **Plugin**: FastAsyncWorldEdit (`com.fastasyncworldedit:FastAsyncWorldEdit-Core` / `com.sk89q.worldedit:worldedit-bukkit`)
* **Role**: Pastes architectural schematic files (`.schem`) into node chunks asynchronously when nodes are created or upgraded to multi-chunk levels (`2x2`).
* **Fallback**: If FAWE/WorldEdit is missing, `NoOpStructureProvider` places simple holographic markers and center blocks without interrupting territory or production logic.

## 5.3 Vault (Optional Economy Bridge)
* **Plugin**: Vault (`com.github.MilkBowl:VaultAPI:1.7`)
* **Role**: Optional bridge to allow external server plugins to interact with Branz.Idle Coin balances.
* **Architecture**: Branz.Idle maintains its own internal currency database tables (`player_resource` / `coins`). Vault is merely hooked if the server owner enables `vault-sync` in `config.yml`.

---

# 6. Build & Development Tools

## Gradle & `run-paper` Plugin
* **Build System**: Gradle 8.x + Kotlin DSL or Groovy DSL (`build.gradle`).
* **Test Environment**: `xyz.jpenilla.run-paper` plugin (`version '2.3.1'`) for automated server provisioning (`minecraftVersion("26.2")`) during development.
* **Shadow Jar**: `com.github.johnrengelman.shadow` for bundling Caffeine and HikariCP cleanly inside the final plugin jar (`branz-idle-plugin.jar`).

---

# 7. Summary Dependency Table

| Library / Plugin | Version / Range | Type | Purpose |
|---|---|---|---|
| Paper API | `26.2.build.+` | `compileOnly` | Core Minecraft server engine & API |
| HikariCP | `5.1.0+` | `implementation` (Shadowed) | JDBC Database Connection Pooling |
| Caffeine | `3.1.8+` | `implementation` (Shadowed) | High-performance memory caching |
| SQLite JDBC | `3.45.+` | `implementation` (Shadowed) | Local embedded database driver |
| Citizens API | `2.0.35+` | `compileOnly` (`softdepend`) | Worker NPC visual entity spawning |
| WorldEdit / FAWE | `2.11.+` | `compileOnly` (`softdepend`) | `.schem` structure pasting |
| Vault API | `1.7+` | `compileOnly` (`softdepend`) | Optional economy synchronization |

---

# Document References

Next document:

**05DomainModel.md** *(Note: 04ProjectStructure.md has been merged into 18ProjectStructure.md)*
