# 19 - Protection System.md

# Branz.Idle Self-Contained Territory Protection System

---

# 1. Protection Philosophy

In accordance with our **A Living & Protected Base** philosophy and to eliminate mandatory dependencies on heavy external protection suites (`such as WorldGuard or GriefPrevention`), Branz.Idle embeds a high-performance **Self-Contained Protection Engine** directly inside the `protection` module (`com.example.plugin.protection`).

Every 16x16 chunk claimed inside the dedicated `idle_world` (`whether RESIDENTIAL or PRODUCTION`) is automatically shielded against unauthorized modifications, griefing, explosions, and entity interference.

---

# 2. Protection Boundaries & Scope

## 2.1 Dedicated World Isolation (`idle_world`)
The protection engine listens specifically to Bukkit events occurring inside the world configured under `config.yml` -> `territory.dedicated_world_name` (`default: idle_world`). Events outside this world (`e.g., world_nether, survival_world`) are ignored immediately (`O(1) world check`) to prevent interference with other server gamemodes.

## 2.2 Claimed Territory Shield
Inside `idle_world`, any block coordinate (`Location`) mapped to a `ChunkClaim` record inside `TerritoryCache` is governed by strict ownership verification.

---

# 3. Protected Actions & Listener Architecture

The `protection` module divides event monitoring across three specialized listeners:

## 3.1 Block Edits (`BlockBreakPlaceListener`)
* **Events Intercepted**: `BlockBreakEvent`, `BlockPlaceEvent`, `BlockMultiPlaceEvent`, `BucketEmptyEvent`, `BucketFillEvent`.
* **Behavior**: If a player attempts to place or break a block inside a claimed chunk that they do not own, `event.setCancelled(true)` is invoked instantly.

---

## 3.2 Interactions & Containers (`EntityInteractListener`)
* **Events Intercepted**: `PlayerInteractEvent`, `PlayerInteractEntityEvent`, `PlayerArmorStandManipulateEvent`, `EntityDamageByEntityEvent`.
* **Behavior**:
  * **Containers & Redstone**: Unauthorized players cannot open Chests, Barrels, Shulker Boxes, Furnaces, Hoppers, or flip Levers and Doors inside another player's base.
  * **Entity & NPC Safety**: Protects all Citizens worker NPCs, armor stands, item frames, and passive animals inside the territory from being attacked, pushed, or killed by visitors or hostile mobs.

---

## 3.3 Environmental & Explosions (`EnvironmentalProtectionListener`)
* **Events Intercepted**: `EntityExplodeEvent` (`Creeper, TNT, Wither, Fireball`), `BlockExplodeEvent` (`Bed/Respawn Anchor`), `EntityChangeBlockEvent` (`Enderman block theft`), `BlockBurnEvent` (`Fire spread`), `BlockIgniteEvent`, `PistonExtendEvent`, `PistonRetractEvent`.
* **Behavior**:
  * **Explosions**: When a Creeper or TNT explodes within or near a claimed chunk, the event's affected block list (`event.blockList()`) is filtered; any block belonging to a claimed chunk is stripped from the explosion destruction list.
  * **Pistons**: Prevents players from using pistons outside a territory border to push blocks into or pull structures out of a claimed chunk.
  * **Endermen & Fire**: Automatically prevents Endermen from picking up or placing blocks, and completely halts fire spread inside claimed chunks.

---

# 4. Permission Checking Engine (`ProtectionService`)

All three listeners route permission checks through a single, highly optimized method inside `ProtectionService`:

```java
public boolean hasAccess(Player player, Location location, ActionType actionType) {
    // 1. Check if location is inside the dedicated idle_world
    if (!location.getWorld().getName().equals(dedicatedWorldName)) {
        return true; // Ignore outside worlds
    }

    // 2. Lookup ChunkClaim in O(1) from TerritoryCache
    int chunkX = location.getBlockX() >> 4;
    int chunkZ = location.getBlockZ() >> 4;
    Optional<ChunkClaim> claimOpt = territoryService.getClaim(chunkX, chunkZ);

    // 3. Unclaimed Wilderness Handling
    if (claimOpt.isEmpty()) {
        // Return true if wilderness building is allowed by config, or false if restricted to claims only
        return allowWildernessBuilding;
    }

    ChunkClaim claim = claimOpt.get();

    // 4. Ownership Verification
    if (player.getUniqueId().equals(claim.getOwnerId())) {
        return true; // Owner has full access
    }

    // 5. Admin Bypass
    if (player.hasPermission("branzidle.admin.bypass")) {
        return true; // Staff override
    }

    // 6. Access Denied -> Fire event and notify
    Bukkit.getPluginManager().callEvent(new TerritoryAccessDeniedEvent(player, claim.getOwnerId(), location, actionType));
    player.sendActionBar(Component.text("You cannot modify territory belonging to another player!", NamedTextColor.RED));
    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
    
    return false;
}
```

---

# 5. Optional External Bridge (`WorldGuard Bridge`)

While native protection is self-contained and active by default, if a server administrator has complex existing WorldGuard region setups across `idle_world` (`e.g., server spawn protected by WorldGuard`), `ProtectionService` can optionally check `WorldGuardPlugin` flags before evaluating `hasAccess()`, ensuring 100% compatibility with third-party server regions.

---

# Document References

Next document:

**20OnboardingSystem.md**
