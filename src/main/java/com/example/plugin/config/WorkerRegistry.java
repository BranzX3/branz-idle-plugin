package com.example.plugin.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry responsible for loading and caching worker templates from workers.yml.
 */
public class WorkerRegistry implements ConfigRegistry {

    public record WorkerTemplate(
        String templateId,
        String displayName,
        String profession,
        ResourceRegistry.Rarity rarity,
        String citizensSkin,
        double speedBonus,
        double yieldBonus,
        double rareDropBonus,
        double growthPerLevel
    ) {}

    private final Map<String, WorkerTemplate> templates = new HashMap<>();

    @Override
    public void load(YamlConfiguration config) {
        clear();
        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) continue;

            String displayName = section.getString("display_name", key);
            String profession = section.getString("profession", "MINING").toUpperCase();
            ResourceRegistry.Rarity rarity;
            try {
                rarity = ResourceRegistry.Rarity.valueOf(section.getString("rarity", "COMMON").toUpperCase());
            } catch (IllegalArgumentException e) {
                rarity = ResourceRegistry.Rarity.COMMON;
            }
            String skin = section.getString("citizens_skin", "Steve");

            ConfigurationSection statsSection = section.getConfigurationSection("base_stats");
            double speed = statsSection != null ? statsSection.getDouble("speed_bonus", 0.0) : 0.0;
            double yield = statsSection != null ? statsSection.getDouble("yield_bonus", 0.0) : 0.0;
            double rare = statsSection != null ? statsSection.getDouble("rare_drop_bonus", 0.0) : 0.0;
            double growth = section.getDouble("growth_per_level", 0.05);

            WorkerTemplate template = new WorkerTemplate(key, displayName, profession, rarity, skin, speed, yield, rare, growth);
            templates.put(key, template);
        }
    }

    @Override
    public void clear() {
        templates.clear();
    }

    public Optional<WorkerTemplate> getTemplate(String templateId) {
        return Optional.ofNullable(templates.get(templateId));
    }

    public Map<String, WorkerTemplate> getAllTemplates() {
        return Collections.unmodifiableMap(templates);
    }
}
