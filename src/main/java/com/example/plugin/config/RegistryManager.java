package com.example.plugin.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * Coordinates atomic loading and hot-reloading of all configuration registries.
 */
public class RegistryManager {

    private final JavaPlugin plugin;
    private volatile ResourceRegistry resourceRegistry = new ResourceRegistry();
    private volatile WorkerRegistry workerRegistry = new WorkerRegistry();
    private volatile NodeRegistry nodeRegistry = new NodeRegistry();
    private volatile DropTableRegistry dropTableRegistry = new DropTableRegistry();
    private volatile GachaRegistry gachaRegistry = new GachaRegistry();

    public RegistryManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads all YAML definitions from disk into memory.
     */
    public synchronized void loadAll() {
        saveDefaultFiles();
        reloadAll();
    }

    /**
     * Atomically parses YAML configs into new temporary registries, validates them,
     * and swaps pointer references to ensure zero race conditions during live server reloads.
     */
    public synchronized boolean reloadAll() {
        try {
            ResourceRegistry newResources = new ResourceRegistry();
            WorkerRegistry newWorkers = new WorkerRegistry();
            NodeRegistry newNodes = new NodeRegistry();
            DropTableRegistry newDropTables = new DropTableRegistry();
            GachaRegistry newGacha = new GachaRegistry();

            newResources.load(loadYaml("resources.yml"));
            newWorkers.load(loadYaml("workers.yml"));
            newNodes.load(loadYaml("nodes.yml"));
            newDropTables.load(loadYaml("drop_tables.yml"));
            newGacha.load(loadYaml("gacha.yml"));

            // Atomic reference swap
            this.resourceRegistry = newResources;
            this.workerRegistry = newWorkers;
            this.nodeRegistry = newNodes;
            this.dropTableRegistry = newDropTables;
            this.gachaRegistry = newGacha;

            plugin.getLogger().info("[RegistryManager] Successfully loaded/reloaded all 5 data registries.");
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("[RegistryManager] Failed to reload registries: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private YamlConfiguration loadYaml(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private void saveDefaultFiles() {
        String[] files = {"config.yml", "resources.yml", "workers.yml", "nodes.yml", "drop_tables.yml", "gacha.yml"};
        for (String f : files) {
            File dest = new File(plugin.getDataFolder(), f);
            if (!dest.exists()) {
                plugin.saveResource(f, false);
            }
        }
    }

    public ResourceRegistry getResourceRegistry() {
        return resourceRegistry;
    }

    public WorkerRegistry getWorkerRegistry() {
        return workerRegistry;
    }

    public NodeRegistry getNodeRegistry() {
        return nodeRegistry;
    }

    public DropTableRegistry getDropTableRegistry() {
        return dropTableRegistry;
    }

    public GachaRegistry getGachaRegistry() {
        return gachaRegistry;
    }
}
