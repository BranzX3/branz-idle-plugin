# 17 - Command System.md

# Branz.Idle Command System Specification

---

# 1. Command Philosophy & Structure

All interactions in Branz.Idle are accessible via intuitive GUI menus (`/base`), while subcommands provide shortcuts for power users and comprehensive management tools for server administrators (`/idle`).

Commands are registered inside `plugin.yml` and routed cleanly via `CommandController` using a subcommand dispatcher pattern (`Map<String, SubCommand>`).

---

# 2. Player Commands (`/base`)

The `/base` command hierarchy is available to all players (`Permission: branzidle.player` — default `true`).

| Command | Subcommand | Description |
|---|---|---|
| `/base` | *(None)* | Opens the `BaseDashboardGUI` overview of the player's territory, nodes, and currency. |
| `/base claim` | `claim` | Claims the `16x16` Minecraft chunk the player is currently standing in inside `idle_world` (`if adjacent to existing claim or first-time onboarding`). |
| `/base unclaim` | `unclaim` | Unclaims and abandons the current chunk (`demolishing any active node`). |
| `/base collect` | `collect` | Instantly transfers all generated resources from all `NodeStorage` buffers into the `PlayerResourceWallet`. |
| `/base workers` | `workers` | Opens the paginated `WorkerManagementGUI`. |
| `/base gacha` | `gacha` | Opens the `GachaGUI` for Coin/Diamond pulls. |
| `/base wallet` | `wallet` | Opens the `PlayerWalletGUI` to inspect virtual resource quantities. |

## First-Time Onboarding Exception
If a new player types `/base` before completing onboarding (`players.onboarding_completed == false`), `CommandController` intercepts the call, prompts them to stand in an empty spot after RTP, and executes the first-time `Starter Pack` claim flow (`granting 4 free chunks + 3 starter workers + 1,000 coins`).

---

# 3. Administrator Commands (`/idle`)

The `/idle` command hierarchy is restricted to server staff (`Permission: branzidle.admin` — default `op`).

| Command | Usage Example | Description |
|---|---|---|
| `/idle reload` | `/idle reload` | Atomically hot-reloads all YAML configuration files and in-memory registries (`config, resources, workers, nodes, drop_tables, gacha`). |
| `/idle give` | `/idle give Notch coins 50000` | Grants `coins` or `diamonds` to a player's `PlayerProfile`. |
| `/idle resource` | `/idle resource Notch iron_ore 1000` | Deposits virtual resource quantities directly into a player's `player_resource` wallet. |
| `/idle worker` | `/idle worker Notch miner_epic_1 50` | Instantiates and grants a specific worker template (`optional starting level`) to a player. |
| `/idle schematic` | `/idle schematic repaste <nodeId>` | Forces `StructureProvider` (`FAWE`) to repaste a node's `.schem` file (`useful if blocks were accidentally corrupted or after schematic updates`). |
| `/idle info` | `/idle info <nodeId|playerId>` | Prints detailed diagnostic stats (`JSON/Text cache view, active SQL queue state, worker stats`) to admin chat. |

---

# 4. Permission Tree (`plugin.yml`)

```yaml
permissions:
  branzidle.player:
    description: Allows access to basic player gameplay and /base commands.
    default: true
  branzidle.admin:
    description: Allows access to administrative /idle commands and schematic reloading.
    default: op
```

---

# Document References

Next document:

**18ProjectStructure.md**
