package com.example.plugin.integration.points;

import com.example.plugin.bootstrap.ProviderManager;
import com.example.plugin.economy.service.EconomyService;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Bridge layer linking Branz.Idle with PlayerPoints API for premium currency operations.
 */
public class PlayerPointsBridge {

    private final JavaPlugin plugin;
    private final ProviderManager providerManager;
    private final EconomyService economyService;
    private PlayerPointsAPI pointsAPI;

    public PlayerPointsBridge(JavaPlugin plugin, ProviderManager providerManager, EconomyService economyService) {
        this.plugin = plugin;
        this.providerManager = providerManager;
        this.economyService = economyService;
    }

    public void initialize() {
        if (!providerManager.hasPlayerPoints()) {
            plugin.getLogger().info("[PlayerPointsBridge] PlayerPoints not detected; using standalone local Diamonds db.");
            return;
        }

        try {
            this.pointsAPI = PlayerPoints.getInstance().getAPI();
            plugin.getLogger().info("[PlayerPointsBridge] Successfully hooked into PlayerPoints API!");
        } catch (Throwable t) {
            plugin.getLogger().warning("[PlayerPointsBridge] Could not hook into PlayerPoints API: " + t.getMessage());
        }
    }

    public boolean hasHook() {
        return pointsAPI != null;
    }

    public long getPoints(UUID playerId) {
        if (pointsAPI == null) {
            return economyService.getProfile(playerId)
                .map(com.example.plugin.economy.model.PlayerProfile::getDiamonds)
                .orElse(0L);
        }
        try {
            return pointsAPI.look(playerId);
        } catch (Exception e) {
            return economyService.getProfile(playerId)
                .map(com.example.plugin.economy.model.PlayerProfile::getDiamonds)
                .orElse(0L);
        }
    }

    public boolean givePoints(UUID playerId, long amount) {
        if (pointsAPI == null) {
            economyService.addDiamonds(playerId, amount);
            return true;
        }
        try {
            return pointsAPI.give(playerId, (int) amount);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean takePoints(UUID playerId, long amount) {
        if (pointsAPI == null) {
            return economyService.removeDiamonds(playerId, amount);
        }
        try {
            return pointsAPI.take(playerId, (int) amount);
        } catch (Exception e) {
            return false;
        }
    }
}
