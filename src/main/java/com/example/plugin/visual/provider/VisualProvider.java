package com.example.plugin.visual.provider;

import org.bukkit.Location;

import java.util.UUID;

/**
 * Abstraction layer for visual worker entity rendering and NPC animations.
 */
public interface VisualProvider {

    /**
     * Spawns or updates a visual NPC representing a worker at the target location.
     */
    void spawnWorkerNPC(UUID workerId, String templateId, Location location);

    /**
     * Removes the visual NPC associated with a worker ID.
     */
    void despawnWorkerNPC(UUID workerId);

    /**
     * Spawns a temporary dummy worker to preview a schematic's blueprint navigation paths.
     */
    default void spawnDummyPreview(org.bukkit.entity.Player player, String schematicName) {}

    /**
     * Removes all visual NPCs spawned by this provider.
     */
    void despawnAll();
}
