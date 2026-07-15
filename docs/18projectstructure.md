# 18 - Project Structure.md

# Branz.Idle Comprehensive Project & Package Structure

---

# 1. Architectural Layout & Module Breakdown

Branz.Idle is organized into **Feature-Based Domain Modules** under the root package `com.example.plugin`. Every gameplay domain encapsulates its own models, service interfaces, implementations, and database repositories.

```text
com.example.plugin
├── bootstrap
│    ├── BranzIdlePlugin.java             # Main JavaPlugin entry point
│    ├── ServiceRegistry.java             # Centralized service locator & DI wiring
│    └── ProviderManager.java             # Detection of Citizens, FAWE, and Vault
├── config
│    ├── RegistryManager.java             # Atomic hot-reload manager
│    ├── ConfigRegistry.java              # Base registry interface
│    ├── ResourceRegistry.java            # resources.yml loader & definition cache
│    ├── WorkerRegistry.java              # workers.yml loader & definition cache
│    ├── NodeRegistry.java                # nodes.yml loader & definition cache
│    ├── DropTableRegistry.java           # drop_tables.yml loader & definition cache
│    └── GachaRegistry.java               # gacha.yml loader & definition cache
├── database
│    ├── DatabaseManager.java             # HikariCP pool setup (SQLite / MySQL)
│    ├── SaveQueueService.java            # Asynchronous batching queue for dirty records
│    └── Repository.java                  # Generic CRUD interface
├── territory
│    ├── model
│    │    ├── ChunkClaim.java             # Claimed chunk record & coordinates
│    │    └── ChunkType.java              # RESIDENTIAL vs PRODUCTION enum
│    ├── service
│    │    ├── TerritoryService.java       # Claiming, adjacent bounds checking interface
│    │    └── TerritoryServiceImpl.java   # Implementation
│    ├── repository
│    │    └── TerritoryRepository.java    # territory_chunks SQL queries
│    └── event
│         ├── TerritoryClaimedEvent.java
│         └── TerritoryAccessDeniedEvent.java
├── protection
│    ├── listener
│    │    ├── BlockBreakPlaceListener.java  # Blocks unauthorized block edits inside claims
│    │    ├── EntityInteractListener.java   # Blocks unauthorized entity interaction & container access
│    │    └── EnvironmentalProtectionListener.java # Prevents creeper/tnt explosions & enderman griefing
│    └── service
│         └── ProtectionService.java      # Permission checking engine
├── onboarding
│    ├── model
│    │    └── StarterPack.java            # 4 free chunks + 3 workers + starter coins DTO
│    ├── service
│    │    ├── OnboardingService.java      # RTP spawn & starter pack distribution interface
│    │    └── OnboardingServiceImpl.java
│    └── event
│         └── PlayerOnboardingCompletedEvent.java
├── node
│    ├── model
│    │    ├── ProductionNode.java         # Node instance domain model
│    │    ├── NodeType.java               # MINING, LUMBER, FISHING enum
│    │    └── NodeSize.java               # SINGLE_1X1 vs MULTI_2X2 enum
│    ├── service
│    │    ├── NodeService.java            # Creation, upgrade, size expansion interface
│    │    └── NodeServiceImpl.java
│    ├── repository
│    │    └── NodeRepository.java         # production_nodes SQL queries
│    └── event
│         ├── NodeCreatedEvent.java
│         ├── NodeLevelUpEvent.java
│         └── NodeExpandedEvent.java
├── worker
│    ├── model
│    │    ├── WorkerInstance.java         # Individual worker domain entity
│    │    └── WorkerStats.java            # Runtime speed/yield/rare stat record
│    ├── service
│    │    ├── WorkerService.java          # Gacha pulling, leveling, assignment interface
│    │    └── WorkerServiceImpl.java
│    ├── repository
│    │    └── WorkerRepository.java       # worker_instances SQL queries
│    └── event
│         ├── WorkerGachaPulledEvent.java
│         ├── WorkerAssignedEvent.java
│         └── WorkerLevelUpEvent.java
├── production
│    ├── service
│    │    ├── ProductionService.java      # Online ticker & offline delta engine interface
│    │    └── ProductionServiceImpl.java
│    ├── task
│    │    └── ProductionTickerTask.java   # Bukkit repeating scheduler (every 5s)
│    └── event
│         └── ResourceProducedEvent.java
├── storage
│    ├── model
│    │    ├── NodeStorage.java            # Node local resource buffer
│    │    └── PlayerResourceWallet.java   # Centralized virtual wallet map
│    ├── service
│    │    ├── StorageService.java         # Collection flow & spending verification interface
│    │    └── StorageServiceImpl.java
│    ├── repository
│    │    ├── StorageRepository.java      # node_storage SQL queries
│    │    └── WalletRepository.java       # player_resource SQL queries
│    └── event
│         └── ResourceCollectedEvent.java
├── exploration
│    ├── model
│    │    └── NodeExploration.java        # Per-node depth level & XP record
│    ├── service
│    │    ├── ExplorationService.java     # Depth progression & drop table rolling interface
│    │    └── ExplorationServiceImpl.java
│    └── repository
│         └── ExplorationRepository.java  # node_exploration SQL queries
├── economy
│    ├── model
│    │    └── PlayerProfile.java          # Coins, Diamonds, and username record
│    ├── service
│    │    ├── EconomyService.java         # Currency transactions interface
│    │    └── EconomyServiceImpl.java
│    └── repository
│         └── PlayerRepository.java       # players SQL queries
├── gui
│    ├── BaseGUI.java                     # Common inventory wrapper interface
│    ├── GUIController.java               # Event handler & click debounce controller
│    ├── menu
│    │    ├── BaseDashboardGUI.java       # /base overview menu
│    │    ├── NodeDetailGUI.java          # Facility inspection & upgrade menu
│    │    ├── WorkerManagementGUI.java    # Paginated worker collection menu
│    │    ├── PlayerWalletGUI.java        # Paginated virtual resource menu
│    │    ├── GachaGUI.java               # Coin & Diamond Gacha pull menu
│    │    └── OnboardingWelcomeGUI.java   # First-time tutorial menu
│    └── util
│         ├── ItemBuilder.java            # Adventure API item stack helper
│         └── PaginationUtil.java         # 45-slot inventory page divider
├── visual
│    ├── VisualProvider.java              # Visual abstraction interface
│    ├── CitizensVisualProvider.java      # Citizens 2 NPC spawner & animation controller
│    ├── NoOpVisualProvider.java          # Fallback visual stub
│    └── VisualService.java               # Owner-only distance checking service (<=64 blocks)
├── integration
│    ├── structure
│    │    ├── StructureProvider.java      # Schematic pasting abstraction interface
│    │    ├── FAWEStructureProvider.java  # FastAsyncWorldEdit async pasting controller
│    │    └── NoOpStructureProvider.java  # Fallback beacon/hologram marker
│    └── vault
│         ├── VaultBridge.java            # Economy bridge abstraction interface
│         ├── BranzIdleVaultProvider.java # Vault API Economy implementation hook
│         └── NoOpVaultBridge.java        # Isolated internal economy stub
└── command
     ├── CommandController.java           # Subcommand router & dispatcher
     ├── BaseCommand.java                 # /base player command handler
     └── IdleCommand.java                 # /idle admin command handler
```

---

# 2. Complete `plugin.yml` Specification

```yaml
name: BranzIdle
version: '${version}'
main: com.example.plugin.bootstrap.BranzIdlePlugin
api-version: '26.2'
author: Antigravity
description: A production-ready Minecraft Idle MMORPG framework.
softdepend:
  - Citizens
  - FastAsyncWorldEdit
  - WorldEdit
  - Vault

commands:
  base:
    description: Opens the Branz.Idle player dashboard and territory controls.
    permission: branzidle.player
    aliases: [idlebase, mybase]
  idle:
    description: Branz.Idle administrative commands and registry reloading.
    permission: branzidle.admin

permissions:
  branzidle.player:
    description: Allows access to player gameplay and /base menus.
    default: true
  branzidle.admin:
    description: Allows access to administrative /idle management commands.
    default: op
```

---

# 3. Complete `build.gradle` Specification

```groovy
plugins {
    id 'java'
    id 'xyz.jpenilla.run-paper' version '2.3.1'
    id 'com.github.johnrengelman.shadow' version '8.1.1'
}

group = 'com.example.plugin'
version = '1.0.0-SNAPSHOT'

repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = "https://repo.papermc.io/repository/maven-public/"
    }
    maven {
        name = "citizens-repo"
        url = "https://repo.citizensnpcs.co/"
    }
    maven {
        name = "enginehub-repo"
        url = "https://maven.enginehub.org/repo/"
    }
    maven {
        name = "jitpack"
        url = "https://jitpack.io"
    }
}

dependencies {
    // Paper Server API (Provided by server at runtime)
    compileOnly "io.papermc.paper:paper-api:26.2.build.+"

    // Soft-Dependency External APIs (Provided by external plugins if installed)
    compileOnly "net.citizensnpcs:citizens-main:2.0.35-SNAPSHOT"
    compileOnly "com.fastasyncworldedit:FastAsyncWorldEdit-Core:2.11.1"
    compileOnly "com.sk89q.worldedit:worldedit-bukkit:7.3.0"
    compileOnly "com.github.MilkBowl:VaultAPI:1.7"

    // Bundled Libraries (Shadowed into final jar)
    implementation "com.zaxxer:HikariCP:5.1.0"
    implementation "com.github.ben-manes.caffeine:caffeine:3.1.8"
    implementation "org.xerial:sqlite-jdbc:3.45.1.0"
}

def targetJavaVersion = 25
java {
    def javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible()) {
        options.release.set(targetJavaVersion)
    }
}

processResources {
    def props = [version: version]
    inputs.properties props
    filteringCharset 'UTF-8'
    filesMatching('plugin.yml') {
        expand props
    }
}

tasks.shadowJar {
    archiveClassifier.set('')
    relocate 'com.zaxxer.hikari', 'com.example.plugin.lib.hikari'
    relocate 'com.github.benmanes.caffeine', 'com.example.plugin.lib.caffeine'
}

tasks.build {
    dependsOn tasks.shadowJar
}

tasks.runServer {
    minecraftVersion("26.2")
}
```

---

# 4. Implementation Roadmap & Phases

When coding begins, development strictly follows four sequential phases:

### Phase 1: Core Foundation & Infrastructure (`bootstrap`, `config`, `database`, `economy`)
* Set up HikariCP (`SQLite / MySQL`), DDL migration execution, and `SaveQueueService`.
* Implement `RegistryManager` and load YAML definitions into memory (`ResourceRegistry`, `GachaRegistry`).
* Implement `ServiceRegistry` and `PlayerProfile` caching (`Caffeine`).

### Phase 2: Territory, Protection & Onboarding (`territory`, `protection`, `onboarding`)
* Implement `/base claim` and `TerritoryService` adjacent boundary validation.
* Build self-contained `protection` listeners blocking unauthorized block breaks and entity interactions.
* Implement `OnboardingService` (`RTP spawn -> 4 free chunks -> 3 starter workers -> 1,000 coins`).

### Phase 3: Gameplay Loop & Simulation (`node`, `worker`, `production`, `storage`, `exploration`)
* Build `ProductionNode` entities, multi-chunk (`2x2`) expansion checks, and `WorkerInstance` leveling/stats.
* Implement `ProductionTickerTask` (online loops) and `calculateOfflineDelta` (`24h offline cap`).
* Implement `NodeStorage` buffer overflow rules and `StorageService.collectAll()` transfer into `PlayerResourceWallet`.
* Implement `DropTableRegistry` rare item rolls fueled by per-node `explorationLevel`.

### Phase 4: Presentation & External Providers (`gui`, `visual`, `integration`, `command`)
* Implement `GUIController` click debounce (`250ms`) and menus (`BaseDashboardGUI`, `NodeDetailGUI`, `WorkerManagementGUI`, `PlayerWalletGUI`, `GachaGUI`, `OnboardingWelcomeGUI`).
* Wire external `ProviderManager` (`CitizensVisualProvider` owner-only visibility `<= 64 blocks`; `FAWEStructureProvider` async `.schem` pasting).
* Finalize `/base` and `/idle` subcommands.

---

# Document References

Next documents:

**19ProtectionSystem.md** & **20OnboardingSystem.md**
