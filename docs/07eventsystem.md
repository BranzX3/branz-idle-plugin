# 07 - Event System.md

# Branz.Idle Event System Specification

---

# 1. Event-Driven Architecture

To ensure strict decoupling across our feature modules (`territory`, `node`, `worker`, `production`, `storage`, `onboarding`, `protection`), inter-module notification is driven by custom events extending Bukkit's `Event` base class (`org.bukkit.event.Event`).

* Events are published using Bukkit's event bus: `Bukkit.getPluginManager().callEvent(event)`.
* Event handlers are registered via `@EventHandler` inside module listeners.
* Asynchronous events (`isAsync() == true`) must never modify Bukkit main-thread state directly.

---

# 2. Domain Events Hierarchy

## 2.1 Onboarding & Player Events (`com.example.plugin.onboarding.event`)
* **`PlayerOnboardingCompletedEvent`**
  * **Fired**: Immediately when a new player completes the initial `/base` claim following their RTP spawn and receives their Starter Pack.
  * **Payload**: `UUID playerId`, `int starterChunksGranted`, `List<UUID> starterWorkerIds`.
  * **Listeners**: `ProtectionListener` (initializes claim permissions), `GUIController` (opens welcome guide).

---

## 2.2 Territory & Protection Events (`com.example.plugin.territory.event` & `protection.event`)
* **`TerritoryClaimedEvent`**
  * **Fired**: When a player successfully claims a chunk (`RESIDENTIAL` or `PRODUCTION`).
  * **Payload**: `UUID playerId`, `int chunkX`, `int chunkZ`, `ChunkType type`.
  * **Listeners**: `VisualService` (draws border particles), `SaveQueueService`.
* **`TerritoryAccessDeniedEvent`** (`Cancellable`)
  * **Fired**: When an unauthorized player attempts to break, place, or interact within a protected territory chunk.
  * **Payload**: `Player violator`, `UUID ownerId`, `Location targetLocation`, `String actionType`.
  * **Listeners**: Sends warning message/sound to the violator; blocks the action.

---

## 2.3 Production Node Events (`com.example.plugin.node.event`)
* **`NodeCreatedEvent`**
  * **Fired**: When a `ProductionNode` facility is established on a claimed chunk.
  * **Payload**: `ProductionNode node`.
  * **Listeners**: `StructureProvider` (pastes Lv 1 schematic), `ProductionService` (starts ticker loop).
* **`NodeLevelUpEvent`**
  * **Fired**: When a player upgrades a facility level.
  * **Payload**: `ProductionNode node`, `int oldLevel`, `int newLevel`.
  * **Listeners**: Checks for node expansion threshold (`Lv 5+`); triggers schematic repaste (`StructureProvider`).
* **`NodeExpandedEvent`**
  * **Fired**: When a node reaches Level 5+ and physically expands from `1x1` (1 chunk) to `2x2` (4 chunks).
  * **Payload**: `ProductionNode node`, `List<ChunkPosition> expandedChunks`.
  * **Listeners**: `TerritoryService` (converts adjacent residential chunks to production chunks), `StructureProvider` (pastes multi-chunk complex schematic).

---

## 2.4 Worker & Gacha Events (`com.example.plugin.worker.event`)
* **`WorkerGachaPulledEvent`**
  * **Fired**: When a worker is drawn from the Coin or Diamond Gacha pool.
  * **Payload**: `UUID playerId`, `WorkerInstance worker`, `GachaPoolType poolType`.
* **`WorkerAssignedEvent`**
  * **Fired**: When a worker is assigned to (`or unassigned from`) a production node.
  * **Payload**: `WorkerInstance worker`, `UUID previousNodeId`, `UUID newNodeId`.
  * **Listeners**: `VisualProvider` (spawns/removes Citizens NPC at node target), `ProductionService` (recalculates node production tick rates).
* **`WorkerLevelUpEvent`**
  * **Fired**: When a worker gains enough XP from active node work or exploration to level up.
  * **Payload**: `WorkerInstance worker`, `int oldLevel`, `int newLevel`, `WorkerStats newStats`.

---

## 2.5 Production & Wallet Events (`com.example.plugin.production.event` & `storage.event`)
* **`ResourceProducedEvent`**
  * **Fired**: Every time a node generates resources (online tick or offline catch-up).
  * **Payload**: `ProductionNode node`, `Map<String, Long> producedDelta`, `boolean isOfflineCatchUp`.
  * **Listeners**: `StorageService` (adds to `NodeStorage` buffer).
* **`ResourceCollectedEvent`**
  * **Fired**: When a player clicks "Collect All" or collects from a node GUI, transferring buffered resources into their Virtual Resource Wallet.
  * **Payload**: `UUID playerId`, `UUID nodeId`, `Map<String, Long> transferredResources`.
  * **Listeners**: `SaveQueueService` (persists new `player_resource` wallet balances and zeroes out `node_storage`).

---

# 3. Custom Event Code Template

All domain events follow a standardized structure:

```java
package com.example.plugin.node.event;

import com.example.plugin.node.model.ProductionNode;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class NodeLevelUpEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final ProductionNode node;
    private final int oldLevel;
    private final int newLevel;

    public NodeLevelUpEvent(ProductionNode node, int oldLevel, int newLevel) {
        this.node = node;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    public ProductionNode getNode() { return node; }
    public int getOldLevel() { return oldLevel; }
    public int getNewLevel() { return newLevel; }

    @NotNull
    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
```

---

# Document References

Next document:

**08RegistrySystem.md**
