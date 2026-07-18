package com.example.plugin.visual.provider;

import com.example.plugin.visual.model.WorkshopBlueprint;
import com.example.plugin.visual.model.WorkshopBlueprint.MarkerDefinition;
import com.example.plugin.visual.service.OccupationManager;
import com.example.plugin.visual.service.WorkshopBlueprintManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Visual provider implementation leveraging Citizens API to spawn NPC workers.
 * Uses reflection to ensure clean compilation and runtime safety without hard class dependencies.
 * Governs the advanced data-driven Behavior Framework with occupancy checks, paths, and blackboard states.
 */
public class CitizensVisualProvider implements VisualProvider {

    private final JavaPlugin plugin;
    private final Map<UUID, Object> spawnedNpcs = new ConcurrentHashMap<>();
    private final Map<UUID, WorkerNpcState> activeStates = new ConcurrentHashMap<>();
    private int tickTaskId = -1;

    private static class WorkerNpcState {
        final Object npc;
        final Location originLocation; // Node chunk origin Location
        final String schematicName;
        final UUID workerId;
        final UUID nodeId;
        final String profession;

        // Blackboard Memory
        String state = "RESTING";
        int timer = 0;
        String lastMarker = null;
        String reservedMarker = null;
        List<Location> activePathPoints = null;
        int pathIndex = 0;
        String currentAnimation = "NONE";
        List<String> allowedActions = new ArrayList<>();

        WorkerNpcState(Object npc, Location originLocation, String schematicName, UUID workerId, UUID nodeId, String profession) {
            this.npc = npc;
            this.originLocation = originLocation;
            this.schematicName = schematicName;
            this.workerId = workerId;
            this.nodeId = nodeId;
            this.profession = profession.toUpperCase();
        }
    }

    public CitizensVisualProvider(JavaPlugin plugin) {
        this.plugin = plugin;
        startStateTicker();
    }

    private void startStateTicker() {
        tickTaskId = Bukkit.getScheduler().runTaskTimer(plugin, this::tickAllStates, 20L, 20L).getTaskId();
    }

    private void tickAllStates() {
        try {
            com.example.plugin.bootstrap.BranzIdlePlugin mainPlugin = JavaPlugin.getPlugin(com.example.plugin.bootstrap.BranzIdlePlugin.class);
            WorkshopBlueprintManager bpManager = mainPlugin.getServiceRegistry().getRequiredService(WorkshopBlueprintManager.class);
            OccupationManager occManager = mainPlugin.getServiceRegistry().getRequiredService(OccupationManager.class);

            for (Map.Entry<UUID, WorkerNpcState> entry : activeStates.entrySet()) {
                WorkerNpcState ws = entry.getValue();
                if (!isNpcSpawned(ws.npc)) continue;

                ws.timer++;
                WorkshopBlueprint bp = bpManager.getBlueprint(ws.schematicName);
                World world = ws.originLocation.getWorld();

                switch (ws.state) {
                    case "RESTING" -> {
                        // Check if we've been resting or if we were waiting in queue and a work slot opened up
                        boolean triggerWork = ws.timer >= 15;
                        if ("QUEUE".equalsIgnoreCase(ws.reservedMarker) && ws.timer % 2 == 0) {
                            // Periodically check if a work slot is now free
                            String freeWorkMarker = findFreeMarker(ws, bp, occManager, "WORK", world);
                            if (freeWorkMarker != null) {
                                occManager.release(ws.nodeId, ws.reservedMarker, ws.workerId);
                                occManager.reserve(ws.nodeId, freeWorkMarker, ws.workerId);
                                ws.reservedMarker = freeWorkMarker;
                                setupNavigation(ws, bp, freeWorkMarker);
                                ws.state = "WALKING_TO_WORK";
                                ws.timer = 0;
                                triggerWork = false;
                            }
                        }

                        if (triggerWork) {
                            String chosenMarker = findFreeMarker(ws, bp, occManager, "WORK", world);
                            if (chosenMarker != null) {
                                occManager.reserve(ws.nodeId, chosenMarker, ws.workerId);
                                ws.reservedMarker = chosenMarker;
                                setupNavigation(ws, bp, chosenMarker);
                                ws.state = "WALKING_TO_WORK";
                            } else {
                                // No work slots free! Attempt to occupy a QUEUE marker
                                String queueMarker = findFreeMarker(ws, bp, occManager, "QUEUE", world);
                                if (queueMarker != null) {
                                    occManager.reserve(ws.nodeId, queueMarker, ws.workerId);
                                    ws.reservedMarker = queueMarker;
                                    setupNavigation(ws, bp, queueMarker);
                                    ws.state = "WALKING_TO_WORK";
                                } else {
                                    // Remain resting
                                    ws.timer = 0;
                                }
                            }
                            ws.timer = 0;
                        }
                    }
                    case "WALKING_TO_WORK" -> {
                        if (ws.activePathPoints != null && !ws.activePathPoints.isEmpty()) {
                            Location currentTarget = ws.activePathPoints.get(ws.pathIndex);
                            org.bukkit.entity.Entity entity = getNpcEntity(ws.npc);
                            if (entity != null && (entity.getLocation().distanceSquared(currentTarget) < 2.25 || !isNavigating(ws.npc) || ws.timer >= 15)) {
                                ws.pathIndex++;
                                ws.timer = 0;
                                if (ws.pathIndex < ws.activePathPoints.size()) {
                                    navigateTo(ws.npc, ws.activePathPoints.get(ws.pathIndex));
                                } else {
                                    ws.activePathPoints = null;
                                    arriveAtWork(ws, bp);
                                }
                            }
                        } else {
                            if (!isNavigating(ws.npc) || ws.timer >= 15) {
                                arriveAtWork(ws, bp);
                            }
                        }
                    }
                    case "WORKING" -> {
                        // Play action animations
                        if (ws.timer % 2 == 0) {
                            playRandomAnimation(ws);
                        }
                        if (ws.timer >= 20) { // Work duration
                            occManager.release(ws.nodeId, ws.reservedMarker, ws.workerId);
                            ws.lastMarker = ws.reservedMarker;

                            String restMarker = findFreeMarker(ws, bp, occManager, "REST", world);
                            if (restMarker == null) {
                                restMarker = "rest_default"; // fallback
                            }
                            occManager.reserve(ws.nodeId, restMarker, ws.workerId);
                            ws.reservedMarker = restMarker;
                            setupNavigation(ws, bp, restMarker);
                            ws.state = "WALKING_TO_REST";
                            ws.timer = 0;
                        }
                    }
                    case "WALKING_TO_REST" -> {
                        if (ws.activePathPoints != null && !ws.activePathPoints.isEmpty()) {
                            Location currentTarget = ws.activePathPoints.get(ws.pathIndex);
                            org.bukkit.entity.Entity entity = getNpcEntity(ws.npc);
                            if (entity != null && (entity.getLocation().distanceSquared(currentTarget) < 2.25 || !isNavigating(ws.npc) || ws.timer >= 15)) {
                                ws.pathIndex++;
                                ws.timer = 0;
                                if (ws.pathIndex < ws.activePathPoints.size()) {
                                    navigateTo(ws.npc, ws.activePathPoints.get(ws.pathIndex));
                                } else {
                                    ws.activePathPoints = null;
                                    ws.state = "RESTING";
                                    ws.timer = 0;
                                }
                            }
                        } else {
                            if (!isNavigating(ws.npc) || ws.timer >= 15) {
                                ws.state = "RESTING";
                                ws.timer = 0;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[CitizensVisualProvider] Error in behavior ticker: " + e.getMessage());
        }
    }

    private void arriveAtWork(WorkerNpcState ws, WorkshopBlueprint bp) {
        ws.state = "WORKING";
        ws.timer = 0;

        // Resolve allowed actions/animations for current marker
        MarkerDefinition mDef = bp.getMarkers().get(ws.reservedMarker);
        if (mDef != null) {
            ws.allowedActions.clear();
            List<String> overrides = mDef.getProfessionOverrides().get(ws.profession);
            if (overrides != null && !overrides.isEmpty()) {
                ws.allowedActions.addAll(overrides);
            } else {
                ws.allowedActions.addAll(mDef.getActions());
            }
        }
    }

    private void setupNavigation(WorkerNpcState ws, WorkshopBlueprint bp, String targetMarker) {
        MarkerDefinition mDef = bp.getMarkers().get(targetMarker);
        if (mDef == null) return;

        // Calculate offset with radius randomization
        double rx = mDef.getX();
        double ry = mDef.getY();
        double rz = mDef.getZ();
        double rad = mDef.getRadius();
        if (rad > 0.0) {
            double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
            double r = ThreadLocalRandom.current().nextDouble() * rad;
            rx += r * Math.cos(angle);
            rz += r * Math.sin(angle);
        }

        Location finalTarget = new Location(
            ws.originLocation.getWorld(),
            ws.originLocation.getX() + rx,
            ws.originLocation.getY() + ry,
            ws.originLocation.getZ() + rz
        );

        // Check if there is a mapped path from lastMarker to targetMarker
        String pathKey = ws.lastMarker + "_to_" + targetMarker;
        List<String> markerPath = bp.getPaths().get(pathKey);
        if (markerPath == null) {
            // Check if generic "work_route" or "rest_route" is available
            markerPath = bp.getPaths().get("work_route");
        }

        if (markerPath != null && !markerPath.isEmpty()) {
            ws.activePathPoints = new ArrayList<>();
            for (String markerName : markerPath) {
                MarkerDefinition nodeDef = bp.getMarkers().get(markerName);
                if (nodeDef != null) {
                    ws.activePathPoints.add(new Location(
                        ws.originLocation.getWorld(),
                        ws.originLocation.getX() + nodeDef.getX(),
                        ws.originLocation.getY() + nodeDef.getY(),
                        ws.originLocation.getZ() + nodeDef.getZ()
                    ));
                }
            }
            // Add final target block
            ws.activePathPoints.add(finalTarget);
            ws.pathIndex = 0;
            navigateTo(ws.npc, ws.activePathPoints.get(0));
        } else {
            ws.activePathPoints = null;
            navigateTo(ws.npc, finalTarget);
        }
    }

    private String findFreeMarker(WorkerNpcState ws, WorkshopBlueprint bp, OccupationManager occManager, String type, World world) {
        List<String> candidates = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        double totalWeight = 0.0;

        long time = world.getTime();
        boolean isNight = time >= 12000 && time < 24000;
        boolean isStorming = world.hasStorm();

        for (Map.Entry<String, MarkerDefinition> entry : bp.getMarkers().entrySet()) {
            MarkerDefinition m = entry.getValue();
            if (!m.getType().equalsIgnoreCase(type)) continue;

            // Check condition time
            if ("DAY".equalsIgnoreCase(m.getConditionTime()) && isNight) continue;
            if ("NIGHT".equalsIgnoreCase(m.getConditionTime()) && !isNight) continue;

            // Check condition weather
            if ("RAIN".equalsIgnoreCase(m.getConditionWeather()) && !isStorming) continue;
            if ("SUN".equalsIgnoreCase(m.getConditionWeather()) && isStorming) continue;

            // Check blackboard memory: avoid choosing same work marker back-to-back
            if (entry.getKey().equals(ws.lastMarker) && bp.getMarkers().size() > 2) continue;

            // Check occupancy (QUEUE is shared / not strictly occupied if multiple stand in queue, but WORK and REST are reserved)
            if (!"QUEUE".equalsIgnoreCase(type) && occManager.isOccupied(ws.nodeId, entry.getKey())) continue;

            candidates.add(entry.getKey());
            weights.add(m.getWeight());
            totalWeight += m.getWeight();
        }

        if (candidates.isEmpty()) return null;

        // Roll weight
        double roll = ThreadLocalRandom.current().nextDouble() * totalWeight;
        double accum = 0.0;
        for (int i = 0; i < candidates.size(); i++) {
            accum += weights.get(i);
            if (roll <= accum) {
                return candidates.get(i);
            }
        }
        return candidates.get(0);
    }

    private void playRandomAnimation(WorkerNpcState ws) {
        if (ws.allowedActions.isEmpty()) {
            // Default swing
            org.bukkit.entity.Entity entity = getNpcEntity(ws.npc);
            if (entity instanceof org.bukkit.entity.LivingEntity le) {
                le.swingMainHand();
            }
            return;
        }

        String anim = ws.allowedActions.get(ThreadLocalRandom.current().nextInt(ws.allowedActions.size()));
        org.bukkit.entity.Entity entity = getNpcEntity(ws.npc);
        if (entity instanceof org.bukkit.entity.LivingEntity le) {
            switch (anim.toUpperCase()) {
                case "SWING", "PICKAXE", "AXE", "HOE" -> le.swingMainHand();
                case "LOOK" -> {
                    Location loc = le.getLocation();
                    loc.setYaw(loc.getYaw() + (ThreadLocalRandom.current().nextFloat() * 40.0f - 20.0f));
                    le.teleport(loc);
                }
            }
        }
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

            String skinName = "Steve";
            String profession = "MINER";
            try {
                com.example.plugin.bootstrap.BranzIdlePlugin mainPlugin = JavaPlugin.getPlugin(com.example.plugin.bootstrap.BranzIdlePlugin.class);
                com.example.plugin.config.RegistryManager rm = mainPlugin.getServiceRegistry().getRegistryManager();
                var tplOpt = rm.getWorkerRegistry().getTemplate(templateId);
                if (tplOpt.isPresent()) {
                    skinName = tplOpt.get().citizensSkin();
                    profession = tplOpt.get().profession();
                }
            } catch (Exception ignored) {}

            try {
                Class<?> skinTraitClass = Class.forName("net.citizensnpcs.trait.SkinTrait");
                Method getOrAddTraitMethod = npc.getClass().getMethod("getOrAddTrait", Class.class);
                Object skinTrait = getOrAddTraitMethod.invoke(npc, skinTraitClass);
                if (skinTrait != null) {
                    Method setSkinNameMethod = skinTrait.getClass().getMethod("setSkinName", String.class);
                    setSkinNameMethod.invoke(skinTrait, skinName);
                }
            } catch (Exception skinEx) {
                plugin.getLogger().warning("[CitizensVisualProvider] Could not apply skin trait for " + templateId + ": " + skinEx.getMessage());
            }

            Method spawnMethod = npc.getClass().getMethod("spawn", Location.class);
            spawnMethod.invoke(npc, location);

            String schematicName = resolveSchematicName(location);

            // Resolve nodeId
            UUID nodeId = null;
            try {
                com.example.plugin.bootstrap.BranzIdlePlugin mainPlugin = JavaPlugin.getPlugin(com.example.plugin.bootstrap.BranzIdlePlugin.class);
                var ts = mainPlugin.getServiceRegistry().getRequiredService(com.example.plugin.territory.service.TerritoryService.class);
                int cx = location.getChunk().getX();
                int cz = location.getChunk().getZ();
                nodeId = ts.getClaim(cx, cz).map(com.example.plugin.territory.model.ChunkClaim::getNodeId).orElse(null);
            } catch (Exception ignored) {}

            spawnedNpcs.put(workerId, npc);
            activeStates.put(workerId, new WorkerNpcState(npc, location, schematicName, workerId, nodeId, profession));
        } catch (Exception e) {
            plugin.getLogger().warning("[CitizensVisualProvider] Could not spawn NPC for worker " + workerId + ": " + e.getMessage());
        }
    }

    private String resolveSchematicName(Location location) {
        try {
            com.example.plugin.bootstrap.BranzIdlePlugin mainPlugin = JavaPlugin.getPlugin(com.example.plugin.bootstrap.BranzIdlePlugin.class);
            var ts = mainPlugin.getServiceRegistry().getRequiredService(com.example.plugin.territory.service.TerritoryService.class);
            var ns = mainPlugin.getServiceRegistry().getRequiredService(com.example.plugin.node.service.NodeService.class);
            var rm = mainPlugin.getServiceRegistry().getRequiredService(com.example.plugin.config.RegistryManager.class);

            int cx = location.getChunk().getX();
            int cz = location.getChunk().getZ();
            var claimOpt = ts.getClaim(cx, cz);
            if (claimOpt.isPresent() && claimOpt.get().getNodeId() != null) {
                var nodeOpt = ns.getNode(claimOpt.get().getNodeId());
                if (nodeOpt.isPresent()) {
                    var node = nodeOpt.get();
                    String style = node.getStyleId();
                    if ("default".equalsIgnoreCase(style)) {
                        var defOpt = rm.getNodeRegistry().getNodeDefinition(node.getTierKey());
                        if (defOpt.isPresent()) {
                            return defOpt.get().schematic();
                        }
                    } else {
                        return node.getNodeType().name().toLowerCase() + "_" + style + "_lv" + node.getLevel();
                    }
                }
            }
        } catch (Exception ignored) {}
        return "default_schem";
    }

    @Override
    public void despawnWorkerNPC(UUID workerId) {
        WorkerNpcState ws = activeStates.remove(workerId);
        if (ws != null && ws.nodeId != null && ws.reservedMarker != null) {
            try {
                com.example.plugin.bootstrap.BranzIdlePlugin mainPlugin = JavaPlugin.getPlugin(com.example.plugin.bootstrap.BranzIdlePlugin.class);
                mainPlugin.getServiceRegistry().getService(OccupationManager.class)
                    .ifPresent(om -> om.release(ws.nodeId, ws.reservedMarker, ws.workerId));
            } catch (Exception ignored) {}
        }

        Object npc = spawnedNpcs.remove(workerId);
        if (npc != null) {
            try {
                Method destroyMethod = npc.getClass().getMethod("destroy");
                destroyMethod.invoke(npc);
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    @Override
    public void spawnDummyPreview(org.bukkit.entity.Player player, String schematicName) {
        UUID dummyId = UUID.randomUUID();
        org.bukkit.Chunk chunk = player.getLocation().getChunk();
        Location origin = new Location(player.getWorld(), (chunk.getX() * 16) + 8.0, player.getLocation().getY(), (chunk.getZ() * 16) + 8.0);

        spawnWorkerNPC(dummyId, "Steve", origin);

        WorkerNpcState ws = activeStates.get(dummyId);
        if (ws != null) {
            activeStates.put(dummyId, new WorkerNpcState(ws.npc, origin, schematicName, dummyId, UUID.randomUUID(), "MINER"));
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                despawnWorkerNPC(dummyId);
                player.sendMessage("§c§l[Preview] §7Dummy preview for " + schematicName + " finished.");
            }, 1200L);

            player.sendMessage("§a§l[Preview] §7Dummy spawned at chunk origin center. It will auto-despawn in 60s.");
        }
    }

    @Override
    public void despawnAll() {
        if (tickTaskId != -1) {
            Bukkit.getScheduler().cancelTask(tickTaskId);
            tickTaskId = -1;
        }
        for (UUID workerId : spawnedNpcs.keySet()) {
            despawnWorkerNPC(workerId);
        }
        spawnedNpcs.clear();
        activeStates.clear();
    }

    private boolean isNpcSpawned(Object npc) {
        try {
            return (boolean) npc.getClass().getMethod("isSpawned").invoke(npc);
        } catch (Exception e) {
            return false;
        }
    }

    private org.bukkit.entity.Entity getNpcEntity(Object npc) {
        try {
            return (org.bukkit.entity.Entity) npc.getClass().getMethod("getEntity").invoke(npc);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isNavigating(Object npc) {
        try {
            Object navigator = npc.getClass().getMethod("getNavigator").invoke(npc);
            return (boolean) navigator.getClass().getMethod("isNavigating").invoke(navigator);
        } catch (Exception e) {
            return false;
        }
    }

    private void navigateTo(Object npc, Location loc) {
        try {
            Object navigator = npc.getClass().getMethod("getNavigator").invoke(npc);
            Method setTargetMethod = navigator.getClass().getMethod("setTarget", Location.class);
            setTargetMethod.invoke(navigator, loc);
        } catch (Exception e) {
            plugin.getLogger().warning("[CitizensVisualProvider] Failed to navigate NPC: " + e.getMessage());
        }
    }
}
