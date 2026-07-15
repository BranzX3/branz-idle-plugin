package com.example.plugin.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry responsible for loading and caching resource definitions from resources.yml.
 */
public class ResourceRegistry implements ConfigRegistry {

    public enum Rarity {
        COMMON, UNCOMMON, RARE, EPIC, LEGENDARY
    }

    public record ResourceDefinition(
        String key,
        String displayName,
        Material material,
        int customModelData,
        Rarity rarity
    ) {}

    private final Map<String, ResourceDefinition> resources = new HashMap<>();

    @Override
    public void load(YamlConfiguration config) {
        clear();
        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) continue;

            String displayName = section.getString("display_name", key);
            String matName = section.getString("material", "STONE");
            Material material = Material.matchMaterial(matName);
            if (material == null) {
                material = Material.STONE;
            }
            int customModelData = section.getInt("custom_model_data", 0);
            Rarity rarity;
            try {
                rarity = Rarity.valueOf(section.getString("rarity", "COMMON").toUpperCase());
            } catch (IllegalArgumentException e) {
                rarity = Rarity.COMMON;
            }

            ResourceDefinition def = new ResourceDefinition(key, displayName, material, customModelData, rarity);
            resources.put(key, def);
        }
    }

    @Override
    public void clear() {
        resources.clear();
    }

    public Optional<ResourceDefinition> getResource(String key) {
        return Optional.ofNullable(resources.get(key));
    }

    public Map<String, ResourceDefinition> getAllResources() {
        return Collections.unmodifiableMap(resources);
    }
}
