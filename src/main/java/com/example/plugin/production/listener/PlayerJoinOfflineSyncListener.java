package com.example.plugin.production.listener;

import com.example.plugin.production.service.ProductionService;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Listener intercepting player logins to trigger offline production catch-up calculations asynchronously.
 */
public class PlayerJoinOfflineSyncListener implements Listener {

    private final JavaPlugin plugin;
    private final ProductionService productionService;

    public PlayerJoinOfflineSyncListener(JavaPlugin plugin, ProductionService productionService) {
        this.plugin = plugin;
        this.productionService = productionService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Execute short delay after login to ensure player data and profile are fully loaded
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                productionService.calculateOfflineDelta(event.getPlayer());
            }
        }, 40L); // 2 seconds delay
    }
}
