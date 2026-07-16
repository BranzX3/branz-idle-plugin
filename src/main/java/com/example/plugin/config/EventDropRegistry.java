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
 * Registry responsible for loading, managing, and caching event drop tables from event_drops.yml.
 */
public class EventDropRegistry implements ConfigRegistry {

    public record EventDropEntry(
        String resourceKey,
        double weight,
        int minQty,
        int maxQty
    ) {}

    public static class EventDefinition {
        private final String eventKey;
        private String displayName;
        private int minExplorationLevel;
        private int maxExplorationLevel;
        private double eventChance;
        private int durationCycles;
        private final List<EventDropEntry> entries;

        public EventDefinition(
            String eventKey,
            String displayName,
            int minExplorationLevel,
            int maxExplorationLevel,
            double eventChance,
            int durationCycles,
            List<EventDropEntry> entries
        ) {
            this.eventKey = eventKey;
            this.displayName = displayName;
            this.minExplorationLevel = minExplorationLevel;
            this.maxExplorationLevel = maxExplorationLevel;
            this.eventChance = eventChance;
            this.durationCycles = durationCycles;
            this.entries = entries;
        }

        public String eventKey() {
            return eventKey;
        }

        public String displayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public int minExplorationLevel() {
            return minExplorationLevel;
        }

        public void setMinExplorationLevel(int minExplorationLevel) {
            this.minExplorationLevel = minExplorationLevel;
        }

        public int maxExplorationLevel() {
            return maxExplorationLevel;
        }

        public void setMaxExplorationLevel(int maxExplorationLevel) {
            this.maxExplorationLevel = maxExplorationLevel;
        }

        public double eventChance() {
            return eventChance;
        }

        public void setEventChance(double eventChance) {
            this.eventChance = eventChance;
        }

        public int durationCycles() {
            return durationCycles;
        }

        public void setDurationCycles(int durationCycles) {
            this.durationCycles = durationCycles;
        }

        public List<EventDropEntry> entries() {
            return entries;
        }
    }

    public static class EventCategory {
        private final String nodeTypeKey;
        private final Map<String, EventDefinition> events = new HashMap<>();

        public EventCategory(String nodeTypeKey) {
            this.nodeTypeKey = nodeTypeKey;
        }

        public String nodeTypeKey() {
            return nodeTypeKey;
        }

        public Map<String, EventDefinition> events() {
            return events;
        }
    }

    private final Map<String, EventCategory> categories = new HashMap<>();

    @Override
    public void load(YamlConfiguration config) {
        clear();
        for (String nodeTypeKey : config.getKeys(false)) {
            ConfigurationSection typeSection = config.getConfigurationSection(nodeTypeKey);
            if (typeSection == null) continue;

            EventCategory category = new EventCategory(nodeTypeKey.toLowerCase());
            ConfigurationSection eventsSection = typeSection.getConfigurationSection("events");
            if (eventsSection != null) {
                for (String eventKey : eventsSection.getKeys(false)) {
                    ConfigurationSection eventSec = eventsSection.getConfigurationSection(eventKey);
                    if (eventSec == null) continue;

                    String displayName = eventSec.getString("display_name", eventKey);
                    int minLvl = eventSec.getInt("min_exploration_level", 1);
                    int maxLvl = eventSec.getInt("max_exploration_level", 100);
                    double chance = eventSec.getDouble("event_chance", 0.05);
                    int duration = eventSec.getInt("duration_cycles", 50);

                    List<EventDropEntry> entries = new ArrayList<>();
                    List<Map<?, ?>> rawList = eventSec.getMapList("entries");
                    for (Map<?, ?> rawMap : rawList) {
                        Object resKeyVal = rawMap.get("resource_key");
                        String resKey = resKeyVal != null ? resKeyVal.toString() : "diamond";

                        Object weightVal = rawMap.get("weight");
                        double weight = weightVal instanceof Number ? ((Number) weightVal).doubleValue() : 50.0;

                        Object minQtyVal = rawMap.get("min_qty");
                        int minQty = minQtyVal instanceof Number ? ((Number) minQtyVal).intValue() : 1;

                        Object maxQtyVal = rawMap.get("max_qty");
                        int maxQty = maxQtyVal instanceof Number ? ((Number) maxQtyVal).intValue() : 1;

                        entries.add(new EventDropEntry(resKey, weight, minQty, maxQty));
                    }

                    EventDefinition eventDef = new EventDefinition(
                        eventKey, displayName, minLvl, maxLvl, chance, duration, entries
                    );
                    category.events().put(eventKey, eventDef);
                }
            }
            categories.put(nodeTypeKey.toLowerCase(), category);
        }
    }

    @Override
    public void clear() {
        categories.clear();
    }

    public List<EventDropEntry> getEntries(String nodeTypeKey) {
        List<EventDropEntry> allEntries = new ArrayList<>();
        EventCategory category = categories.get(nodeTypeKey.toLowerCase());
        if (category != null) {
            for (EventDefinition def : category.events().values()) {
                allEntries.addAll(def.entries());
            }
        }
        return allEntries;
    }

    public List<EventDefinition> getEvents(String nodeTypeKey) {
        EventCategory category = categories.get(nodeTypeKey.toLowerCase());
        if (category == null) return Collections.emptyList();
        return new ArrayList<>(category.events().values());
    }

    public Optional<EventDefinition> getEvent(String nodeTypeKey, String eventKey) {
        EventCategory category = categories.get(nodeTypeKey.toLowerCase());
        if (category == null) return Optional.empty();
        return Optional.ofNullable(category.events().get(eventKey));
    }

    public Map<String, EventCategory> getAllCategories() {
        return Collections.unmodifiableMap(categories);
    }
}
