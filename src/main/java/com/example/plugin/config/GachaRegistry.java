package com.example.plugin.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registry responsible for loading and caching Gacha pools from gacha.yml.
 */
public class GachaRegistry implements ConfigRegistry {

    public record GachaRewardEntry(
        String workerTemplate,
        double weight
    ) {}

    public record GachaPoolDefinition(
        String poolId,
        String currencyType, // COINS or DIAMONDS
        double costPerPull,
        List<GachaRewardEntry> rewards
    ) {}

    private final Map<String, GachaPoolDefinition> pools = new HashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public void load(YamlConfiguration config) {
        clear();
        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) continue;

            String currencyType = section.getString("currency_type", "COINS").toUpperCase();
            double cost = section.getDouble("cost_per_pull", 1000.0);

            List<GachaRewardEntry> rewards = new ArrayList<>();
            List<Map<?, ?>> rawList = section.getMapList("rewards");
            for (Map<?, ?> rawMap : rawList) {
                Object templateVal = rawMap.get("worker_template");
                String template = templateVal != null ? templateVal.toString() : "miner_common_1";

                Object weightVal = rawMap.get("weight");
                double weight = weightVal instanceof Number ? ((Number) weightVal).doubleValue() : 10.0;

                rewards.add(new GachaRewardEntry(template, weight));
            }

            GachaPoolDefinition def = new GachaPoolDefinition(key, currencyType, cost, Collections.unmodifiableList(rewards));
            pools.put(key, def);
        }
    }

    @Override
    public void clear() {
        pools.clear();
    }

    public Optional<GachaPoolDefinition> getPool(String poolId) {
        return Optional.ofNullable(pools.get(poolId));
    }

    public Map<String, GachaPoolDefinition> getAllPools() {
        return Collections.unmodifiableMap(pools);
    }
}
