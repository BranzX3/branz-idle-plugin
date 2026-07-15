# 13 - GUI System.md

# Branz.Idle Presentation GUI System

---

# 1. GUI Architecture & Controller Pattern

To prevent item duplication exploits and keep inventory interaction logic cleanly separated from domain simulation, all menus in Branz.Idle are managed by a centralized **`GUIController`** inside the `gui` module (`com.example.plugin.gui`).

```text
GUIController (Registered Bukkit InventoryClickEvent / InventoryCloseEvent listener)
 ├── BaseDashboardGUI      (/base — Overview, Collect All, Navigation)
 ├── NodeDetailGUI         (Inspect facility, Upgrade, Style Selection, Assign Worker)
 ├── WorkerManagementGUI   (Paginated worker collection, stats, assign/unassign)
 ├── PlayerWalletGUI       (Inspect all virtual resources in player_resource)
 ├── GachaGUI              (Coin & Diamond 1x / 10x Pull menus with odds)
 └── OnboardingWelcomeGUI  (Starter Pack summary & first-time tutorial)
```

---

# 2. Core Security & Design Rules

## 2.1 Click Debouncing & Duping Prevention
* Every GUI click (`InventoryClickEvent`) is intercepted by `GUIController`.
* `event.setCancelled(true)` is invoked immediately on **all** custom GUI slots to ensure players can never shift-click or drag GUI items into their personal Minecraft inventory.
* A `DebounceCache` (`Caffeine Cache with 250ms expiration`) tracks player click timestamps. If a player spams an "Upgrade" or "Pull Gacha" button faster than `250ms`, the click is ignored to prevent double-charging or race conditions.

## 2.2 Async Data Fetch -> Sync Open
Never run `SELECT` queries inside `InventoryOpenEvent` or `/base` commands synchronously:
1. When `/base` is typed, `GUIController` fetches node and wallet data asynchronously (`CompletableFuture`).
2. Once data is ready, `GUIController` switches to the Bukkit main thread (`runTask()`) to construct the `Inventory` object and open it for the player.

---

# 3. Core Menu Specifications

## 3.1 Base Dashboard GUI (`BaseDashboardGUI`)
* **Trigger**: `/base` command (`or right-clicking the central hub block`).
* **Size**: `54 slots` (`6 rows`).
* **Key Components**:
  * **Top Row**: Player Profile summary (`Coins: 12,500`, `Diamonds: 150`, `Claimed Chunks: 4 / 10`).
  * **Center Grid (Slots 10-43)**: Visual map/list of owned `ProductionNode` facilities and `RESIDENTIAL` chunks. Clicking a node opens `NodeDetailGUI`.
  * **Bottom Navigation**:
    * **Slot 45**: Open `PlayerWalletGUI`.
    * **Slot 48**: **"Collect All Resources"** (`StorageService.collectAll()`).
    * **Slot 50**: Open `WorkerManagementGUI`.
    * **Slot 53**: Open `GachaGUI`.

---

## 3.2 Node Detail GUI (`NodeDetailGUI`)
* **Trigger**: Clicking a node inside `BaseDashboardGUI`.
* **Size**: `36 slots` (`4 rows`).
* **Key Components**:
  * **Slot 4 (Header)**: Node Info (`Level 4 Mining Facility`, `Size: 1x1 Chunk`, `Exploration: Lv 12`).
  * **Slot 13 (Upgrade Button)**: Shows cost (`Coins: 25,000`, `Iron Ore: 2,000`). If upgrading to Level 5 (`Expansion Threshold`), clearly notes: `"⚠️ Will expand facility to 2x2 multi-chunk complex! Requires 3 adjacent Residential chunks."`
  * **Slot 15 (Style Customizer)**: Opens the WorldEdit Schematic selection menu (`nodes.yml` styles).
  * **Slots 21-25 (Assigned Worker Slots)**: Shows currently assigned `WorkerInstance` heads. Clicking an empty slot opens `WorkerManagementGUI` in assignment mode. Clicking an occupied slot unassigns the worker.

---

## 3.3 Worker Management GUI (`WorkerManagementGUI`)
* **Trigger**: Base Dashboard -> Workers button (`or Node Detail -> Assign Worker`).
* **Size**: `54 slots` (`Paginated`).
* **Key Components**:
  * **Slots 0-44**: Displays `WorkerInstance` skull items.
    * Lore includes: `Profession`, `Rarity`, `Level 24 / 100`, `Speed Bonus: +25%`, `Yield Bonus: +40%`, `Status: Assigned to Mining Lv4 (`or Idle`)`.
  * **Slot 45**: Previous Page button (`if page > 1`).
  * **Slot 49**: Sort / Filter toggle (`By Profession`, `By Rarity`, `By Level`).
  * **Slot 53**: Next Page button (`if total > 45`).

---

## 3.4 Player Virtual Wallet GUI (`PlayerWalletGUI`)
* **Trigger**: Base Dashboard -> Wallet button.
* **Size**: `54 slots` (`Paginated`).
* **Key Components**:
  * Displays every resource key currently stored in `player_resource`.
  * Icons use `ResourceRegistry.material` (`e.g., IRON_ORE`) with custom `CustomModelData`.
  * Lore clearly shows formatted total units (`e.g., &bQuantity: 250,420`).

---

## 3.5 Gacha Pull GUI (`GachaGUI`)
* **Trigger**: Base Dashboard -> Gacha button (`or /idle gacha`).
* **Size**: `27 slots` (`3 rows`).
* **Key Components**:
  * **Slot 11 (Coin Basic Pool)**: Pull 1x (`1,000 Coins`) or Pull 10x (`10,000 Coins`).
  * **Slot 15 (Diamond Premium Pool)**: Pull 1x (`100 Diamonds`) or Pull 10x (`1,000 Diamonds`).
  * **Slot 22 (Drop Odds)**: Shows `GachaRegistry` probabilities (`Common: 60%, Rare: 25%, Epic: 12%, Legendary: 3%`).

---

## 3.6 Onboarding Welcome GUI (`OnboardingWelcomeGUI`)
* **Trigger**: Automatically opened when `PlayerOnboardingCompletedEvent` fires following initial `/base` claim.
* **Size**: `27 slots` (`3 rows`).
* **Key Components**:
  * **Slot 13**: Starter Pack Summary (`"Congratulations! You have received 4 Free Territory Chunks + 3 Starter Workers + 1,000 Coins!"`).
  * **Slot 15**: **"Accept & Open Base Dashboard"** (`closes tutorial and opens BaseDashboardGUI`).

---

# Document References

Next document:

**14VisualSystem.md**
