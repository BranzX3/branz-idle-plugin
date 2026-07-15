package com.example.plugin.protection;

import com.example.plugin.territory.event.TerritoryAccessDeniedEvent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Listener securing containers, redstone mechanisms, armor stands, and entities inside territory.
 */
public class EntityInteractListener implements Listener {

    private final ProtectionService protectionService;

    public EntityInteractListener(ProtectionService protectionService) {
        this.protectionService = protectionService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        Player player = event.getPlayer();
        Material mat = block.getType();

        if (isContainer(mat)) {
            if (!protectionService.hasAccess(player, block.getLocation(), TerritoryAccessDeniedEvent.ActionType.CONTAINER_ACCESS)) {
                event.setCancelled(true);
            }
        } else if (isRedstoneMechanism(mat) || event.getAction().isRightClick()) {
            if (!protectionService.hasAccess(player, block.getLocation(), TerritoryAccessDeniedEvent.ActionType.REDSTONE_INTERACT)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            Entity target = event.getEntity();
            if (!protectionService.hasAccess(player, target.getLocation(), TerritoryAccessDeniedEvent.ActionType.ENTITY_DAMAGE)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!protectionService.hasAccess(event.getPlayer(), event.getRightClicked().getLocation(), TerritoryAccessDeniedEvent.ActionType.ENTITY_DAMAGE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (!protectionService.hasAccess(event.getPlayer(), event.getRightClicked().getLocation(), TerritoryAccessDeniedEvent.ActionType.ENTITY_DAMAGE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (event.getRemover() instanceof Player player) {
            if (!protectionService.hasAccess(player, event.getEntity().getLocation(), TerritoryAccessDeniedEvent.ActionType.BLOCK_BREAK)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        if (event.getPlayer() != null) {
            if (!protectionService.hasAccess(event.getPlayer(), event.getBlock().getLocation(), TerritoryAccessDeniedEvent.ActionType.BLOCK_PLACE)) {
                event.setCancelled(true);
            }
        }
    }

    private boolean isContainer(Material mat) {
        String name = mat.name();
        return name.contains("CHEST") || name.contains("SHULKER_BOX") || name.contains("BARREL")
            || name.contains("FURNACE") || name.contains("SMOKER") || name.contains("BLAST_FURNACE")
            || name.contains("HOPPER") || name.contains("DISPENSER") || name.contains("DROPPER")
            || name.contains("BREWING_STAND");
    }

    private boolean isRedstoneMechanism(Material mat) {
        String name = mat.name();
        return name.contains("DOOR") || name.contains("GATE") || name.contains("TRAPDOOR")
            || name.contains("BUTTON") || name.contains("LEVER") || name.contains("COMPARATOR")
            || name.contains("REPEATER") || name.contains("PRESSURE_PLATE");
    }
}
