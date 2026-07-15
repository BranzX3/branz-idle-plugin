0# 00 - Vision.md

# Branz.Idle

> A production-ready Minecraft Idle MMORPG framework focused on autonomous progression, modular game systems, and long-term maintainability.

---

# Vision

**Branz.Idle** is not designed as a traditional Minecraft plugin.

It is designed as a complete **Idle MMORPG Engine** running inside Minecraft, where players continuously expand their territory, build autonomous production zones, develop workers, and progress through long-term strategic decisions.

The project aims to provide a living world that combines:

* Survival
* Idle Progression
* MMORPG Systems
* Player Economy
* Worker Collection
* Territory Expansion
* Long-term Character Growth

Every system should support years of future expansion without requiring fundamental architectural redesign.

---

# Project Philosophy

Branz.Idle follows several core principles.

## 1. Progress Never Stops

Player progression continues even while offline.

Offline production is a core gameplay mechanic rather than an optional feature.

Players should always feel rewarded for long-term progression.

---

## 2. Meaningful Automation

Automation exists to reduce repetitive gameplay—not to remove player decisions.

Players are responsible for:

* Expanding territory
* Creating production nodes
* Assigning workers
* Managing resources
* Upgrading infrastructure
* Optimizing production

Idle systems should reward planning rather than repetitive interaction.

---

## 3. A Living Base

Every player base should feel alive.

Workers travel.

Buildings evolve.

Production areas become larger.

Structures visually represent progression.

The world itself should communicate player growth without relying entirely on GUI interfaces.

---

## 4. Engine Before Content

The engine should never depend on specific gameplay content.

Mining, Lumber, Fishing, Farming, Ranching, and all future professions are considered independent content modules built on top of the core engine.

---

## 5. Data-Driven Design

Game content should be configurable without modifying source code.

Examples include:

* Drop tables
* Worker tiers
* Gacha rates
* Production rewards
* Exploration rewards
* Upgrade costs
* Node styles

The engine should remain stable while content evolves.

---

## 6. Modular Architecture

Every gameplay component should be replaceable.

This includes:

* Database providers
* Worker providers
* Animation systems
* GUI implementations
* Content modules

The core engine should never directly depend on third-party plugins.

---

# Gameplay Loop

The intended gameplay loop is:

Claim Territory

↓

Create Production Nodes

↓

Assign Workers

↓

Generate Resources

↓

Upgrade Infrastructure

↓

Expand Territory

↓

Collect Better Workers

↓

Unlock New Resources

↓

Strengthen Economy

↓

Repeat

---

# Core Progression

Player progression consists of several independent systems.

## Territory

Expand the player's land.

Unlock additional production nodes.

---

## Infrastructure

Upgrade production nodes.

Increase worker capacity.

Unlock new visual stages.

---

## Workers

Collect.

Train.

Level up.

Optimize.

---

## Exploration

Unlock deeper production areas.

Discover rare resources.

Expand production possibilities.

---

## Economy

Trade.

Invest.

Upgrade.

Expand.

---

# Non-Goals

Branz.Idle is intentionally **not** designed to become:

* A Skyblock server
* A technical factory simulator
* A clicker game
* A fully AFK experience

Players should always make meaningful strategic decisions.

---

# Engineering Principles

The project follows these engineering principles.

## Engine First

Core systems are implemented before gameplay content.

---

## Data-Driven

Content belongs in configuration.

Logic belongs in the engine.

---

## Composition Over Inheritance

Favor composition whenever possible to improve flexibility and maintainability.

---

## Loose Coupling

Systems communicate through services and events.

Direct dependencies between gameplay modules should be avoided.

---

## Asynchronous by Default

Heavy tasks should never block the Minecraft main thread.

Database operations, production calculations, and background processing should be asynchronous whenever possible.

---

## Logic and Presentation Separation

Gameplay simulation is independent from visual presentation.

Workers, structures, animations, and particles exist solely to improve immersion and should never directly control gameplay logic.

---

# Long-Term Goal

Branz.Idle is designed to support:

* Hundreds of node styles
* Thousands of collectible workers
* Multiple professions
* Community-created structures
* Live balance updates
* Seasonal content
* Future MMORPG systems

without requiring major architectural redesign.

---

# Document References

Next document:

**01Architecture.md**
