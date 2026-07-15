# 14 - Visual System.md

# Branz.Idle Visual & Structure Presentation System

---

# 1. Visual Separation Architecture

In alignment with our **Engine Before Content** philosophy, all visual presentation elements (worker NPCs, architectural structures, floating holograms, particle borders) are completely isolated from the mathematical core engine (`production`, `storage`, `worker leveling`).

```text
+-----------------------------------------------------------------+
|                       CORE ENGINE LAYER                         |
|      (Production Calculations, Worker Stats, Node Levels)       |
+-----------------------------------------------------------------+
           |                                           |
           | WorkerAssignedEvent                       | NodeCreatedEvent / LevelUpEvent
           v                                           v
+-------------------------------------+ +---------------------------------+
|     VisualProvider (Interface)      | |  StructureProvider (Interface)  |
+-------------------------------------+ +---------------------------------+
     |                            |           |                      |
     v                            v           v                      v
CitizensVisualProvider   NoOpVisualProvider  FAWEStructureProvider  NoOpStructureProvider
(If Citizens installed)  (If missing)        (If FAWE installed)    (If missing)
```

If third-party plugins (`Citizens`, `FastAsyncWorldEdit`) are uninstalled or disabled, the engine safely registers `NoOp` implementations. Gameplay, resource yields, and database saving continue without throwing `ClassDefFoundError` or interrupting player progression.

---

# 2. Worker Visual Presentation (`VisualProvider`)

## 2.1 Owner-Only Scope & Server Optimization
To prevent hundreds of active workers from saturating server RAM and degrading TPS through simultaneous pathfinding across every claimed territory, worker NPCs are managed under strict **Owner-Only Visibility Rules**:

1. **Active Evaluation Trigger**: When `PlayerMoveEvent` (`debounced to chunk transitions`) or `PlayerJoinEvent` fires, `VisualService` calculates the player's distance to their owned `ProductionNode` chunks.
2. **Spawning Threshold (`<= 64 blocks`)**: If the node owner is online and within `64 blocks` of their facility:
   * `VisualProvider.spawnWorkerVisual(worker, nodeTargetLocation)` is called.
   * If Citizens is installed, an NPC is spawned at the node's work location using the worker's custom skin (`worker_template.citizens_skin`).
   * Simple work animations (`swinging arm, looking at target block`) are initiated on the Bukkit main thread.
3. **Despawning Threshold (`> 64 blocks` or Offline)**: When the owner walks away from their territory or logs out (`PlayerQuitEvent`), `VisualProvider.removeWorkerVisual(workerId)` is triggered instantly, destroying the Citizens NPC to release server memory.
4. **Visitor Isolation**: If Player B visits Player A's base while Player A is offline (`or far away`), Player B will see all buildings and schematic blocks, but **will see zero worker NPCs**.

---

# 3. Structure & Schematic Presentation (`StructureProvider`)

## 3.1 Architectural Schematic Pasting via FAWE
When a `ProductionNode` is created (`NodeCreatedEvent`) or upgraded (`NodeLevelUpEvent` / `NodeExpandedEvent`), `StructureService` instructs the `StructureProvider` to paste the architectural schematic defined in `nodes.yml`.

```yaml
mining_lv1:
  size_chunks: 1
  schematic: "mining_t1_default.schem" # 16x16 footprint
mining_lv5:
  size_chunks: 4
  schematic: "mining_t2_complex.schem" # 32x32 footprint (Multi-chunk expansion)
```

## 3.2 Asynchronous Pasting Pipeline (`FAWEStructureProvider`)
1. Schematic file is loaded from disk (`plugins/BranzIdle/schematics/<filename>`).
2. `FastAsyncWorldEdit` (`FAWE`) API is invoked on an async background thread to paste the clipboard contents into the target chunk coordinates inside `idle_world`.
3. Because FAWE operates outside the main Bukkit tick loop, pasting even massive `32x32` Level 5 multi-chunk complexes causes **0 ms main-thread lag**.
4. Once completed, all pasted blocks inherit the `owner_id` and `node_id` protection rules enforced by the `protection` module.

## 3.3 Fallback Marker Placements (`NoOpStructureProvider`)
If WorldEdit / FAWE is not installed on the server, `NoOpStructureProvider` executes a lightweight fallback on the Bukkit main thread:
* Places a `BEACON` (`or bedrock/lodestone marker block`) at the center of the node chunk (`Y+1`).
* Spawns floating holographic text above the marker (`DecentHolograms or Paper Display Entity bridge`):
  * `&6&l[Mining Facility Lv 4]`
  * `&fStatus: &aProducing Iron Ore`
  * `&fWorkers: &e3 / 3 Assigned`

---

# 4. Territory Boundary & Particle Visuals

When a player enters `or claims` a territory chunk (`TerritoryClaimedEvent`), `VisualService` renders temporary boundary particles (`org.bukkit.Particle.VILLAGER_HAPPY` or `FLAME`) along the `16x16` (`or 32x32`) perimeter blocks for `5 seconds` (`using only Bukkit Main Thread packets sent exclusively to the owner and nearby players`).

---

# Document References

Next document:

**15ExplorationSystem.md**
