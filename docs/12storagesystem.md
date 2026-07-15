# 12 - Storage System.md

# Branz.Idle Storage & Virtual Resource Wallet System

---

# 1. Storage Architecture Overview

To support massive MMORPG-scale idle resource generation without cluttering physical Minecraft player inventories (`limited to 36 slots and 64-item stacks`), Branz.Idle implements a **Dual-Layer Virtual Storage Architecture**:

```text
+-----------------------------------------------------------------+
|                       LAYER 1: NODE STORAGE                     |
|                   (node_storage database table)                 |
|                                                                 |
| * Local buffer attached to each Production Node instance        |
| * Holds raw output from online ticks & offline catch-up cycles  |
| * Bounded by a strict maxCapacity (scaled by node level)        |
| * When full, production pauses until the player collects        |
+-----------------------------------------------------------------+
                                 |
           Player clicks "Collect All" inside GUI
                                 v
+-----------------------------------------------------------------+
|               LAYER 2: PLAYER VIRTUAL RESOURCE WALLET           |
|                  (player_resource database table)               |
|                                                                 |
| * Centralized account-level virtual storage                     |
| * Infinite stacking capacity (Long.MAX_VALUE per resource key)  |
| * Direct funding source for Node Upgrades and Structure Crafting|
+-----------------------------------------------------------------+
```

---

# 2. Layer 1: Node Storage Buffer (`NodeStorage`)

Every `ProductionNode` maintains an independent `NodeStorage` buffer (`Map<String, Long> resourceBuffer`) tracking generated items awaiting player collection.

## 2.1 Storage Capacity & Scaling
Each node has a `maxCapacity` limit defined in `nodes.yml`:
* **Level 1 Single-Chunk Facility (`1x1`)**: `5,000` base capacity.
* **Level 5 Multi-Chunk Complex (`2x2`)**: `50,000` base capacity.
* **Worker Stat Multipliers**: Workers assigned to the facility can provide storage capacity multipliers via special template attributes, allowing active workers to expand local buffer space.

## 2.2 Buffer Overflow Prevention
When `ProductionService` runs a tick (`or offline catch-up delta calculation`), it evaluates total stored units:

```java
long currentTotal = node.getStorage().getTotalStoredQuantity();
if (currentTotal >= node.getMaxCapacity()) {
    // Buffer is full — pause production cleanly without throwing errors
    return;
}
long allowedDelta = Math.min(producedDelta, node.getMaxCapacity() - currentTotal);
node.getStorage().addResource(resourceKey, allowedDelta);
```

---

# 3. Layer 2: Player Virtual Resource Wallet (`PlayerResourceWallet`)

The `PlayerResourceWallet` (`backed by player_resource table`) serves as the player's primary inventory inside the Branz.Idle engine.

## 3.1 Why Virtual Wallets over Physical Items?
1. **No Inventory Management Fatigue**: Players do not spend gameplay time organizing chests or dropping overflowing cobblestone stacks.
2. **High-Precision Economy**: Upgrading a Level 50 Mining Facility may require `250,000 Iron Ore`—an amount that physically requires nearly `4,000 Minecraft slots` (`75 double chests`). A virtual wallet handles this effortlessly using a single `BIGINT` database column.
3. **Seamless GUI Integration**: All collected quantities (`iron_ore: 250,420`, `oak_log: 1,200,000`) are displayed cleanly using formatted Adventure API text icons inside the Player Dashboard GUI (`GUIController`).

---

# 4. Collection Flow (`StorageService.collectNodeStorage`)

When a player interacts with a node (`or clicks "Collect All" on their Base Dashboard GUI`), `StorageService` executes the collection flow:

1. **Verify Ownership**: Ensure `player.getUniqueId().equals(node.getOwnerId())`.
2. **Iterate Buffer**: Loop through `node_storage` entries (`iron_ore: 4500`, `mithril_crystal: 12`).
3. **Atomic Wallet Deposit**: Add all quantities directly into the player's `player_resource` wallet map inside `PlayerCache`.
4. **Flush Buffer**: Reset all `node_storage` entry quantities to `0`.
5. **Publish & Persist**:
   * Fire `ResourceCollectedEvent(playerId, nodeId, transferredMap)`.
   * Submit async batch update (`SaveQueueService`) to commit the `node_storage` clear and `player_resource` additions simultaneously.
6. **Player Feedback**: Play an Adventure API sound effect (`ENTITY_EXPERIENCE_ORB_PICKUP`) and display an action bar summary (`"Collected +4,500 Iron Ore, +12 Mithril Crystal!"`).

---

# 5. Spending Resources from the Virtual Wallet

Whenever a player upgrades a facility (`NodeService.upgradeNode`) or unlocks a rare structural schematic (`nodes.yml` costs), `StorageService.consumeResources(playerId, costMap)` is called:

```java
public boolean consumeResources(UUID playerId, Map<String, Long> requiredCosts) {
    PlayerResourceWallet wallet = getWallet(playerId);
    // 1. Verify sufficient balances across all required keys
    for (var entry : requiredCosts.entrySet()) {
        if (wallet.getQuantity(entry.getKey()) < entry.getValue()) {
            return false; // Insufficient resources
        }
    }
    // 2. Deduct quantities and queue async database save
    for (var entry : requiredCosts.entrySet()) {
        wallet.removeResource(entry.getKey(), entry.getValue());
    }
    SaveQueueService.queueWalletUpdate(wallet);
    return true;
}
```

---

# Document References

Next document:

**13GUISystem.md**
