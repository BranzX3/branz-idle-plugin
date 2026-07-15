package com.example.plugin.worker.model;

/**
 * Value object holding calculated effective bonuses for a worker instance.
 */
public record WorkerStats(
    double speedBonus,
    double yieldBonus,
    double rareDropBonus
) {}
