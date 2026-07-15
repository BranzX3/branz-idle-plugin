package com.example.plugin.visual.provider;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Visual provider implementation leveraging Citizens API to spawn NPC workers.
 * Uses reflection to ensure clean compilation and runtime safety without hard class dependencies.
 */
public class CitizensVisualProvider implements VisualProvider {

    private final JavaPlugin plugin;
    private final Map<UUID, Object> spawnedNpcs = new ConcurrentHashMap<>();

    public CitizensVisualProvider(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void spawnWorkerNPC(UUID workerId, String templateId, Location location) {
        if (location == null || location.getWorld() == null) return;
        try {
            despawnWorkerNPC(workerId);

            Class<?> citizensApiClass = Class.forName("net.citizensnpcs.api.CitizensAPI");
            Method getRegistryMethod = citizensApiClass.getMethod("getNPCRegistry");
            Object registry = getRegistryMethod.invoke(null);

            Method createNpcMethod = registry.getClass().getMethod("createNPC", EntityType.class, String.class);
            Object npc = createNpcMethod.invoke(registry, EntityType.PLAYER, "Worker: " + templateId);

            Method spawnMethod = npc.getClass().getMethod("spawn", Location.class);
            spawnMethod.invoke(npc, location);

            spawnedNpcs.put(workerId, npc);
        } catch (Exception e) {
            plugin.getLogger().warning("[CitizensVisualProvider] Could not spawn NPC for worker " + workerId + ": " + e.getMessage());
        }
    }

    @Override
    public void despawnWorkerNPC(UUID workerId) {
        Object npc = spawnedNpcs.remove(workerId);
        if (npc != null) {
            try {
                Method destroyMethod = npc.getClass().getMethod("destroy");
                destroyMethod.invoke(npc);
            } catch (Exception e) {
                // Ignore destruction errors
            }
        }
    }

    @Override
    public void despawnAll() {
        for (UUID workerId : spawnedNpcs.keySet()) {
            despawnWorkerNPC(workerId);
        }
        spawnedNpcs.clear();
    }
}
