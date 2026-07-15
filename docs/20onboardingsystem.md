# 20 - Onboarding System.md

# Branz.Idle New Player Onboarding & Starter Pack Specification

---

# 1. Onboarding Philosophy

To guarantee that every new player entering Branz.Idle enjoys an instant, gratifying first impression without enduring hours of early-game resource or coin grinding, the `onboarding` module (`com.example.plugin.onboarding`) orchestrates a structured **Starter Pack & Guidance Flow**.

Instead of dropping players into an empty wilderness and forcing them to figure out chunk claiming and node construction from scratch, the engine automatically sets up a fully functioning **4-Chunk Starter Base** with **3 Active Workers** the exact moment they claim their first plot of land.

---

# 2. Step-by-Step Onboarding Flow

```text
Player enters dedicated `idle_world` (or types /base for the first time)
                         ↓
System checks `PlayerProfile.onboardingCompleted == false`
                         ↓
Player stands on an empty chunk & executes `/base claim`
                         ↓
`OnboardingService` validates `2x2 / adjacent area availability`
                         ↓
System atomically claims 4 Contiguous Chunks (1 Hub + 3 Nodes)
                         ↓
System instantiates & grants 3 Random Tier-1 Workers + 1,000 Coins
                         ↓
Fires `PlayerOnboardingCompletedEvent` -> Sets `onboarding_completed = true`
                         ↓
Opens `OnboardingWelcomeGUI` tutorial (`Ready to assign workers!`)
```

---

# 3. First Claim & Area Validation (`OnboardingService`)

When an unonboarded player executes `/base claim` inside `idle_world`:

1. **Origin Check**: `TerritoryService` verifies that the `16x16` chunk the player is standing in (`Origin Chunk X, Z`) is unclaimed.
2. **Adjacent Buffer Check**: Because the Starter Pack grants `4 contiguous chunks`, `OnboardingService` checks if `Origin + East + South + South-East` (`or a T-shape pattern around Origin`) are also completely free of existing claims.
   * If any adjacent chunk is blocked by another player's base, the command aborts cleanly with a helpful prompt: `"Please move to a slightly more open area! Your free 4-chunk Starter Base requires adjacent empty land."`
3. **Random Teleport (RTP) Shortcut**: If the server uses an RTP plugin (`or embedded RTP utility inside Branz.Idle`), typing `/base rtp` teleports new players directly to a verified open `4-chunk` clearing in `idle_world`, ready for instant claiming.

---

# 4. Starter Pack Assets (`StarterPack`)

Once land validation succeeds, `OnboardingService.grantStarterPack(player, originChunk)` executes inside a single asynchronous database transaction (`SaveQueueService`):

## 4.1 Four Free Claimed Chunks (`Territory & Node Creation`)
Instead of just giving 1 empty residential chunk, the engine establishes a complete starting compound:
* **Chunk 1 (`Origin: X, Z`)**: Designated as **`RESIDENTIAL`** (Player Hub / Central Spawn point / Decorative yard).
* **Chunk 2 (`East: X+1, Z`)**: Converted into a **`PRODUCTION`** chunk with a pre-built **`MINING` Node (`Level 1`)**.
* **Chunk 3 (`South: X, Z+1`)**: Converted into a **`PRODUCTION`** chunk with a pre-built **`LUMBER` Node (`Level 1`)**.
* **Chunk 4 (`South-East: X+1, Z+1`)**: Converted into a **`PRODUCTION`** chunk with a pre-built **`FISHING` Node (`Level 1`)**.
* *All 4 chunks have their architectural schematics pasted immediately via `StructureProvider` (`FAWE`).*

---

## 4.2 Three Starter Workers (`WorkerService`)
To allow the player to populate their three newly created Level 1 nodes instantly:
* `WorkerService` queries `WorkerRegistry` and randomly selects `3 Tier-1 (COMMON) templates` (`e.g., 1 Miner, 1 Lumberjack, 1 Fisher`).
* Three unique `WorkerInstance` records are inserted into `worker_instances` (`assigned_node_id = NULL`) and added to the player's collection.

---

## 4.3 Starter Currency (`EconomyService`)
* Exactly **`1,000 Coins`** are added to `PlayerProfile.coins`, giving the player enough funds to perform their first Gacha pull or upgrade a Level 1 node when ready.

---

# 5. Completion & Tutorial Welcome (`OnboardingWelcomeGUI`)

1. `PlayerProfile.onboardingCompleted` is set to `true` inside `PlayerCache` and `players` table.
2. `Bukkit.getPluginManager().callEvent(new PlayerOnboardingCompletedEvent(player.getUniqueId(), 4, starterWorkerIds))` is published.
3. `GUIController` immediately opens **`OnboardingWelcomeGUI`**:
   * **Header**: `"Welcome to your new Idle Empire, <Player>!"`
   * **Summary Panel**: `"We've prepared your starting base: a Residential Hub, a Mining Camp, a Lumber Mill, and a Fishing Dock."`
   * **Action Prompt**: `"You also received 3 Starter Workers! Click 'Open Base Dashboard' below, select your Mining Facility, and assign your first worker to begin generating resources!"`

From this exact moment onward, the player is fully onboarded and active in the continuous **Progress Never Stops** idle loop.

---

# Document References

This completes the **Branz.Idle Specification Suite (Documents 00 to 20)**.

Return to:

**00Vision.md** | **18ProjectStructure.md**
