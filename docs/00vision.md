# 00 - Vision.md

# Branz.Idle

> A production-ready Minecraft Idle MMORPG framework focused on autonomous progression, modular game systems, territory protection, and long-term maintainability.

---

# Vision

**Branz.Idle** is not designed as a traditional Minecraft plugin.

It is designed as a complete **Idle MMORPG Engine** running inside a dedicated Minecraft world, where players continuously claim and protect territory, build autonomous production zones, develop collectible workers, and progress through long-term strategic decisions.

The project aims to provide a living world inside a **Dedicated World** (`idle_world`) that combines:

* Survival & Exploration
* Idle Progression & Offline Resource Generation
* MMORPG Systems & Economy
* Player Territory & Self-Contained Protection
* Worker Collection & Optimization
* Long-term Character & Empire Growth

Every system is engineered to support years of future expansion without requiring fundamental architectural redesign.

---

# Project Philosophy

Branz.Idle follows several core principles.

## 1. Progress Never Stops

Player progression continues even while offline.

Offline production is a core gameplay mechanic rather than an optional feature. Players generate resources continuously based on elapsed time and stored calculation states.

Players should always feel rewarded for long-term progression and planning.

---

## 2. Meaningful Automation

Automation exists to reduce repetitive gameplay—not to remove player decisions.

Players are responsible for:

* Claiming and protecting territory
* Selecting and creating specialized production nodes
* Assigning and optimizing workers
* Managing resource storage and economy
* Upgrading infrastructure and node capacity
* Exploring deeper production tiers

Idle systems reward strategic placement and management rather than repetitive manual clicking.

---

## 3. A Living & Protected Base

Every player base inside the dedicated world should feel alive and safe.

Workers travel to visual work locations. Buildings and structures evolve with node levels. Production areas become larger as infrastructure expands. Structures visually represent progression.

At the same time, all claimed territory is protected by a self-contained protection engine, ensuring that player structures, workers, and resources are safe from griefing, block breaking, and environmental damage.

---

## 4. Engine Before Content

The engine should never depend on specific gameplay content.

Mining, Lumber, Fishing, Farming, Ranching, and all future professions are considered independent content modules defined via external configuration files and registered into the core engine at runtime.

---

## 5. Data-Driven Design

Game content must be configurable without modifying source code.

Examples include:

* Node definitions and upgrade costs
* Drop tables and resource rarities
* Worker templates, tiers, and growth rates
* Gacha pools and probabilities
* Exploration rewards and zone unlocks
* Structure styles and schematic mappings

The core engine remains stable and compiled while game balancing and content evolve dynamically.

---

## 6. Modular Architecture

Every gameplay component is built as a feature-oriented module.

This includes:

* Territory & Protection systems
* Node & Worker management
* Production & Storage calculators
* Economy & Gacha engines
* Presentation & GUI controllers
* External integrations (Citizens, WorldEdit/FAWE, Vault)

The core engine communicates cleanly via service interfaces and events, ensuring soft dependencies on third-party visualization or schematic tools.

---

# Gameplay Loop & Onboarding

## New Player Onboarding (Starter Flow)

To ensure an engaging first impression without tedious early grinding, new players enter through a structured onboarding flow:

1. **Random Teleport (RTP)**: Players enter the dedicated `idle_world` and are teleported to a clean, available location.
2. **First Claim & Starter Pack**: Upon claiming their first chunk via `/base`, players receive a **Starter Pack**:
   * **4 Free Claimed Chunks**: Arranged as a contiguous starting base (1 Residential/Central Chunk + 1 Mining Node + 1 Lumber Node + 1 Fishing Node).
   * **3 Random Starter Workers**: Instantly added to the player's collection to begin assigning immediately.
   * **Starter Coins**: Initial currency to facilitate early GUI interactions and basic upgrades.

## Continuous Gameplay Loop

```text
Claim Adjacent Territory & Expand Base
                ↓
Create & Customize Production Nodes
                ↓
Assign & Optimize Workers from Gacha
                ↓
Generate Resources (Online & Offline)
                ↓
Collect Resources into Virtual Wallet
                ↓
Sell / Trade / Upgrade Infrastructure
                ↓
Explore Deeper Zones for Rare Materials
                ↓
Repeat & Scale Production Empire
```

---

# Core Progression

Player progression consists of several interconnected, feature-based systems.

## Territory & Protection

* Claim contiguous 16x16 Minecraft chunks in the dedicated world.
* Expand land automatically or via currency.
* Full self-contained protection against unauthorized block breaking, placing, explosions, and mob griefing.

---

## Infrastructure & Node Expansion

* Upgrade production nodes from basic Lv1 facilities (1 chunk, 16x16) up to advanced Lv5+ multi-chunk complexes (2x2 chunks, 32x32).
* Increase worker slot capacity and storage limits.
* Unlock and paste new architectural styles via WorldEdit/FAWE schematics.

---

## Workers

* Collect unique workers via Coin and Diamond Gacha pools.
* Train and level up workers (Max Level 100) through active production and exploration.
* Optimize production speed, resource bonuses, and rare drop chances.

---

## Exploration

* Each node maintains independent exploration progression.
* Discover deeper zones (e.g., Surface Mine → Deep Cave → Ancient Mine).
* Unlock rare materials and high-tier drop tables.

---

## Economy & Storage

* Virtual Resource Wallet: All collected node resources are safely stored in a virtual wallet (`player_resource`) rather than cluttering physical inventory.
* Dual Currency: Coins (gameplay earned) for basic upgrades and gacha; Diamonds (premium/rare rewards) for acceleration and rare gacha pools.

---

# Non-Goals

Branz.Idle is intentionally **not** designed to become:

* A traditional Skyblock or Factions server
* A complex technical factory simulator requiring redstone/pipes
* A mindless clicker game
* An unprotected free-for-all building world

Players should always make meaningful strategic decisions within a secure, autonomous environment.

---

# Engineering Principles

## Engine First

Core simulation and state management are implemented cleanly before visual or content modules.

---

## Data-Driven & Configurable

All templates (`worker_template`, `resource`, `drop_table`, `node_style`, `gacha_pool`) reside in YAML files and runtime registries—never hardcoded or stored in relational database tables.

---

## Feature-Based Modular Packaging

Code is organized by business domain (`territory`, `node`, `worker`, `production`, `protection`, `onboarding`, etc.) to ensure clear ownership and seamless scalability.

---

## Loose Coupling via Services & Events

Systems communicate through a centralized `ServiceRegistry` and Bukkit/Paper event publishing. Direct cross-module database or GUI calls are strictly prohibited.

---

## Asynchronous by Default

Database persistence, production catch-up calculations, and schematic pastes execute on background threads to ensure the Minecraft server main thread remains lightweight and lag-free at 20 TPS.

---

## Logic and Presentation Separation

Gameplay simulation runs completely independent of visual entities. If Citizens NPCs or WorldEdit schematics are missing or unloaded, production and worker progression continue without interruption.

---

# Document References

Next document:

**01Architecture.md**
