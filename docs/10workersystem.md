# 10 - Worker System.md

# Branz.Idle Worker System Specification

---

# 1. Worker Philosophy & Overview

Workers (`WorkerInstance`) are collectible characters assigned to `ProductionNode` facilities to automate and accelerate resource generation.

Workers represent the primary collectible and optimization vector in Branz.Idle. Players acquire workers through the **Starter Pack** upon joining or by pulling from **Gacha Pools** using Coins and Diamonds.

---

# 2. Starter Workers (Onboarding Flow)

To ensure that every new player can immediately experience the core idle loop without grinding for Gacha currency, the engine grants **3 Free Starter Workers** during initial onboarding:

1. When a player completes their first `/base` claim after RTP (`PlayerOnboardingCompletedEvent`), `WorkerService` queries the `WorkerRegistry`.
2. Three Tier-1 (`COMMON`) templates matching the starter node types (`MINING`, `LUMBER`, `FISHING`) are instantiated as new `WorkerInstance` records.
3. The instances are saved via `SaveQueueService` and added to the player's unassigned worker collection, ready to be placed into their 4 free chunks immediately.

---

# 3. Worker Visual Scope (Owner-Only Visibility)

## 3.1 The Server Performance Challenge
If every assigned worker across hundreds of player territories was spawned as a permanent Citizens NPC in the world 24/7, server RAM would saturate rapidly and TPS would drop dramatically due to pathfinding and entity tracking overhead.

## 3.2 Owner-Only Visibility Rule (`VisualService` & `VisualProvider`)
To maintain 20 TPS even on massive MMO servers, Branz.Idle enforces strict **Owner-Only Visual Spawning**:

1. **Only When Owner is Online**: When a player is offline, **zero** Citizens NPCs exist for their workers. Their production calculation runs purely in mathematical state (`WorkerStats`).
2. **Only When Owner is Nearby**: When a player is online, a lightweight periodic check (`or PlayerMoveEvent chunk border trigger`) evaluates their distance to their claimed nodes.
   * If the owner enters within `64 blocks` (configurable) of their `ProductionNode`, `VisualProvider.spawnWorkerVisual(worker, nodeTarget)` is called. Citizens spawns an NPC using the worker's custom skin and plays simple work animations (e.g., swinging a pickaxe at an ore block).
   * If the owner moves further than `64 blocks` away or disconnects (`PlayerQuitEvent`), `VisualProvider.removeWorkerVisual(workerId)` despawns the NPC instantly.
3. **No Visitor NPCs**: If Player B walks through Player A's territory while Player A is offline (`or far away`), Player B will see Player A's structures and architectural schematics, but **will not see any worker NPCs**. This ensures optimal client FPS and server memory conservation.

---

# 4. Worker Attributes & Stat Scaling

Every worker possesses three core multipliers calculated from their template (`WorkerRegistry`) plus their current level (`1` to `100`):

```java
public record WorkerStats(
    double productionSpeedBonus, // Multiplier reducing tick intervals (e.g., 1.25x = 20% faster ticks)
    double resourceYieldBonus,   // Multiplier increasing resource output per tick (e.g., 1.50x = +50% ore)
    double rareDropBonus         // Multiplier boosting probabilities of rare items in DropTableRegistry
) {}
```

## Stat Calculation Formula
When a worker levels up (`WorkerLevelUpEvent`), their runtime `WorkerStats` are recalculated:
$$\text{Stat}_{\text{current}} = \text{BaseStat}_{\text{template}} \times (1 + (\text{GrowthRate}_{\text{template}} \times (\text{Level} - 1)))$$

---

# 5. Worker Leveling & Experience

Workers earn XP through two primary channels:

## 5.1 Production Work XP
* Every time a node completes a production cycle (`ResourceProducedEvent`), all workers assigned (`assignedNodeId == nodeId`) receive base experience (`e.g., +10 XP per tick`).
* During **Offline Catch-up Delta Calculation**, workers receive proportional accumulated XP (`e.g., 12 hours offline = +7,200 XP`), allowing workers to level up automatically while the player sleeps.

## 5.2 Exploration Task XP
* When an assigned node discovers a new exploration tier or completes a rare exploration roll, assigned workers gain a large bonus XP burst (`e.g., +500 XP`), accelerating their progression toward Max Level 100.

---

# 6. Assignment & Unassignment Constraints

1. **Single Node Assignment**: A `WorkerInstance` can only be assigned (`assignedNodeId`) to **one** `ProductionNode` at a time.
2. **Node Slot Limits**: Each `ProductionNode` has a maximum worker slot capacity (`worker_slots`) defined by its Level in `nodes.yml`. For example, Level 1 allows `2 workers`; Level 5 (`2x2 multi-chunk`) allows `5 workers`.
3. **Instant Swapping**: Players can assign or unassign workers cleanly via the Worker GUI (`GUIController`). If a worker is unassigned mid-tick, their stat bonus is immediately removed from the node's production rate calculations (`ProductionService`).

---

# Document References

Next document:

**11ProductionSystem.md**
