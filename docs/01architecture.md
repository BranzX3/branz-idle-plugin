# 01 - Architecture.md

# Branz.Idle Architecture Specification

---

# 1. Architectural Philosophy

Branz.Idle is engineered as a **Feature-Based Modular Engine** that operates inside Minecraft (Paper API).

The system strictly enforces separation between four primary layers:

1. **Bootstrap Layer** – Plugin lifecycle, ServiceRegistry initialization, and provider detection.
2. **Feature & Core Modules Layer** – Independent domain modules containing Models, Services, and Repositories.
3. **Infrastructure Layer** – Database persistence, YAML configuration loading, and external integrations.
4. **Presentation Layer** – Inventories (GUIs), Holograms, Visual Entities (Citizens), and Schematics (WorldEdit/FAWE).

---

# 2. High-Level Architecture Diagram

```text
+-------------------------------------------------------------------------+
|                            BOOTSTRAP LAYER                              |
|           Plugin Main | ServiceRegistry | Provider Detection            |
+-------------------------------------------------------------------------+
                                     |
                                     v
+-------------------------------------------------------------------------+
|                      FEATURE & CORE MODULES LAYER                       |
|                                                                         |
|  [Territory]    [Protection]    [Node]        [Worker]    [Production]  |
|  [Storage]      [Exploration]   [Economy]     [Gacha]     [Onboarding]  |
|                                                                         |
|  * Each module encapsulates its own Models, Services & Repositories     |
|  * Inter-module communication strictly via Service Interfaces & Events  |
+-------------------------------------------------------------------------+
            |                                             |
            v                                             v
+---------------------------------------+ +-------------------------------+
|         INFRASTRUCTURE LAYER          | |      PRESENTATION LAYER       |
|                                       | |                               |
| * Database (SQLite / MySQL JDBC)      | | * Inventory GUI Controllers   |
| * YAML Config & Registries            | | * Visual Provider (Citizens)  |
| * Async Save Queue & Thread Pool      | | * Structure Provider (FAWE)   |
| * External Bridges (Vault Optional)   | | * Command System              |
+---------------------------------------+ +-------------------------------+
```

---

# 3. Core Systems (Feature Modules)

Every gameplay system is encapsulated inside its own feature module (`com.example.plugin.<feature>`):

* **Territory Module (`territory`)**: Manages chunk claims inside the dedicated world (`idle_world`), expansion bounds, and ownership tracking.
* **Protection Module (`protection`)**: Self-contained event listeners ensuring blocks, entities, and structures inside claimed chunks cannot be broken or grieved by others.
* **Node Module (`node`)**: Manages production facilities, node levels, multi-chunk expansion (e.g., 2x2 chunks at Lv5+), and structure styles.
* **Worker Module (`worker`)**: Manages worker instances, leveling (Max Lv 100), stats, assignments, and visual spawning conditions (owner-only visibility).
* **Production Module (`production`)**: Handles time-based resource generation calculation for both online ticks and offline catch-up delta calculation.
* **Storage & Wallet Module (`storage`)**: Manages node storage buffers and the player's Virtual Resource Wallet (`player_resource`).
* **Exploration Module (`exploration`)**: Tracks per-node exploration levels and unlocks rare drop tables.
* **Economy & Gacha Module (`economy`)**: Handles Coin/Diamond currencies, transaction auditing, and worker gacha pool mechanics.
* **Onboarding Module (`onboarding`)**: Manages RTP spawn, starter pack distribution (4 free chunks + 3 workers + starter coins), and tutorial flow.

---

# 4. Core Design Patterns

## 4.1 Feature-Based Modular Packaging

Instead of grouping classes horizontally by layer across the entire plugin (`core.services.*`, `core.models.*`), code is organized by feature domain (`node.model`, `node.service`, `node.repository`).

This guarantees:
* Clear boundaries and ownership per gameplay mechanic.
* Easy horizontal scaling when new systems are added.
* Reduced merge conflicts and tight encapsulation.

---

## 4.2 Manual ServiceRegistry Pattern

To avoid external heavy Dependency Injection frameworks while maintaining decoupling, the engine utilizes a centralized **`ServiceRegistry`**:

* Services are instantiated and registered during plugin startup (`onEnable`).
* Modules retrieve dependency interfaces via `ServiceRegistry.get(WorkerService.class)`.
* Guarantees deterministic initialization ordering and simple mock injection during unit testing.

---

## 4.3 Provider Pattern (External Soft-Dependencies)

Presentation elements depend on third-party plugins that may not always be present or loaded. The engine abstracts them using Provider interfaces:

### Visual Provider (Citizens Soft-Depend)
* `VisualProvider` interface abstracts NPC spawning and navigation.
* If Citizens is installed, `CitizensVisualProvider` spawns workers visually when the owner is online and nearby.
* If Citizens is missing, a `NoOpVisualProvider` is used—production and worker logic run seamlessly without crashes.

### Structure Provider (WorldEdit/FAWE Soft-Depend)
* `StructureProvider` interface abstracts pasting architectural schematics `.schem`.
* If WorldEdit / FastAsyncWorldEdit (FAWE) is installed, `FAWEStructureProvider` pastes node upgrades asynchronously.
* If missing, `NoOpStructureProvider` logs a warning or places simple marker blocks without breaking domain logic.

---

## 4.4 Data-Driven Registry Pattern

All content definitions (`worker_template`, `resource`, `drop_table`, `node_style`, `gacha_pool`) are loaded from YAML configuration files into memory-cached `Registries` (`ResourceRegistry`, `WorkerRegistry`, etc.).
The relational database stores **only player instance data and state**.

---

# 5. Threading & Concurrency Model

Minecraft Server runs on a single main thread at 20 TPS. Heavy operations must never block the main thread.

```text
+-----------------------------------------------------------------------+
|                         BUKKIT MAIN THREAD                            |
|                                                                       |
| * Inventory GUI Clicks & Updates                                      |
| * Entity Spawning & Navigation (Visual NPCs)                          |
| * Block Updates & Marker Placements                                   |
| * Command Handling                                                    |
| * Lightweight Production Ticker (Interval trigger)                    |
+-----------------------------------------------------------------------+
                                   |
              (Async Dispatch)     |    (Sync Callback / Event)
                                   v
+-----------------------------------------------------------------------+
|                      ASYNC THREAD POOL & WORKERS                      |
|                                                                       |
| * SQLite / MySQL Database Queries & Save Queue Batching               |
| * Heavy Offline Catch-up Production Calculations                      |
| * YAML Configuration Loading & Registry Parsing                       |
| * Schematic File Reading & FAWE Async Pasting                         |
+-----------------------------------------------------------------------+
```

## Rules of Concurrency
1. **Never touch Bukkit API from an Async thread.** (No opening GUIs, spawning entities, or altering blocks directly outside FAWE async API).
2. **Never execute SQL queries synchronously on the Main thread.** All database reads and writes must pass through the `Async Save Queue` or background thread pool.
3. **Caffeine Cache layer bridges Sync and Async.** Hot player and node states are cached in memory using Caffeine (`PlayerCache`, `NodeCache`) for instant synchronous reads on the main thread, with async write-behind persistence.

---

# 6. Inter-Module Communication

When a module needs to notify others of state changes, it publishes Bukkit events or custom domain events rather than direct cross-calling where appropriate:

* `TerritoryClaimedEvent` -> triggers `OnboardingService` and `ProtectionListener`.
* `NodeLevelUpEvent` -> triggers `StructureProvider` (FAWE paste) and `ExplorationService`.
* `WorkerAssignedEvent` -> triggers `VisualProvider` (Citizens NPC spawn) and `ProductionService` recalculation.

---

# Document References

Next document:

**02Development-Guideline.md**
