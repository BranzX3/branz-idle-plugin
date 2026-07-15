# 05 - Domain Model.md

# Branz.Idle Core Domain Model

---

# 1. Domain Architecture Overview

The Branz.Idle engine models gameplay entities around a clear hierarchy of ownership, location, and functional state:

```text
Player (Account & Virtual Wallet)
 └── Territory (Claimed Chunks in idle_world)
      ├── Residential Chunk (Hub / Decoration / No Production)
      └── Production Node (Mining / Lumber / Fishing Facility)
           ├── Node Size & Expansion (1 Chunk at Lv1 -> 2x2 Chunks at Lv5+)
           ├── Node Storage (Local buffer for produced resources)
           ├── Node Exploration (Per-Node tier & zone unlock state)
           └── Assigned Workers (WorkerInstance list)
```

---

# 2. Core Entities

## 2.1 PlayerDomain (`PlayerProfile`)
Represents the player's overarching profile inside the idle engine.
* **`playerId`** (`UUID`): Unique Minecraft player UUID.
* **`coins`** (`double`): Standard in-game earned currency used for basic upgrades and regular Gacha.
* **`diamonds`** (`long`): Premium or rare currency used for speedups and special Gacha pools.
* **`wallet`** (`PlayerResourceWallet`): Virtual inventory containing all collected resources across all nodes.
* **`onboardingCompleted`** (`boolean`): Flag indicating whether the player has completed the initial RTP + Starter Pack claim flow.

---

## 2.2 Territory & Claimed Chunks (`ChunkClaim`)
Represents a single 16x16 block area inside the dedicated `idle_world`.
* **`chunkX`**, **`chunkZ`** (`int`): Minecraft chunk coordinates.
* **`ownerId`** (`UUID`): ID of the player who owns this chunk.
* **`chunkType`** (`ChunkType`): Enum (`RESIDENTIAL`, `PRODUCTION`).
  * `RESIDENTIAL`: Base area for player decoration, central hub, or buffer space. Produces no resources.
  * `PRODUCTION`: Dedicated to a specific `ProductionNode`.
* **`protectionFlags`** (`ProtectionFlags`): Self-contained permissions (block break/place, mob spawning, explosions).

---

## 2.3 Production Node (`ProductionNode`)
Represents an active resource-producing facility placed on claimed chunks.
* **`nodeId`** (`UUID`): Unique node instance ID.
* **`ownerId`** (`UUID`): Player who owns this facility.
* **`nodeType`** (`NodeType`): Profession type (`MINING`, `LUMBER`, `FISHING`, etc.).
* **`level`** (`int`): Current facility level (1 to 100).
* **`sizeChunks`** (`NodeSize`): Physical footprint inside the world.
  * **Lv 1 to 4 (`SINGLE_1X1`)**: Occupies exactly 1 chunk (`16x16`).
  * **Lv 5+ (`MULTI_2X2`)**: Expands to occupy a `2x2` grid of chunks (`32x32`), requiring 3 adjacent owned residential chunks to merge.
* **`nodeStyleId`** (`String`): Reference to the active WorldEdit schematic definition (`mining_t1_rustic`, etc.).
* **`storage`** (`NodeStorage`): Local buffer storing generated resources until the player collects them.
* **`explorationLevel`** (`int`): **Per-Node** exploration progression determining deeper zone drops (e.g., surface vs deep cave).
* **`assignedWorkerIds`** (`List<UUID>`): List of active worker UUIDs operating inside this facility.

---

## 2.4 Worker Instance (`WorkerInstance`)
Represents an individual worker owned by a player.
* **`workerId`** (`UUID`): Unique instance ID.
* **`ownerId`** (`UUID`): Owner UUID.
* **`templateId`** (`String`): Reference to the YAML registry template (`miner_common_1`).
* **`assignedNodeId`** (`UUID`): Nullable reference to the node currently assigned to.
* **`level`** (`int`): Current worker level (`1` to `100`).
* **`experience`** (`long`): Current XP accumulated toward the next level.
* **`stats`** (`WorkerStats`): Calculated runtime attributes:
  * `productionSpeedBonus` (`double`): Multiplier reducing node tick intervals.
  * `resourceYieldBonus` (`double`): Multiplier increasing base output quantity.
  * `rareDropBonus` (`double`): Multiplier improving the chance of rolling rare exploration table drops.

---

## 2.5 Storage Buffers & Virtual Wallet

### Node Storage (`NodeStorage`)
A local buffer attached to a specific `ProductionNode`.
* **`nodeId`** (`UUID`): Parent node.
* **`resourceBuffer`** (`Map<String, Long>`): Map of resource key (`iron_ore`, `oak_log`) to quantity stored.
* **`maxCapacity`** (`long`): Maximum storage limit scaled by node level and assigned worker stats.
* **`lastCalculatedTime`** (`long`): Epoch timestamp in milliseconds of the last production tick or offline catch-up.

### Player Resource Wallet (`PlayerResourceWallet`)
The player's centralized virtual wallet (backed by `player_resource` database table).
* **`playerId`** (`UUID`): Wallet owner.
* **`resources`** (`Map<String, Long>`): Total accumulated resources collected from all node storages.
* **Purpose**: Prevents Minecraft physical inventory clutter (`64 stack limits`) and serves as the primary currency/material source for node upgrades and structure crafting.

---

## 2.6 Onboarding Starter Pack (`StarterPack`)
Defines the initial assets granted to a player upon first chunk claim (`/base` after RTP):
* **`freeChunksGranted`** (`int`): Exactly 4 chunks (1 Residential Hub + 3 basic Nodes: Mining, Lumber, Fishing).
* **`starterWorkers`** (`List<String>`): 3 randomly selected Tier-1 worker template keys added directly to the player's collection.
* **`starterCoins`** (`double`): 1,000 Coins to kickstart early level upgrades.

---

# 3. Domain Relationships & Integrity Rules

1. **One Node per Chunk Area**: A `1x1` node owns 1 `ChunkClaim`. A `2x2` node owns 4 `ChunkClaim` records (`chunkX/chunkZ`). Two nodes cannot overlap.
2. **Adjacent Territory Expansion**: A player cannot claim a disconnected chunk across the world. All territory claims must share at least one edge (`North`, `South`, `East`, `West`) with an existing claim belonging to that player.
3. **Worker Assignment Constraint**: A `WorkerInstance` can only be assigned to (`assignedNodeId`) **one** `ProductionNode` at any given time. The node must belong to the same `ownerId`.
4. **Per-Node Exploration**: Exploration depth (`explorationLevel`) belongs strictly to the `ProductionNode` instance (`node_exploration`), encouraging players to develop unique specialized mines or forests rather than unlocking everything globally.

---

# Document References

Next document:

**06Database.md**
