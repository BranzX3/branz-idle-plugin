package com.example.plugin.integration.vault;

import com.example.plugin.bootstrap.ProviderManager;
import com.example.plugin.economy.service.EconomyService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Bridge layer linking Branz.Idle EconomyService with Vault API if Vault is present on the server.
 */
public class VaultBridge {

    private final JavaPlugin plugin;
    private final ProviderManager providerManager;
    private final EconomyService economyService;
    private Object vaultEconomy;

    public VaultBridge(JavaPlugin plugin, ProviderManager providerManager, EconomyService economyService) {
        this.plugin = plugin;
        this.providerManager = providerManager;
        this.economyService = economyService;
    }

    public void initialize() {
        if (!providerManager.hasVault()) {
            plugin.getLogger().info("[VaultBridge] Vault not detected; using standalone EconomyService.");
            return;
        }

        try {
            Class<?> econClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> rsp = Bukkit.getServicesManager().getRegistration(econClass);
            if (rsp != null) {
                this.vaultEconomy = rsp.getProvider();
                plugin.getLogger().info("[VaultBridge] Successfully hooked into external Vault economy provider!");
            } else {
                plugin.getLogger().info("[VaultBridge] Vault detected, but no external economy plugin found. Running standalone economy.");
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[VaultBridge] Could not initialize Vault bridge: " + t.getMessage());
        }
    }

    public boolean hasVaultHook() {
        return vaultEconomy != null;
    }

    public double getVaultBalance(UUID playerId) {
        if (vaultEconomy == null) return economyService.getProfile(playerId).map(com.example.plugin.economy.model.PlayerProfile::getCoins).orElse(0.0);
        try {
            Method getBalMethod = vaultEconomy.getClass().getMethod("getBalance", org.bukkit.OfflinePlayer.class);
            org.bukkit.OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
            return (double) getBalMethod.invoke(vaultEconomy, player);
        } catch (Exception e) {
            return economyService.getProfile(playerId).map(com.example.plugin.economy.model.PlayerProfile::getCoins).orElse(0.0);
        }
    }

    public boolean depositVault(UUID playerId, double amount) {
        if (vaultEconomy == null) {
            economyService.addCoins(playerId, amount);
            return true;
        }
        try {
            Method depositMethod = vaultEconomy.getClass().getMethod("depositPlayer", org.bukkit.OfflinePlayer.class, double.class);
            org.bukkit.OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
            depositMethod.invoke(vaultEconomy, player, amount);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean withdrawVault(UUID playerId, double amount) {
        if (vaultEconomy == null) {
            return economyService.removeCoins(playerId, amount);
        }
        try {
            Method withdrawMethod = vaultEconomy.getClass().getMethod("withdrawPlayer", org.bukkit.OfflinePlayer.class, double.class);
            org.bukkit.OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
            Object response = withdrawMethod.invoke(vaultEconomy, player, amount);
            Method typeMethod = response.getClass().getMethod("type");
            Object type = typeMethod.invoke(response);
            return type.toString().equals("SUCCESS");
        } catch (Exception e) {
            return false;
        }
    }
}
