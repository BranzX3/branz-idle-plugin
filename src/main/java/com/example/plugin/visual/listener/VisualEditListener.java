package com.example.plugin.visual.listener;

import com.example.plugin.visual.model.VisualEditSession;
import com.example.plugin.visual.model.WorkshopBlueprint.MarkerDefinition;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener capturing Visual Wand clicks to register, cycle types/animations, and manage blueprint markers in Edit Mode.
 */
public class VisualEditListener implements Listener {

    private static final Map<UUID, VisualEditSession> activeSessions = new ConcurrentHashMap<>();

    public static Map<UUID, VisualEditSession> getActiveSessions() {
        return activeSessions;
    }

    public static void startSession(UUID playerId, VisualEditSession session) {
        activeSessions.put(playerId, session);
    }

    public static VisualEditSession removeSession(UUID playerId) {
        return activeSessions.remove(playerId);
    }

    @EventHandler
    public void onWandUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        VisualEditSession session = activeSessions.get(player.getUniqueId());
        if (session == null) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.BLAZE_ROD) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName() || !meta.getDisplayName().contains("Visual Editor Wand")) return;

        event.setCancelled(true);

        Block block = event.getClickedBlock();
        if (block == null) return;

        Location origin = session.getOriginLocation();
        double rx = block.getX() - origin.getX() + 0.5;
        double ry = block.getY() - origin.getY() + 1.0;
        double rz = block.getZ() - origin.getZ() + 0.5;

        // Shift Click: Actions
        if (player.isSneaking()) {
            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                // Shift + Left-Click: Cycle actions of closest marker
                String closestKey = findClosestMarker(session, rx, ry, rz);
                if (closestKey != null) {
                    MarkerDefinition m = session.getMarkers().get(closestKey);
                    String current = m.getActions().isEmpty() ? "NONE" : m.getActions().get(0);
                    String next = switch (current.toUpperCase()) {
                        case "SWING" -> "LOOK";
                        case "LOOK" -> "BREAK";
                        case "BREAK" -> "INSPECT";
                        case "INSPECT" -> "FISH";
                        case "FISH" -> "SIT";
                        default -> "SWING";
                    };
                    m.getActions().clear();
                    m.getActions().add(next);
                    player.sendMessage("§b[Editor] Cycled marker §e" + closestKey + " §banimation to: §e" + next);
                } else {
                    player.sendMessage("§cNo marker found near clicked block.");
                }
            } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                // Shift + Right-Click: Delete nearest marker
                String closestKey = findClosestMarker(session, rx, ry, rz);
                if (closestKey != null) {
                    session.getMarkers().remove(closestKey);
                    player.sendMessage("§c[Editor] Removed marker: §e" + closestKey);
                } else {
                    player.sendMessage("§cNo marker found near clicked block.");
                }
            }
            return;
        }

        // Left-Click: Add Work Point
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            String closestKey = findClosestMarker(session, rx, ry, rz);
            if (closestKey != null) {
                player.sendMessage("§e[Editor] Marker already exists here: §f" + closestKey);
                return;
            }

            String markerName = "marker_" + (session.getMarkers().size() + 1);
            MarkerDefinition mDef = new MarkerDefinition(rx, ry, rz);
            mDef.setType("WORK");
            mDef.getActions().add("SWING");
            session.getMarkers().put(markerName, mDef);

            player.sendMessage("§a[Editor] Added §eWORK Marker §a(" + markerName + ") at relative: §f" + String.format("%.1f, %.1f, %.1f", rx, ry, rz));
        }
        // Right-Click: Cycle marker type / Add Rest Point
        else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            String closestKey = findClosestMarker(session, rx, ry, rz);
            if (closestKey != null) {
                MarkerDefinition m = session.getMarkers().get(closestKey);
                String currentType = m.getType();
                String nextType = switch (currentType.toUpperCase()) {
                    case "WORK" -> "REST";
                    case "REST" -> "STORAGE";
                    case "STORAGE" -> "QUEUE";
                    default -> "WORK";
                };
                m.setType(nextType);

                // Set default action for types
                m.getActions().clear();
                switch (nextType) {
                    case "REST" -> m.getActions().add("SIT");
                    case "WORK" -> m.getActions().add("SWING");
                    default -> m.getActions().add("LOOK");
                }

                player.sendMessage("§e[Editor] Cycled marker §a" + closestKey + " §etype to: §6" + nextType);
            } else {
                // Add new REST marker directly
                String markerName = "marker_" + (session.getMarkers().size() + 1);
                MarkerDefinition mDef = new MarkerDefinition(rx, ry, rz);
                mDef.setType("REST");
                mDef.getActions().add("SIT");
                session.getMarkers().put(markerName, mDef);

                player.sendMessage("§d[Editor] Added §eREST Marker §d(" + markerName + ") at relative: §f" + String.format("%.1f, %.1f, %.1f", rx, ry, rz));
            }
        }
    }

    private String findClosestMarker(VisualEditSession session, double x, double y, double z) {
        String closest = null;
        double bestDist = 2.25; // max 1.5 blocks distance squared
        for (Map.Entry<String, MarkerDefinition> entry : session.getMarkers().entrySet()) {
            MarkerDefinition m = entry.getValue();
            double dx = m.getX() - x;
            double dy = m.getY() - y;
            double dz = m.getZ() - z;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq < bestDist) {
                bestDist = distSq;
                closest = entry.getKey();
            }
        }
        return closest;
    }
}
