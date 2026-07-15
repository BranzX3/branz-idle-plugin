# 02 - Development Guideline.md

# Branz.Idle Development Guideline

> Defines the coding standards, architectural rules, naming conventions, and development practices for the Branz.Idle project.

---

# Purpose

This document ensures that every component of the project follows the same engineering standards.

Regardless of who writes the code—human or AI—the resulting architecture should remain consistent, maintainable, and scalable.

---

# General Principles

Every implementation should follow these principles:

* Readability over cleverness
* Maintainability over optimization
* Composition over inheritance
* Explicit over implicit
* Engine before content
* Data-driven whenever possible

---

# Package Structure

All source code must follow the predefined package structure.

```text
com.branz.idle
│
├── api
├── bootstrap
├── common
├── content
├── core
├── database
├── gui
└── integration
```

No classes should exist outside these root packages.

---

# Package Responsibilities

## api

Public interfaces intended for cross-module communication.

Never contains implementation.

---

## bootstrap

Plugin entry point.

Responsible only for initialization and registration.

No gameplay logic is allowed.

---

## common

Shared utilities, constants, helper classes, exceptions, and reusable components.

---

## content

Game definitions.

Examples:

* Node definitions
* Drop tables
* Worker tiers
* Upgrade costs
* Styles

No business logic.

---

## core

Business logic.

Contains every gameplay system.

---

## database

Persistence layer.

Repositories and database providers only.

---

## gui

Presentation layer.

Contains inventory menus and GUI controllers.

Never performs business logic directly.

---

## integration

Adapters for external plugins.

Examples:

* Citizens
* PlaceholderAPI
* Vault

Core systems must never directly depend on external plugins.

---

# Naming Conventions

## Classes

Use PascalCase.

Examples:

```text
NodeService
WorkerRepository
ChunkManager
ProductionScheduler
```

---

## Interfaces

Do not prefix with "I".

Good:

```text
WorkerRepository
VisualProvider
```

Bad:

```text
IWorkerRepository
```

---

## Methods

Use camelCase.

Examples:

```text
createNode()
assignWorker()
collectStorage()
calculateProduction()
```

Methods should describe actions.

---

## Variables

Use meaningful names.

Good:

```text
assignedWorker
productionSpeed
storageCapacity
```

Bad:

```text
data
value
temp
obj
```

---

## Constants

Use UPPER_SNAKE_CASE.

Example:

```text
MAX_WORKER_LEVEL
DEFAULT_STORAGE_SIZE
SAVE_INTERVAL
```

---

# Class Responsibilities

Every class should have one reason to change.

Examples:

NodeService

Responsible for node operations only.

Not responsible for:

* Database
* GUI
* Animation

---

# Service Rules

Business logic belongs inside Services.

Services may communicate with:

* Repositories
* Other Services
* Event System

Services must not communicate directly with:

* Inventory GUI
* Citizens API
* SQL Statements

---

# Repository Rules

Repositories only store and retrieve data.

Repositories never contain gameplay calculations.

Good:

```text
saveWorker()

findWorker()

deleteWorker()
```

Bad:

```text
calculateWorkerLevel()

upgradeNode()

produceItems()
```

---

# GUI Rules

GUI classes are controllers.

Responsibilities:

* Display data
* Receive player input
* Forward requests to Services

GUI never changes gameplay directly.

---

# Event Rules

Events describe something that has already happened.

Examples:

```text
NodeCreatedEvent
WorkerAssignedEvent
StorageFullEvent
```

Avoid command-like events such as:

```text
CreateNodeEvent
UpgradeNodeEvent
```

---

# Configuration Rules

Gameplay values belong in configuration files.

Never hardcode:

* Production values
* Drop rates
* Upgrade costs
* Gacha probabilities
* Exploration rewards

The engine should remain independent from balancing.

---

# Threading Rules

Main Thread

Allowed:

* GUI
* Entities
* Blocks
* Players

Background Threads

Allowed:

* Database
* Production calculation
* Cache
* Save queue

Never access Bukkit API from asynchronous threads unless explicitly supported.

---

# Error Handling

Recover whenever possible.

Log meaningful information.

Never silently ignore exceptions.

Avoid catching generic Exception unless absolutely necessary.

---

# Logging

Use structured logging.

Every important operation should provide enough context for debugging.

Examples:

* Player UUID
* Node ID
* Worker ID
* Chunk Position

Avoid unnecessary console spam.

---

# Dependency Rules

Allowed flow:

```text
GUI
    ↓
Service
    ↓
Repository
```

Never reverse dependencies.

Repositories should never call Services.

---

# Performance Guidelines

Avoid unnecessary object allocation inside repeating tasks.

Cache frequently used data.

Load content once during startup.

Use asynchronous saving.

Keep main-thread operations lightweight.

---

# Documentation

Every public API should include JavaDoc.

Complex algorithms should explain **why**, not **what**.

Code should be self-explanatory whenever possible.

---

# Code Review Checklist

Before merging any feature, verify:

* Single Responsibility Principle
* No duplicated logic
* No hardcoded gameplay values
* Proper service separation
* Async safety
* Configuration support
* Event usage where appropriate
* Naming consistency
* Documentation completed

---

# Summary

Consistency is more valuable than individual coding style.

A well-defined standard enables faster development, easier maintenance, better AI-assisted generation, and long-term scalability.

Every contributor should follow this guideline before implementing new features.

---

# Next Document

03techstack.md
