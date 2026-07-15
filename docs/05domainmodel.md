# 05 - Domain Model.md

# Branz.Idle Domain Model

> Defines the core gameplay entities, relationships, and ownership boundaries inside Branz.Idle.

---

# Purpose

This document defines the fundamental objects that exist within the Branz.Idle world.

A Domain Model represents gameplay concepts, not database tables.

The goal is to ensure that every system has a clear ownership of data and responsibilities.

---

# Domain Overview

The main gameplay domains are:

```text
Player

│

├── Territory
│
│   └── Chunk
│
│       └── Node
│
│           ├── Worker
│           │
│           ├── Production
│           │
│           └── Storage
│
└── Progression
```

---

# Player

## Description

Represents a player account inside Branz.Idle.

A player owns territory, workers, progression, and economy.

---

## Responsibilities

Player owns:

* Claimed chunks
* Workers
* Currency
* Progression
* Exploration progress

---

## Does Not Own

Player does not directly own:

* Production calculation
* Node logic
* Visual entities

---

# Territory

## Description

Represents the player's controlled world area.

Territory is the foundation of Branz.Idle expansion.

---

## Rules

* Players start with a predefined number of chunks.
* New chunks must connect to existing territory.
* Territory expansion requires cost.
* Territory cannot overlap with other players.

---

# Chunk

## Description

A Chunk is the smallest ownership unit in Branz.Idle.

A chunk represents one controllable area.

---

## Core Rule

```text
1 Chunk = 1 Node
```

A claimed chunk can become a production node.

---

## Chunk States

Example:

```text
UNCLAIMED

↓

CLAIMED

↓

NODE_ASSIGNED

↓

ACTIVE
```

---

## Responsibilities

Chunk manages:

* Owner
* Location
* Node assignment
* Visual boundaries

---

# Node

## Description

A Node represents a specialized production area.

Examples:

* Mining Node
* Lumber Node
* Fishing Node
* Farming Node
* Ranch Node

---

## Node Responsibilities

A Node manages:

* Production type
* Assigned workers
* Node level
* Style
* Storage
* Production state

---

## Node Does Not Manage

Node does not directly control:

* NPC rendering
* GUI
* Database saving

---

# Node Progression

Node progression is based on infrastructure growth.

Primary progression:

* Worker capacity
* Visual upgrade
* Storage improvement
* Production efficiency

---

## Node Level Philosophy

Node levels should represent visible growth.

Example:

```text
Level 1

Basic Structure

↓

Level 5

Improved Facility

↓

Level 10

Advanced Facility
```

---

# Worker

## Description

A Worker is a collectible gameplay unit assigned to production.

Workers are the main progression and optimization system.

---

## Worker Data

A Worker contains:

* Unique ID
* Tier
* Level
* Experience
* Stats
* Assignment

---

## Worker Does Not Represent

Worker is not a Minecraft Entity.

Worker is gameplay data.

---

# Worker Visual

## Description

Visual representation of a Worker inside Minecraft.

Possible implementations:

* Citizens NPC
* Display Entity
* Future providers

---

## Rule

Worker Data and Worker Visual are separated.

Example:

```text
Worker

├── Worker Data

└── Worker Visual
```

---

# Worker Level

## Maximum Level

```text
Level 100
```

---

## Experience Source

Workers gain experience from:

* Completing production
* Exploration tasks
* Special rewards

---

## Level Growth

Each level increases worker effectiveness.

Stats are determined through:

* Worker tier
* Random stat growth
* Level progression

---

# Worker Stats

Workers do not have classes.

Each worker is differentiated through stats.

Examples:

* Production Speed
* Resource Bonus
* Rare Drop Chance
* Exploration Efficiency
* Storage Efficiency

---

# Production

## Description

Production is the process of converting worker activity into resources.

---

## Production Model

```text
Worker

+

Node

+

Production Rules

↓

Resource Output
```

---

# Production Tick

Production should support:

* Online production
* Offline production
* Catch-up calculation

---

## Offline Philosophy

Offline players receive progress.

However:

Offline efficiency may be limited compared to active gameplay.

---

# Storage

## Description

Each Node has independent storage.

---

## Storage Model

```text
Node

↓

Storage

↓

Resources
```

---

## Rules

Players can manage storage from GUI.

Players do not need to physically visit every node.

---

# Resource

## Description

Resources are production outputs used for progression and economy.

Examples:

* Wood
* Ore
* Fish
* Crops
* Rare Materials

---

## Resource Usage

Resources may be used for:

* Upgrades
* Crafting
* Economy
* Future MMO systems

---

# Exploration

## Description

Exploration represents progression depth within a profession.

---

## Examples

Mining:

```text
Surface Mine

↓

Deep Cave

↓

Ancient Mine

↓

Rare Resource Zone
```

---

## Rules

Exploration progression is separated by Node type.

Example:

Mining exploration does not affect Fishing.

---

# Economy

## Currency Types

Initial design:

## Coin

Obtained through gameplay.

Used for:

* Basic upgrades
* Worker gacha
* Trading

---

## Diamond

Premium currency.

Used for:

* Premium worker gacha
* Territory expansion shortcuts
* Special systems

---

# Gacha Worker System

Workers are obtained through gacha.

Sources:

* Coin Gacha
* Diamond Gacha

---

## Worker Lifecycle

```text
Acquire

↓

Train

↓

Assign

↓

Level

↓

Trade / Convert
```

---

# Worker Removal

Unused workers may:

Option A:

Sell to another player.

Option B:

Convert into Worker EXP material.

Option C:

Convert into currency.

Final balancing decision is handled later.

---

# Domain Relationships

```text
Player

1 ─── N

Chunk


Chunk

1 ─── 1

Node


Node

1 ─── N

Worker


Node

1 ─── 1

Storage


Worker + Node

↓

Production
```

---

# Design Rules

The Domain Model follows these rules:

* Gameplay objects are independent from visuals.
* Data ownership must be clear.
* Production must work without loaded chunks.
* GUI must not own gameplay state.
* External plugins must not define domain behavior.

---

# Summary

The Branz.Idle Domain Model creates a foundation for an expandable idle MMORPG system.

Future systems such as:

* Guilds
* Trading
* Dungeons
* Bosses
* Seasonal Events

should integrate into this model without rewriting existing gameplay systems.

---

# Next Document

06-Database.md
