# 01 - Architecture.md

# Branz.Idle Architecture

> Defines the software architecture, module boundaries, dependency rules, and engineering principles used throughout the project.

---

# Overview

Branz.Idle is designed as a modular game engine rather than a monolithic Minecraft plugin.

The architecture separates **gameplay logic**, **content**, **presentation**, and **external integrations** into independent layers.

This approach improves maintainability, scalability, testing, and future expansion.

---

# Architecture Goals

The architecture is designed to achieve the following goals:

* High maintainability
* Low coupling
* High cohesion
* Modular content
* Data-driven gameplay
* Extensible systems
* Plugin independence
* Performance-oriented execution

---

# High-Level Architecture

```
                   Minecraft Server
                           │
                    Bukkit / Paper API
                           │
                    Bootstrap Layer
                           │
          ┌─────────────────────────────────┐
          │          Core Engine            │
          └─────────────────────────────────┘
                           │
      ┌──────────────┬──────────────┬──────────────┐
      │              │              │              │
 Gameplay       Content       Presentation     Infrastructure
      │              │              │              │
      │              │              │              │
 Nodes        Config Files      GUI System      Database
 Workers      Styles            Worker Visual      Scheduler
 Economy      Drop Tables       Animation       Registry
 Production   Exploration       Structures      Storage
```

The Core Engine owns all gameplay rules.

Every other module exists to support the engine.

---

# Layer Responsibilities

## Bootstrap Layer

Responsible for:

* Plugin initialization
* Dependency validation
* Command registration
* Event registration
* Service registration
* Configuration loading

Contains no gameplay logic.

---

## Core Layer

The heart of the project.

Responsible for:

* Chunk System
* Node System
* Worker System
* Production
* Storage
* Exploration
* Economy
* Progression

The Core Layer must never depend directly on:

* Citizens
* WorldEdit
* PlaceholderAPI
* Database implementations

---

## Content Layer

Contains game content only.

Examples:

* Node definitions
* Drop tables
* Gacha pools
* Worker tiers
* Upgrade costs
* Exploration rewards
* Node styles

No gameplay logic exists here.

---

## Presentation Layer

Responsible for displaying gameplay.

Examples:

* Inventory GUI
* Worker visuals
* Structure generation
* Particle effects
* Sound effects
* Animations

Presentation must never modify gameplay state directly.

---

## Infrastructure Layer

Provides technical implementations.

Examples:

* SQLite
* MySQL
* YAML
* JSON
* Scheduler
* Cache
* Repository implementations

---

# Dependency Rules

Dependencies always flow downward.

```
Bootstrap
    ↓
Core
    ↓
Service
    ↓
Repository
```

Presentation communicates with gameplay through Services only.

Infrastructure never contains business logic.

Content never accesses the database.

---

# Core Systems

The engine consists of independent systems.

* Chunk System
* Node System
* Worker System
* Production System
* Storage System
* Exploration System
* Economy System
* Animation System
* Style System
* GUI System

Each system owns a single responsibility.

---

# Service-Oriented Design

All gameplay interactions occur through Services.

Examples:

* ChunkService
* NodeService
* WorkerService
* ProductionService
* StorageService
* ExplorationService
* StyleService
* AnimationService

Services own business logic.

Repositories only store data.

---

# Event-Driven Architecture

Systems communicate through events whenever possible.

Example flow:

```
Worker Assigned
        ↓
WorkerAssignedEvent
        ↓
Animation System
        ↓
Spawn Worker
```

Example events:

* ChunkClaimedEvent
* NodeCreatedEvent
* NodeUpgradedEvent
* WorkerAssignedEvent
* WorkerLevelUpEvent
* ProductionCompletedEvent
* StorageFullEvent
* ExplorationLevelUpEvent

This minimizes coupling between systems.

---

# Data-Driven Design

Gameplay data should never be hardcoded.

Configurable content includes:

* Node definitions
* Worker tiers
* Production values
* Drop tables
* Gacha rates
* Upgrade costs
* Style definitions
* Exploration rewards

Adding new gameplay content should require little or no code changes.

---

# Provider Pattern

External plugins are accessed through Providers.

Example:

```
Worker Visual
        │
        ▼
WorkerVisualProvider
        │
   ┌────┴────┐
   │         │
Citizens   Future Provider
```

The Core Engine never depends directly on external plugin APIs.

This allows implementations to be replaced without changing gameplay logic.

---

# Registry System

Runtime registries manage dynamic content.

Examples:

* NodeRegistry
* StyleRegistry
* WorkerRegistry
* AnimationRegistry
* DropTableRegistry

Registries become the single source of truth for loaded content.

---

# Separation of Logic and Presentation

Gameplay exists independently from Minecraft entities.

Example:

```
Worker
│
├── WorkerData
└── WorkerVisual
```

WorkerData always exists.

WorkerVisual only exists while the relevant area is loaded.

Production continues even if visual entities are unloaded.

---

# Threading Model

Minecraft Main Thread

Responsible for:

* Block updates
* GUI
* Worker spawning
* Entity movement

Background Threads

Responsible for:

* Database operations
* Production calculations
* Save queue
* Cache updates

Heavy operations must never block the server thread.

---

# Content Expansion

New gameplay content should be added through configuration and content modules.

Example:

```
Mining

↓

Duplicate Configuration

↓

Modify Content

↓

Register

↓

Ready
```

The engine should not require modification to introduce a new profession.

---

# Architecture Principles

Branz.Idle follows these principles:

* Single Responsibility Principle
* Composition over Inheritance
* Dependency Inversion
* Event-Driven Communication
* Data-Driven Gameplay
* Engine before Content
* Separation of Logic and Presentation
* Asynchronous Processing
* Extensibility by Design

---

# Summary

The Branz.Idle architecture is designed to remain stable while gameplay content continuously evolves.

The Core Engine should change rarely.

Content should change frequently.

This separation ensures long-term maintainability and sustainable development.

---

# Next Document

02-Development-Guideline.md
