package com.example.plugin.visual.provider;

import org.bukkit.Location;

import java.util.UUID;

/**
 * Fallback visual provider used when Citizens NPC plugin is not installed on the server.
 */
public class NoOpVisualProvider implements VisualProvider {

    @Override
    public void spawnWorkerNPC(UUID workerId, String templateId, Location location) {
        // No-op fallback
    }

    @Override
    public void despawnWorkerNPC(UUID workerId) {
        // No-op fallback
    }

    @Override
    public void despawnAll() {
        // No-op fallback
    }
}
