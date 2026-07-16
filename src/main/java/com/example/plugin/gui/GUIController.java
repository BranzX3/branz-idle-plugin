package com.example.plugin.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Central event controller routing inventory clicks to InventoryProvider GUIs and enforcing debounce checks.
 */
public class GUIController implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof InventoryProvider provider) {
            if (event.getClickedInventory() == event.getInventory()) {
                event.setCancelled(true);

                if (!DebounceCache.tryClick(player.getUniqueId())) {
                    return; // Double click blocked
                }

                provider.onClick(event);
            } else {
                // Click is in player's own inventory (bottom inventory)
                // Block Shift-Click to prevent injecting items directly into the custom GUI layout
                if (event.isShiftClick()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof InventoryProvider) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        DebounceCache.clearPlayer(event.getPlayer().getUniqueId());
    }
}
