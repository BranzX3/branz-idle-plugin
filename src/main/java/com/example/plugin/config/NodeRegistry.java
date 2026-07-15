package com.example.plugin.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry responsible for loading and caching node tier definitions from nodes.yml.
 */
public class NodeRegistry implements ConfigRegistry {

    public record NodeDefinition(
        String tierKey,
        String nodeType,
        int sizeChunks,
        String schematic,
        Map<String, Long> upgradeCost,
        int workerSlots,
        long maxStorageCapacity,
        int baseTickSeconds
    ) {}

    private final Map<String, NodeDefinition> nodes = new HashMap<>();

    @Override
    public void load(YamlConfiguration config) {
        clear();
        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) continue;

            String nodeType = section.getString("node_type", "MINING").toUpperCase();
            int sizeChunks = section.getInt("size_chunks", 1);
            String schematic = section.getString("schematic", "default.schem");
            int workerSlots = section.getInt("worker_slots", 2);
            long maxCap = section.getLong("max_storage_capacity", 5000L);
            int tickSeconds = section.getInt("base_tick_seconds", 60);

            Map<String, Long> upgradeCost = new HashMap<>();
            ConfigurationSection costSection = section.getConfigurationSection("upgrade_cost");
            if (costSection != null) {
                for (String costKey : costSection.getKeys(false)) {
                    upgradeCost.put(costKey, costSection.getLong(costKey, 0L));
                }
            }

            NodeDefinition def = new NodeDefinition(key, nodeType, sizeChunks, schematic, upgradeCost, workerSlots, maxCap, tickSeconds);
            nodes.put(key, def);
        }
    }

    @Override
    public void clear() {
        nodes.clear();
    }

    public Optional<NodeDefinition> getNodeDefinition(String tierKey) {
        return Optional.ofNullable(nodes.get(tierKey));
    }

    public Map<String, NodeDefinition> getAllNodes() {
        return Collections.unmodifiableMap(nodes);
    }
}
