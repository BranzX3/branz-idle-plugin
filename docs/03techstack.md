# 03 - Technology Stack.md

# Branz.Idle Technology Stack

> Defines the official technology stack, runtime environment, third-party dependencies, and development tools used throughout the project.

---

# Purpose

This document standardizes the technologies used within Branz.Idle.

All contributors should follow this document to ensure consistency across the project.

Technologies not listed here should not be introduced without architectural review.

---

# Runtime Environment

## Minecraft

Target Version

* Minecraft Java Edition **26.2+**

Minimum Supported Version

* **26.2**

Older Minecraft versions are **not supported**.

---

## Server Software

Official Target

* Paper

The project is designed around the Paper API and may use Paper-specific features when they provide better performance or cleaner APIs.

---

## Java

Runtime Version

* **Java 25 LTS**

All source code should target Java 25 language features.

Do not write code using outdated Java patterns solely for backward compatibility.

---

# Build System

## Gradle

Reasons:

* Official Paper support
* Excellent dependency management
* Fast incremental builds
* Strong IDE integration
* Future multi-module support

---

# Core APIs

## Paper API

Primary gameplay API.

Avoid unnecessary usage of Bukkit when Paper provides a better alternative.

---

## Adventure API

Used for:

* Components
* MiniMessage
* Text formatting
* Titles
* BossBars
* ActionBars

Legacy color codes should be avoided whenever possible.

---

# Database

Default Provider

* SQLite

Optional Provider

* MySQL

Database access must always occur through Repository interfaces.

Business logic must never depend on a specific database implementation.

---

# Configuration

Configuration Format

* YAML

Configuration Categories

* Nodes
* Workers
* Styles
* Drop Tables
* Gacha
* Economy
* Exploration
* Messages

Gameplay balancing must remain configurable.

---

# Serialization

Primary Library

* Jackson

Reasons:

* Excellent Java support
* Modern API
* Record compatibility
* Strong performance
* Future extensibility

---

# Cache

Primary Cache

* Caffeine

Used for:

* Player cache
* Worker cache
* Node cache
* Configuration cache

Avoid excessive database access.

---

# NPC Visualization

Primary Provider

* Citizens

Citizens is responsible only for visual worker representation.

Gameplay simulation must remain independent from Citizens.

Future providers should be replaceable without changing the Core Engine.

---

# Logging

Use the Paper logger together with structured logging practices.

Logs should always provide enough context for debugging.

Examples include:

* Player UUID
* Worker ID
* Node ID
* Chunk Coordinates

---

# Scheduler

Gameplay scheduling should use Paper's scheduler APIs.

Heavy computations should execute asynchronously whenever possible.

The main server thread must remain lightweight.

---

# Development Tools

Recommended IDE

* IntelliJ IDEA

Supported IDE

* Visual Studio Code

Recommended Plugins

* Gradle
* Lombok (if adopted)
* SonarLint

---

# Testing

Testing Strategy

* Unit Tests for business logic
* Integration Tests for repositories
* Manual in-game verification for gameplay systems

The Core Engine should remain testable outside of Minecraft whenever practical.

---

# External Plugin Policy

External plugins are integrations, not dependencies.

Current integrations may include:

* Citizens
* PlaceholderAPI
* Vault

The Core Engine must communicate through Provider interfaces rather than directly using third-party APIs.

---

# Technology Principles

Branz.Idle follows these technology principles:

* Modern Java First
* Paper API First
* No NMS by Default
* Data-Driven Configuration
* Replaceable Providers
* Asynchronous Processing
* Engine Independence
* Long-Term Maintainability

---

# Future Considerations

Potential technologies that may be adopted in future versions include:

* Redis
* PostgreSQL
* Web-based Admin Panel
* Remote Content Synchronization
* Metrics & Telemetry

These are intentionally excluded from the initial implementation to keep the MVP focused.

---

# Summary

The selected technology stack prioritizes stability, maintainability, and long-term scalability.

Every technology included in this document has a defined responsibility and should support the architectural goals described in previous documents.

---

# Next Document

04-Project Structure.md
