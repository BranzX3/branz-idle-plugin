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
 * Registry responsible for loading and caching exploration drop tables from drop_tables.yml.
 */
public class DropTableRegistry implements ConfigRegistry {

    public record DropEntry(
        String resourceKey,
        long minQty,
        long maxQty,
        double weight
    ) {}

    public record DropTableDefinition(
        String tableKey,
        int minExplorationLevel,
        List<DropEntry> entries
    ) {}

    private final Map<String, DropTableDefinition> dropTables = new HashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public void load(YamlConfiguration config) {
        clear();
        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) continue;

            int minLevel = section.getInt("min_exploration_level", 1);
            List<DropEntry> entries = new ArrayList<>();

            List<Map<?, ?>> rawList = section.getMapList("entries");
            for (Map<?, ?> rawMap : rawList) {
                Object resKeyVal = rawMap.get("resource_key");
                String resKey = resKeyVal != null ? resKeyVal.toString() : "iron_ore";

                Object minQtyVal = rawMap.get("min_qty");
                long minQty = minQtyVal instanceof Number ? ((Number) minQtyVal).longValue() : 1L;

                Object maxQtyVal = rawMap.get("max_qty");
                long maxQty = maxQtyVal instanceof Number ? ((Number) maxQtyVal).longValue() : 1L;

                Object weightVal = rawMap.get("weight");
                double weight = weightVal instanceof Number ? ((Number) weightVal).doubleValue() : 100.0;

                entries.add(new DropEntry(resKey, minQty, maxQty, weight));
            }

            DropTableDefinition def = new DropTableDefinition(key, minLevel, Collections.unmodifiableList(entries));
            dropTables.put(key, def);
        }
    }

    @Override
    public void clear() {
        dropTables.clear();
    }

    public Optional<DropTableDefinition> getDropTable(String tableKey) {
        return Optional.ofNullable(dropTables.get(tableKey));
    }

    public Map<String, DropTableDefinition> getAllDropTables() {
        return Collections.unmodifiableMap(dropTables);
    }
}
