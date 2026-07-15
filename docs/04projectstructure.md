# 04 - Project Structure.md

# Branz.Idle Project Structure

> Defines the source code organization, package responsibilities, and dependency boundaries of the Branz.Idle project.

---

# Purpose

This document defines how the Branz.Idle codebase is organized.

The goal is to maintain:

* Clear ownership of code
* Predictable navigation
* Low coupling
* Easy feature expansion
* Consistent development patterns

The project structure should reflect the game architecture.

---

# Repository Structure

```text
Branz.Idle
│
├── src
│   ├── main
│   │   ├── java
│   │   └── resources
│   │
│   └── test
│       └── java
│
├── docs
│
├── gradle
│
├── build.gradle
├── settings.gradle
└── README.md
```

---

# Source Package Structure

Base package:

```text
com.branz.idle
```

Structure:

```text
com.branz.idle

├── bootstrap
├── api
├── common
├── core
├── content
├── database
├── gui
├── integration
└── BranzIdlePlugin
```

---

# Bootstrap Package

```text
bootstrap
```

## Responsibility

Responsible for starting the plugin.

Contains:

* Plugin initialization
* Dependency loading
* Service registration
* Listener registration
* Command registration
* Shutdown handling

---

## Rules

Must not contain:

* Gameplay logic
* Database queries
* GUI logic

---

# API Package

```text
api
```

## Responsibility

Contains public contracts between systems.

Examples:

```text
NodeService
WorkerService
StorageService
VisualProvider
```

---

## Rules

API contains:

* Interfaces
* DTOs
* Public events

API does not contain implementations.

---

# Common Package

```text
common
```

## Responsibility

Shared utilities.

Examples:

```text
constants
exceptions
utils
validation
```

---

## Rules

Common must remain generic.

Do not place gameplay logic here.

---

# Core Package

```text
core
```

The heart of Branz.Idle.

Contains all gameplay logic.

Structure:

```text
core

├── chunk
├── node
├── worker
├── production
├── storage
├── exploration
├── economy
├── progression
└── event
```

---

# Chunk Module

```text
core.chunk
```

Responsible for:

* Territory ownership
* Chunk claiming
* Chunk validation
* Neighbor checking

Does not control:

* Node production
* Worker behavior

---

# Node Module

```text
core.node
```

Responsible for:

* Node lifecycle
* Node upgrades
* Node state
* Worker capacity

---

# Worker Module

```text
core.worker
```

Responsible for:

* Worker data
* Worker leveling
* Worker assignment
* Worker progression

Worker gameplay data must be independent from visual entities.

---

# Production Module

```text
core.production
```

Responsible for:

* Production calculation
* Offline calculation
* Reward generation
* Production cycles

Production must not depend on animation or GUI.

---

# Storage Module

```text
core.storage
```

Responsible for:

* Resource storage
* Capacity
* Collection
* Storage limits

---

# Exploration Module

```text
core.exploration
```

Responsible for:

* Zone exploration
* Unlocking resources
* Exploration progression

---

# Economy Module

```text
core.economy
```

Responsible for:

* Currency
* Transactions
* Costs
* Rewards

---

# Content Package

```text
content
```

Contains configurable game definitions.

Structure:

```text
content

├── node
├── worker
├── drop
├── style
├── gacha
└── exploration
```

---

## Example

Node content:

```text
Mining Node

↓

Node Definition

↓

Production Rules

↓

Drop Table
```

---

# Database Package

```text
database
```

Responsible for persistence.

Structure:

```text
database

├── connection
├── repository
├── entity
└── migration
```

---

## Rules

Database layer:

Can:

* Save data
* Load data

Cannot:

* Calculate production
* Upgrade workers
* Handle gameplay rules

---

# GUI Package

```text
gui
```

Responsible for player interaction.

Structure:

```text
gui

├── menu
├── component
├── controller
└── builder
```

---

## Rules

GUI:

Can:

* Display information
* Receive input

Cannot:

* Modify game state directly

---

# Integration Package

```text
integration
```

External plugin adapters.

Structure:

```text
integration

├── citizens
├── vault
└── placeholder
```

---

## Rules

External APIs must stay inside this package.

Example:

Allowed:

```text
CitizensProvider
```

Not allowed:

```text
NodeService imports Citizens API
```

---

# Resource Structure

```text
resources

├── config.yml
├── messages.yml
│
├── content
│   ├── nodes
│   ├── workers
│   ├── drops
│   ├── styles
│   └── gacha
│
└── database
```

---

# Dependency Direction

Allowed:

```text
bootstrap

↓

api

↓

core

↓

database
```

Presentation:

```text
gui

↓

api/core services
```

Integration:

```text
integration

↓

External Plugins
```

---

# Forbidden Dependencies

The following are forbidden:

```text
Core → Citizens

Core → GUI

Core → Database Implementation

Content → Core Logic

Database → Service
```

---

# Feature Addition Flow

When adding a new feature:

Example: New profession "Fishing"

The developer should add:

```text
content

+

core module implementation

+

GUI integration

+

optional visual provider
```

Existing systems should require minimal modification.

---

# Summary

The Branz.Idle project structure is designed around domain ownership.

Each package has a clear responsibility.

The goal is not to create many folders, but to create clear boundaries that prevent the project from becoming difficult to maintain.

---

# Next Document

05-Domain-Model.md
