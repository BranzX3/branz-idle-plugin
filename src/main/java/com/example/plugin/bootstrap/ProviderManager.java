package com.example.plugin.bootstrap;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Detects and manages soft dependencies (Citizens, FastAsyncWorldEdit, WorldEdit, Vault).
 * Ensures safe fallback handling without throwing ClassDefFoundError.
 */
public class ProviderManager {

    private final JavaPlugin plugin;
    private boolean citizensEnabled;
    private boolean faweEnabled;
    private boolean worldEditEnabled;
    private boolean vaultEnabled;

    public ProviderManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Checks Bukkit PluginManager for installed soft dependencies.
     */
    public void detectProviders() {
        this.citizensEnabled = checkPlugin("Citizens");
        this.faweEnabled = checkPlugin("FastAsyncWorldEdit");
        this.worldEditEnabled = checkPlugin("WorldEdit");
        this.vaultEnabled = checkPlugin("Vault");

        plugin.getLogger().info(String.format(
            "[ProviderManager] Detected Providers -> Citizens: %b | FAWE: %b | WorldEdit: %b | Vault: %b",
            citizensEnabled, faweEnabled, worldEditEnabled, vaultEnabled
        ));
    }

    private boolean checkPlugin(String pluginName) {
        Plugin target = Bukkit.getPluginManager().getPlugin(pluginName);
        return target != null && target.isEnabled();
    }

    public boolean hasCitizens() {
        return citizensEnabled;
    }

    public boolean hasFAWE() {
        return faweEnabled;
    }

    public boolean hasWorldEdit() {
        return worldEditEnabled;
    }

    public boolean hasVault() {
        return vaultEnabled;
    }
}
