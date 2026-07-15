# 02 - Development Guideline.md

# Branz.Idle Development & Coding Guidelines

---

# 1. Package Structure & Modular Boundaries

To ensure strict modular encapsulation and maintainability, Branz.Idle follows a **Feature-Based Modular Package Structure**.

Developers must NOT organize classes horizontally across the entire plugin (`core.services`, `core.models`). Instead, classes must be grouped inside their respective feature module.

## 1.1 Root Package

```text
com.example.plugin
```

## 1.2 Module Overview

```text
com.example.plugin
├── bootstrap       # Plugin entry point, lifecycle, and ServiceRegistry
├── config          # YAML configuration loaders and registries
├── database        # Database connection pool (HikariCP) and base JDBC utilities
├── territory       # Chunk claiming, bounds, and ownership mechanics
├── protection      # Self-contained territory & structure protection listeners
├── node            # Production facilities, structure styles, and expansion logic
├── worker          # Worker instances, leveling, stats, and assignment
├── production      # Online ticker and offline delta calculation engine
├── storage         # Node storage buffers and player Virtual Resource Wallet
├── exploration     # Per-node exploration levels and rare drop unlocking
├── economy         # Coin/Diamond currencies, transaction auditing, and Gacha
├── onboarding      # RTP spawn, starter pack distribution, and tutorial flow
├── gui             # Inventory GUI controllers, builders, and pagination
├── visual          # Citizens NPC visual provider and navigation controllers
└── integration     # WorldEdit/FAWE structure provider and optional Vault bridge
```

## 1.3 Inside a Feature Module

Every feature module (e.g., `com.example.plugin.worker`) follows a consistent internal structure:

```text
com.example.plugin.worker
├── model           # Domain entities (WorkerInstance, WorkerStats)
├── service         # Service interface & implementation (WorkerService, WorkerServiceImpl)
├── repository      # Data access layer (WorkerRepository, WorkerRepositoryImpl)
└── event           # Custom Bukkit events published by this module (WorkerAssignedEvent)
```

* **Interface vs Implementation**: Service interfaces (e.g., `WorkerService`) are exposed directly inside the `service` package so other modules can consume them via `ServiceRegistry`. Implementations (`WorkerServiceImpl`) are package-private or hidden behind the registry.

---

# 2. Dependency Injection & Wiring Rules

1. **Use `ServiceRegistry` exclusively.** Do not use static singletons (`WorkerService.getInstance()`) or third-party DI frameworks (`Guice`/`Dagger`).
2. **Register during `onEnable` inside `bootstrap`:**
   ```java
   ServiceRegistry.register(WorkerService.class, new WorkerServiceImpl(workerRepository));
   ```
3. **Consume via `ServiceRegistry.get`:**
   ```java
   WorkerService workerService = ServiceRegistry.get(WorkerService.class);
   ```

---

# 3. Concurrency & Async Rules

## 3.1 Database Access
* **NEVER** run `SELECT`, `INSERT`, `UPDATE`, or `DELETE` synchronously on the Bukkit main thread.
* All reads from the database must occur inside an `CompletableFuture.supplyAsync(...)` or background thread pool.
* All writes to the database must pass through the async save queue (`SaveQueueService`).

## 3.2 Cache First
* Use **Caffeine Cache** (`PlayerCache`, `NodeCache`) to hold active player state.
* When a player joins (`PlayerJoinEvent`), load their data asynchronously into the cache.
* While online, all gameplay systems read synchronously from the Caffeine cache in `O(1)` time.
* When data mutates, update the in-memory cache synchronously and submit a save task to the async save queue.

## 3.3 Bukkit API Thread Safety
* Only touch Bukkit API (`Player`, `World`, `Inventory`, `Entity`) on the main thread.
* If an async calculation finishes and needs to update a GUI or spawn a particle, transition back to the main thread:
   ```java
   Bukkit.getScheduler().runTask(plugin, () -> {
       player.openInventory(gui.build());
   });
   ```

---

# 4. Data-Driven Development Rules

1. **No Hardcoded Numbers**: Costs, production rates, gacha odds, worker tiers, and item requirements MUST be read from YAML config files (`config.yml`, `workers.yml`, `nodes.yml`).
2. **Runtime Registries**: Upon plugin start (`or reload`), YAML definitions are parsed and loaded into immutable objects inside registries (`WorkerRegistry`, `NodeStyleRegistry`).
3. **String Key References**: Database tables store references to definitions via string keys (`worker_template_id = 'miner_t1'`), NOT hardcoded IDs or serialized config blobs.

---

# 5. External Integration Standards (Provider Pattern)

When interacting with external plugins (Citizens, WorldEdit/FAWE, Vault), always use a defensive check and fallback via Provider interfaces:

```java
public interface VisualProvider {
    void spawnWorkerVisual(WorkerInstance worker, Location target);
    void removeWorkerVisual(UUID workerId);
}
```

* During `onEnable`, check `Bukkit.getPluginManager().getPlugin("Citizens")`. If present and enabled, register `CitizensVisualProvider`. Otherwise, register `NoOpVisualProvider`.
* This guarantees the plugin **never throws ClassNotFoundException or NoClassDefFoundError** if optional dependencies are missing.

---

# 6. Error Handling & Logging

1. **Log Meaningful Context**: Always include Player ID, Node ID, or Worker ID in warning/error logs:
   ```java
   plugin.getLogger().warning("[Storage] Failed to add resource for Player " + playerId + ": Wallet capacity exceeded.");
   ```
2. **Never Swallow Exceptions**: If a database query fails inside an async thread, catch `SQLException`, log the stack trace, and notify administrators or retry gracefully.
3. **Player-Facing Error Messages**: User-facing errors (e.g., "Not enough coins", "Chunk already claimed") must be configurable via `messages.yml` and formatted cleanly in chat or sound effects.

---

# Document References

Next document:

**03TechStack.md**
