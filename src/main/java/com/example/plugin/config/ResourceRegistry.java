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
        COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, MYTHIC
    }

    public enum Quality {
        POOR, NORMAL, FINE, EXCELLENT, MASTERWORK
    }

    public enum StackType {
        NORMAL, UNIQUE, SOULBOUND
    }

    public enum Category {
        MINING, LUMBER, FISHING, FARMING, RANCHING, TREASURE, SPECIAL, EVENT
    }

    public enum Tier {
        TIER_1, TIER_2, TIER_3, TIER_4, TIER_5, TIER_6, TIER_7
    }

    public enum ResourceType {
        RAW_RESOURCE, BLOCK, INGREDIENT, TREASURE, FISH
    }

    public record ResourceDefinition(
        String key,
        String displayName,
        Material material,
        int customModelData,
        Rarity rarity,
        Quality quality,
        Category category,
        String family,
        Tier tier,
        int unlockLevel,
        java.util.List<String> allowedNodes,
        java.util.List<String> zones,
        ResourceType resourceType,
        double sellPrice,
        double buyPrice,
        int exp,
        int dropWeight,
        int maxStack,
        boolean npcSell,
        boolean playerTrade,
        StackType stackType,
        java.util.List<String> tags,
        String translationKey,
        boolean enabled,
        int introducedIn
    ) {}

    private final Map<String, ResourceDefinition> resources = new HashMap<>();

    @Override
    public void load(YamlConfiguration config) {
        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) continue;

            String displayName = section.getString("display_name", key);
            
            String matName = section.getString("material");
            if (matName == null && section.isConfigurationSection("icon")) {
                matName = section.getString("icon.material");
            }
            if (matName == null) {
                matName = "STONE";
            }
            Material material = Material.matchMaterial(matName);
            if (material == null) {
                material = Material.STONE;
            }

            int customModelData = section.getInt("custom_model_data", 0);
            if (customModelData == 0 && section.isConfigurationSection("icon")) {
                customModelData = section.getInt("icon.custom_model_data", 0);
            }

            Rarity rarity = Rarity.COMMON;
            try {
                rarity = Rarity.valueOf(section.getString("rarity", "COMMON").toUpperCase());
            } catch (IllegalArgumentException ignored) {}

            Quality quality = Quality.NORMAL;
            try {
                quality = Quality.valueOf(section.getString("quality", "NORMAL").toUpperCase());
            } catch (IllegalArgumentException ignored) {}

            Category category = Category.MINING;
            try {
                category = Category.valueOf(section.getString("category", "MINING").toUpperCase());
            } catch (IllegalArgumentException ignored) {}

            String family = section.getString("family", key.toUpperCase());

            Tier tier = Tier.TIER_1;
            try {
                tier = Tier.valueOf(section.getString("tier", "TIER_1").toUpperCase());
            } catch (IllegalArgumentException ignored) {}

            int unlockLevel = section.getInt("unlock_level", section.getInt("exploration_level", 1));
            java.util.List<String> allowedNodes = section.getStringList("allowed_nodes");
            java.util.List<String> zones = section.getStringList("zones");

            ResourceType resourceType = ResourceType.RAW_RESOURCE;
            try {
                resourceType = ResourceType.valueOf(section.getString("resource_type", "RAW_RESOURCE").toUpperCase());
            } catch (IllegalArgumentException ignored) {}

            double sellPrice = section.getDouble("sell_price", 0.0);
            double buyPrice = section.getDouble("buy_price", 0.0);
            int exp = section.getInt("exp", 0);
            
            int dropWeight = section.getInt("drop_weight", section.getInt("weight", 100));
            int maxStack = section.getInt("max_stack", 64);
            boolean npcSell = section.getBoolean("npc_sell", true);
            boolean playerTrade = section.getBoolean("player_trade", true);

            StackType stackType = StackType.NORMAL;
            try {
                stackType = StackType.valueOf(section.getString("stack_type", "NORMAL").toUpperCase());
            } catch (IllegalArgumentException ignored) {}

            java.util.List<String> tags = section.getStringList("tags");
            String translationKey = section.getString("translation_key", "resource." + key);
            boolean enabled = section.getBoolean("enabled", true);
            int introducedIn = section.getInt("introduced_in", 1);

            ResourceDefinition def = new ResourceDefinition(
                key, displayName, material, customModelData, rarity, quality,
                category, family, tier, unlockLevel, allowedNodes, zones,
                resourceType, sellPrice, buyPrice, exp, dropWeight, maxStack,
                npcSell, playerTrade, stackType, tags, translationKey, enabled, introducedIn
            );
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
