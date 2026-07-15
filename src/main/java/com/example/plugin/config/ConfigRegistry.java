package com.example.plugin.config;

import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Base interface for all YAML configuration-driven registries.
 */
public interface ConfigRegistry {

    /**
     * Loads or reloads definitions from the provided YamlConfiguration.
     *
     * @param config parsed Bukkit YamlConfiguration
     */
    void load(YamlConfiguration config);

    /**
     * Clears all cached definitions.
     */
    void clear();
}
