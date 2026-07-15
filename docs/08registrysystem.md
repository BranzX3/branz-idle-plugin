# 08 - Registry System.md

# Branz.Idle Data-Driven Registry System

---

# 1. Registry Philosophy

In accordance with our **Data-Driven Design** philosophy, no static gameplay balance or content definitions (`worker_template`, `resource`, `drop_table`, `node_style`, `gacha_pool`) are ever hardcoded in Java classes or stored inside relational database tables (`SQLite`/`MySQL`).

Instead, all definitions reside strictly inside YAML configuration files under `plugins/BranzIdle/config/`. When the plugin boots up (`or when /idle reload is executed`), these YAML files are parsed into immutable DTO instances and stored in high-performance in-memory `Registries`.

Database records only store the string key (`template_id = 'miner_t1'`) which looks up the full definition in `O(1)` time from the registry.

---

# 2. Core Registries Overview

All registries implement the common `ConfigRegistry<T>` interface and are managed by the `RegistryManager` inside the `config` module.

```text
RegistryManager
 ├── ResourceRegistry      <-- resources.yml (All game resources, display items, rarity)
 ├── WorkerRegistry        <-- workers.yml (Worker templates, base stats, growth rates)
 ├── NodeRegistry          <-- nodes.yml (Facility types, upgrade costs, structure schematics)
 ├── DropTableRegistry     <-- drop_tables.yml (Exploration drop tables & probabilities)
 └── GachaRegistry         <-- gacha.yml (Coin/Diamond pools, weights, rates)
```

---

# 3. Registry Specifications

## 3.1 Resource Registry (`ResourceRegistry`)
Loads resource definitions from `resources.yml`.
* **Key (`String`)**: Unique resource identifier (`iron_ore`, `oak_log`, `golden_fish`).
* **Attributes**:
  * `displayName` (`String`): Formatted name (`&bIron Ore`).
  * `material` (`org.bukkit.Material`): Bukkit material used for GUI display icons (`IRON_ORE`).
  * `customModelData` (`int`): Optional CustomModelData ID for custom resource textures.
  * `rarity` (`ResourceRarity`): `COMMON`, `UNCOMMON`, `RARE`, `EPIC`, `LEGENDARY`.

```yaml
# resources.yml example
iron_ore:
  display_name: "&fIron Ore"
  material: IRON_ORE
  custom_model_data: 1001
  rarity: COMMON
```

---

## 3.2 Worker Template Registry (`WorkerRegistry`)
Loads worker definitions from `workers.yml`.
* **Key (`String`)**: Template identifier (`miner_common_1`).
* **Attributes**:
  * `displayName`: `&aApprentice Miner`
  * `profession`: `MINING`
  * `rarity`: `COMMON`
  * `baseStats`: Base speed bonus, yield bonus, and rare drop chance.
  * `growthPerLevel`: Multiplier gained per level up (`1` to `100`).
  * `citizensSkin`: Player name or texture URL for Citizens visual NPC skin.

---

## 3.3 Node & Schematic Registry (`NodeRegistry`)
Loads node structures and upgrade tiers from `nodes.yml`.
* **Key (`String`)**: Node type & level tier (`mining_lv1`, `mining_lv5`).
* **Attributes**:
  * `nodeType`: `MINING`
  * `sizeChunks`: `1` (`1x1` chunk) for levels 1-4; `4` (`2x2` chunks) for level 5+.
  * `schematicName`: Filename in `plugins/BranzIdle/schematics/` (`mining_t1.schem`).
  * `upgradeCost`: Map of currency/resource requirements (`coins: 5000, oak_log: 100`).
  * `baseTickSeconds`: Base interval in seconds between resource production ticks (`60`).

---

## 3.4 Drop Table Registry (`DropTableRegistry`)
Loads drop tables and rare exploration rewards from `drop_tables.yml`.
* **Key (`String`)**: Drop table key (`mining_surface_drops`, `mining_deep_drops`).
* **Attributes**:
  * `minExplorationLevel`: Minimum node exploration level required to roll this table (`1`, `20`, `50`).
  * `entries`: List of entries (`resource_key`, `min_qty`, `max_qty`, `weight`).

---

## 3.5 Gacha Pool Registry (`GachaRegistry`)
Loads Gacha pool configurations from `gacha.yml`.
* **Key (`String`)**: Pool identifier (`coin_pool_basic`, `diamond_pool_premium`).
* **Attributes**:
  * `currencyType`: `COINS` or `DIAMONDS`.
  * `costPerPull`: `1000.0` Coins or `100` Diamonds.
  * `rewards`: List of worker template keys and their percentage pull weights.

---

# 4. Atomic Hot-Reloading (`/idle reload`)

To allow administrators and game designers to balance the economy on a live production server without restarting or causing race conditions, `RegistryManager` implements atomic hot-reload:

```java
public synchronized void reloadAll() {
    // 1. Parse all YAML files into temporary new registry instances
    ResourceRegistry newResources = new ResourceRegistry();
    newResources.load(yamlLoader.load("resources.yml"));
    
    // Validate cross-references (e.g., check if workers.yml references valid resources.yml keys)
    validateRegistries(newResources, ...);
    
    // 2. Atomically swap pointer references
    this.resourceRegistry = newResources;
    // ...
    Bukkit.getLogger().info("[Branz.Idle] All registries hot-reloaded successfully.");
}
```

* If a YAML syntax error or invalid cross-reference is encountered during parsing, the reload aborts, an error is printed to console, and the **previous working registry remains active in memory**.

---

# Document References

Next document:

**09NodeSystem.md**
