# 16 - Economy System.md

# Branz.Idle Dual Currency & Resource Economy Specification

---

# 1. Economy Philosophy & Inflation Prevention

Traditional Minecraft server economies often suffer from hyperinflation caused by automated "NPC Sell Shops" where players dump millions of farmable items in exchange for infinite currency.

Branz.Idle prevents hyperinflation by separating the economy into **Currencies** (`Coins & Diamonds`) and **Production Resources** (`Virtual Resource Wallet`), enforcing distinct acquisition channels and utility sinks for each.

---

# 2. Dual Currency Architecture (`players` table)

## 2.1 Coins (`double coins`)
The standard in-game currency fueling everyday progression and basic worker recruitment.

### Acquisition Sources
* **Onboarding Starter Bonus**: Exactly `1,000 Coins` granted upon initial `/base` claim following RTP.
* **Daily Login Rewards**: Periodic rewards claimed via the Base Dashboard GUI (`/base`).
* **Milestone Achievements**: Completing long-term goals (`e.g., "Upgrade 5 Nodes to Level 20"` or `"Collect 1,000,000 Iron Ore"`).
* **Player-to-Player Trading**: Selling rare exploration drops (`mithril_crystal`) to other players.

### Primary Sinks
* **Basic Worker Gacha (`GachaRegistry`)**: Pulling Tier 1-3 workers (`1,000 Coins / pull`).
* **Node & Territory Upgrades**: Combined with physical resources to upgrade node levels (`NodeService.upgradeNode`) and claim new chunks (`/base claim`).

---

## 2.2 Diamonds (`long diamonds`)
The premium and rare currency reserved for high-end progression, rare worker recruitment, and convenience acceleration.

### Acquisition Sources
* **Rare Exploration Drops**: Small chance to drop during Level 50+ deep exploration cycles (`DropTableRegistry`).
* **Server Events & Competitions**: Rewards distributed during community building contests.
* **Optional Server Store Bridge**: Can be hooked to server donation packages (`Buycraft / Tebex`).

### Primary Sinks
* **Premium Worker Gacha (`GachaRegistry`)**: Pulling guaranteed Rare, Epic, or Legendary workers (`100 Diamonds / pull`).
* **Instant Upgrades / Speedups**: Bypassing construction timers or instantly completing offline catch-up cooldowns.
* **Exclusive Structure Styles**: Unlocking premium `.schem` architectural designs (`nodes.yml`).

---

# 3. Resource Economy (Why No Automated NPC Shop)

## 3.1 Basic Resources (`iron_ore`, `oak_log`, `raw_stone`)
* **No NPC Selling**: Players cannot dump basic resources into an automated server shop for Coins.
* **Utility Sink**: Basic resources are consumed in massive quantities directly from the `PlayerResourceWallet` (`player_resource` table) when upgrading nodes, claiming territory, or converting residential chunks.
* **Example**: Upgrading a Mining Node from Level 4 to Level 5 (`2x2 multi-chunk expansion`) requires `25,000 Coins + 2,000 Iron Ore + 500 Oak Log`.

## 3.2 Rare Exploration Drops (`mithril_crystal`, `ancient_bark`)
* **Player Trading Loop**: Because rare items require weeks of deep exploration (`node_exploration >= Lv 50`) and high `rareDropBonus` worker synergies, they hold natural scarcity.
* Players trade rare drops directly with one another (`or via future marketplace plugins`) in exchange for Coins and Diamonds, establishing a player-driven organic economy.

---

# 4. Optional Vault API Integration (`VaultBridge`)

By default, Branz.Idle operates as a self-contained economy using its own database columns (`players.coins` and `players.diamonds`).

If a server owner wishes to integrate Branz.Idle Coins with third-party server plugins (`such as EssentialsX, AuctionHouse, or QuickShop`), they can enable Vault synchronization in `config.yml`:

```yaml
economy:
  vault_sync: true # Hooks Branz.Idle Coins into Vault API
  currency_name: "Coins"
```

## How `VaultBridge` Works (`EconomyService`)
1. When `onEnable` boots the `economy` module, it checks `config.getBoolean("economy.vault_sync")` and `Bukkit.getPluginManager().getPlugin("Vault")`.
2. If both are present, `EconomyService` registers `BranzIdleVaultProvider` implementing `net.milkbowl.vault.economy.Economy`.
3. Any balance changes inside Branz.Idle (`EconomyService.addCoins()`) reflect immediately in `/balance` and third-party shops, and vice versa.
4. If `vault_sync` is false (`or Vault is missing`), `EconomyService` uses internal `NoOpVaultBridge`, operating safely in isolated mode.

---

# Document References

Next document:

**17CommandSystem.md**
