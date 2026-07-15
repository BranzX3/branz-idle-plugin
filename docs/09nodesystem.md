# 09 - Node System.md

# Branz.Idle Production Node System

---

# 1. Node Philosophy & Chunk Conversion

A **Production Node** (`ProductionNode`) is an autonomous resource-generating facility established inside a player's claimed territory in the dedicated `idle_world`.

When a player first claims territory (`/base`), the chunks are designated as **`RESIDENTIAL`** (central base, decoration, or buffer zone). To begin idle production, the player selects a residential chunk and converts it into a **`PRODUCTION`** chunk by creating a Node (`Mining Facility`, `Lumber Mill`, `Fishing Camp`, etc.).

---

# 2. Node Footprint & Size Expansion Mechanics

To solve the visual density and structure size constraints of a standard 16x16 Minecraft chunk, Branz.Idle implements a dynamic **Node Size Expansion** system:

## 2.1 Single-Chunk Footprint (Lv 1 to Lv 4)
* **Footprint**: `1x1` Minecraft Chunk (`16x16 blocks`).
* **Territory Requirement**: Exactly `1` owned `RESIDENTIAL` chunk.
* **Capacity**: Supports basic Level 1-4 schematics and up to `2` assigned workers.

---

## 2.2 Multi-Chunk Complex Footprint (Lv 5+)
When a node is upgraded from Level 4 to **Level 5**, it expands into a **Multi-Chunk Complex**:
* **Footprint**: `2x2` Minecraft Chunks (`32x32 blocks`).
* **Territory Requirement**: The player must own `3 adjacent chunks` forming a clean `2x2` grid (`Origin Chunk + East + South + South-East`). If any of the 3 adjacent chunks are unowned or belong to another node/player, the Lv 5 upgrade is blocked with a clear GUI prompt (`"Requires 3 adjacent Residential chunks to expand into a 2x2 complex!"`).
* **Conversion**: Upon upgrading to Level 5, the 3 adjacent residential chunks are updated in `territory_chunks` with `chunk_type = 'PRODUCTION'` and `node_id = origin_node_id`.
* **Capacity**: Supports massive `32x32` WorldEdit/FAWE schematics, up to `10` assigned workers, and significantly larger storage buffers.

---

# 3. Node Types & Professions

The engine natively supports modular node types registered via `nodes.yml`:

* **`MINING`**: Generates ores, stones, and crystals (`iron_ore`, `coal`, `mithril`).
* **`LUMBER`**: Generates wood, saplings, and rare bark (`oak_log`, `ancient_wood`).
* **`FISHING`**: Generates aquatic resources and sunken relics (`golden_fish`, `pearl`).
* **`FARMING` / `RANCHING`** *(Future Modules)*: Configurable via external module definitions without core engine changes.

---

# 4. Node Upgrades & Level Progression

Nodes can be upgraded from Level `1` up to Level `100`. Each upgrade requires currencies and collected resources defined in `NodeRegistry` (`nodes.yml`):

```yaml
mining_lv1:
  node_type: MINING
  size_chunks: 1
  schematic: "mining_t1_default.schem"
  upgrade_cost:
    coins: 1000
    iron_ore: 50
  worker_slots: 2
  max_storage_capacity: 5000

mining_lv5:
  node_type: MINING
  size_chunks: 4 # 2x2 Multi-Chunk expansion
  schematic: "mining_t2_complex.schem"
  upgrade_cost:
    coins: 25000
    diamonds: 50
    iron_ore: 2000
  worker_slots: 5
  max_storage_capacity: 50000
```

## Lifecycle of a Node Upgrade (`NodeService.upgradeNode`)
1. Player clicks "Upgrade Node" inside the Node GUI (`GUIController`).
2. `NodeService` verifies that the player has sufficient Coins/Diamonds in `PlayerProfile` and required items in `PlayerResourceWallet`.
3. If upgrading to Level 5 (`Expansion Threshold`), `NodeService` queries `TerritoryService` to verify the `2x2` chunk availability.
4. Currencies and resources are deducted asynchronously (`SaveQueueService`).
5. `NodeLevelUpEvent` (and optionally `NodeExpandedEvent`) is published.
6. `StructureProvider` (`FAWE`) asynchronously clears the old `16x16` or `32x32` area and pastes the new architectural `.schem` file.

---

# 5. Structure Styles & Community Building Contests

Because Branz.Idle uses `WorldEdit / FAWE` `.schem` files for node structures, server administrators can host **Community Building Contests**:

1. Players design custom `16x16` (Lv 1-4) or `32x32` (Lv 5+) mining facilities or lumber mills on a creative build server.
2. Admins export winning structures using WorldEdit (`//copy` -> `//schem save mining_t1_elfin.schem`).
3. The schematic is copied into `plugins/BranzIdle/schematics/` and added to `nodes.yml` under a new style key.
4. Players can unlock these alternate visual styles via exploration rewards, Gacha, or diamond purchases, and apply them instantly in the Node Customization GUI.

---

# Document References

Next document:

**10WorkerSystem.md**
