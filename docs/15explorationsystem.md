# 15 - Exploration System.md

# Branz.Idle Node Exploration System Specification

---

# 1. Exploration Philosophy & Per-Node Ownership

In Branz.Idle, **Exploration is strictly Per-Node** (`node_exploration` database table).

When a player builds multiple mining facilities across different territory chunks, Mining Node A and Mining Node B maintain completely separate `explorationLevel` and `experience` tracking.

## 1.1 Why Per-Node instead of Global/Player Progression?
1. **Strategic Specialization**: Players are incentivized to invest deeply into a flagship facility (`e.g., pushing one mine to Level 50 Ancient Depths for rare crystals`) while keeping secondary facilities at shallower depths for high-volume basic ore generation (`Surface Mine`).
2. **Long-Term Gameplay Sink**: Prevents players from unlocking rare drop tables once globally and instantly farming legendary items across 10 newly built Level 1 nodes.
3. **Clean Database Ownership**: Directly ties exploration progression to `node_id`, ensuring seamless cleanup when a node is demolished or converted.

---

# 2. Exploration Depth & Zone Unlocks

Every node begins at `exploration_level = 1`. As the node completes regular production cycles (`ResourceProducedEvent`), or when players assign workers to targeted exploration tasks, the node gains `exploration experience`.

When XP thresholds are reached (`ExplorationLevelUpEvent`), the node advances deeper into its profession zones:

| Exploration Level Range | Zone Tier Name | Primary Drop Table (`drop_tables.yml`) |
|---|---|---|
| **Lv 1 to Lv 19** | `Surface Depths` | Basic resources (`iron_ore`, `oak_log`, `raw_stone`) |
| **Lv 20 to Lv 49** | `Deep Caverns / Dense Forest` | Uncommon ores & woods (`silver_ore`, `hardwood`, `gold_nugget`) |
| **Lv 50 to Lv 79** | `Ancient Abysm / Primordial Grove` | Rare & Epic materials (`mithril_crystal`, `ancient_bark`, `pearl`) |
| **Lv 80 to Lv 100** | `Core of the World` | Legendary relics and top-tier crafting catalysts (`adamantite_core`) |

---

# 3. Rare Drops & Player Trading Loop

Basic resources (`iron_ore`, `oak_log`) generated in high volumes are primarily consumed as materials for upgrading node levels (`NodeService.upgradeNode`) and expanding territory (`TerritoryService`). They hold modest trade value between players.

**Rare Exploration Drops** (`EPIC` and `LEGENDARY` entries unlocked at `exploration_level >= 50`), however, form the core backbone of inter-player trading and economy:
* Rare crystals and ancient barks are required for Tier-5 (`2x2 multi-chunk complex`) structural schematic unlocks.
* Because high-level exploration requires weeks of dedicated idle progression and rare worker assignments, rare materials command high coin and diamond exchange values when players trade with one another.

---

# 4. Drop Table Calculation & Worker Synergy

When `ProductionService` completes a production cycle (`or offline catch-up calculation`), it queries `DropTableRegistry` using the node's `nodeType` and current `explorationLevel`.

```text
Production Cycle Completion
 ├── 1. Generate Base Yield (100% chance for basic resource: e.g., +50 Iron Ore)
 └── 2. Query DropTableRegistry for unlocked entries (min_exploration_level <= node.explorationLevel)
      ├── Base Roll Weight: e.g., Mithril Crystal = 5% base weight
      ├── Apply Assigned Worker Synergy: EffectiveWeight = BaseWeight * (1 + sum(WorkerRareDropBonus))
      └── If check passes -> Add rare item to NodeStorage buffer
```

## Worker `rareDropBonus` Synergy
Assigned workers with high `rareDropBonus` multipliers (`e.g., Epic Explorer worker with +150% rare drop chance`) directly amplify the probability weights of drawing `UNCOMMON`, `RARE`, `EPIC`, and `LEGENDARY` items from the active drop table during every single production tick.

---

# Document References

Next document:

**16EconomySystem.md**
