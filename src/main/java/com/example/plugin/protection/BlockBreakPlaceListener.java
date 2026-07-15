package com.example.plugin.protection;

import com.example.plugin.territory.event.TerritoryAccessDeniedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

/**
 * Listener securing block breaking, placing, and bucket usage inside protected territory.
 */
public class BlockBreakPlaceListener implements Listener {

    private final ProtectionService protectionService;

    public BlockBreakPlaceListener(ProtectionService protectionService) {
        this.protectionService = protectionService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!protectionService.hasAccess(event.getPlayer(), event.getBlock().getLocation(), TerritoryAccessDeniedEvent.ActionType.BLOCK_BREAK)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!protectionService.hasAccess(event.getPlayer(), event.getBlock().getLocation(), TerritoryAccessDeniedEvent.ActionType.BLOCK_PLACE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!protectionService.hasAccess(event.getPlayer(), event.getBlock().getLocation(), TerritoryAccessDeniedEvent.ActionType.BLOCK_PLACE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (!protectionService.hasAccess(event.getPlayer(), event.getBlock().getLocation(), TerritoryAccessDeniedEvent.ActionType.BLOCK_BREAK)) {
            event.setCancelled(true);
        }
    }
}
