package com.example.plugin.visual.service;

import com.example.plugin.visual.model.WorkshopBlueprint;
import com.example.plugin.visual.model.WorkshopBlueprint.MarkerDefinition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service managing WorkshopBlueprint configurations, caching, disk saving, and legacy format migration.
 */
public class WorkshopBlueprintManager {

    private final JavaPlugin plugin;
    private final File schematicsFolder;
    private final Map<String, WorkshopBlueprint> blueprintCache = new ConcurrentHashMap<>();

    public WorkshopBlueprintManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.schematicsFolder = new File(plugin.getDataFolder(), "schematics");
        if (!schematicsFolder.exists()) {
            schematicsFolder.mkdirs();
        }
    }

    public WorkshopBlueprint getBlueprint(String schematicName) {
        if (schematicName == null) {
            WorkshopBlueprint fallback = new WorkshopBlueprint();
            generateDefaultBlueprint(fallback);
            return fallback;
        }
        String baseName = schematicName;
        if (baseName.endsWith(".schem")) {
            baseName = baseName.substring(0, baseName.length() - 6);
        } else if (baseName.endsWith(".schematic")) {
            baseName = baseName.substring(0, baseName.length() - 10);
        }

        return blueprintCache.computeIfAbsent(baseName, this::loadBlueprintFromFile);
    }

    private WorkshopBlueprint loadBlueprintFromFile(String baseName) {
        File workshopFile = new File(schematicsFolder, baseName + ".workshop.yml");
        File legacyFile = new File(schematicsFolder, baseName + ".visual.yml");
        WorkshopBlueprint blueprint = new WorkshopBlueprint();

        if (!workshopFile.exists() && legacyFile.exists()) {
            plugin.getLogger().info("[WorkshopBlueprint] Found legacy visual YML for " + baseName + ". Migrating to workshop.yml...");
            migrateLegacyFile(legacyFile, blueprint);
            saveBlueprint(baseName, blueprint);
            legacyFile.delete();
            return blueprint;
        }

        if (!workshopFile.exists()) {
            generateDefaultBlueprint(blueprint);
            saveBlueprint(baseName, blueprint);
            return blueprint;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(workshopFile);
        int version = config.getInt("version", 2);
        blueprint.setVersion(version);

        ConfigurationSection metaSec = config.getConfigurationSection("metadata");
        if (metaSec != null) {
            blueprint.getMetadata().setName(metaSec.getString("name", ""));
            blueprint.getMetadata().setAuthor(metaSec.getString("author", ""));
            blueprint.getMetadata().setCreatedAt(metaSec.getLong("createdAt", System.currentTimeMillis()));
        }

        ConfigurationSection markersSection = config.getConfigurationSection("markers");
        if (markersSection != null) {
            for (String key : markersSection.getKeys(false)) {
                MarkerDefinition mDef = new MarkerDefinition();
                mDef.setType(markersSection.getString(key + ".type", "WORK"));
                mDef.setX(markersSection.getDouble(key + ".x"));
                mDef.setY(markersSection.getDouble(key + ".y"));
                mDef.setZ(markersSection.getDouble(key + ".z"));
                mDef.setRadius(markersSection.getDouble(key + ".radius", 0.0));
                mDef.setWeight(markersSection.getDouble(key + ".weight", 50.0));
                mDef.setActions(markersSection.getStringList(key + ".actions"));
                mDef.setConditionTime(markersSection.getString(key + ".conditionTime", "ANY"));
                mDef.setConditionWeather(markersSection.getString(key + ".conditionWeather", "ANY"));

                ConfigurationSection overridesSec = markersSection.getConfigurationSection(key + ".professionOverrides");
                if (overridesSec != null) {
                    Map<String, List<String>> overrides = new HashMap<>();
                    for (String prof : overridesSec.getKeys(false)) {
                        overrides.put(prof.toUpperCase(), overridesSec.getStringList(prof));
                    }
                    mDef.setProfessionOverrides(overrides);
                }

                blueprint.getMarkers().put(key, mDef);
            }
        }

        ConfigurationSection pathsSec = config.getConfigurationSection("paths");
        if (pathsSec != null) {
            for (String pathName : pathsSec.getKeys(false)) {
                blueprint.getPaths().put(pathName, pathsSec.getStringList(pathName));
            }
        }

        if (blueprint.getMarkers().isEmpty()) {
            generateDefaultBlueprint(blueprint);
            saveBlueprint(baseName, blueprint);
        }

        return blueprint;
    }

    private void migrateLegacyFile(File legacyFile, WorkshopBlueprint blueprint) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(legacyFile);
        blueprint.setVersion(2);
        blueprint.getMetadata().setName(legacyFile.getName());
        blueprint.getMetadata().setAuthor("System Migration");

        int markerIdx = 1;

        ConfigurationSection markersSection = config.getConfigurationSection("markers");
        if (markersSection != null) {
            for (String key : markersSection.getKeys(false)) {
                MarkerDefinition mDef = new MarkerDefinition();
                mDef.setX(markersSection.getDouble(key + ".x"));
                mDef.setY(markersSection.getDouble(key + ".y"));
                mDef.setZ(markersSection.getDouble(key + ".z"));
                mDef.setWeight(50.0);
                mDef.setRadius(0.0);

                if (key.toLowerCase().startsWith("rest")) {
                    mDef.setType("REST");
                    mDef.getActions().add("SIT");
                } else {
                    mDef.setType("WORK");
                    mDef.getActions().add("SWING");
                }
                blueprint.getMarkers().put(key, mDef);
            }
        } else {
            List<?> restList = config.getList("rest_points");
            if (restList != null) {
                for (Object obj : restList) {
                    if (obj instanceof Map<?, ?> map) {
                        double x = getDouble(map.get("x"));
                        double y = getDouble(map.get("y"));
                        double z = getDouble(map.get("z"));
                        String mName = "rest_" + markerIdx++;
                        MarkerDefinition mDef = new MarkerDefinition(x, y, z);
                        mDef.setType("REST");
                        mDef.getActions().add("SIT");
                        blueprint.getMarkers().put(mName, mDef);
                    }
                }
            }
            List<?> workList = config.getList("work_points");
            if (workList != null) {
                for (Object obj : workList) {
                    if (obj instanceof Map<?, ?> map) {
                        double x = getDouble(map.get("x"));
                        double y = getDouble(map.get("y"));
                        double z = getDouble(map.get("z"));
                        String mName = "work_" + markerIdx++;
                        MarkerDefinition mDef = new MarkerDefinition(x, y, z);
                        mDef.setType("WORK");
                        mDef.getActions().add("SWING");
                        blueprint.getMarkers().put(mName, mDef);
                    }
                }
            }
        }
    }

    private double getDouble(Object obj) {
        if (obj instanceof Number num) return num.doubleValue();
        if (obj instanceof String str) {
            try { return Double.parseDouble(str); } catch (NumberFormatException ignored) {}
        }
        return 0.0;
    }

    private void generateDefaultBlueprint(WorkshopBlueprint blueprint) {
        MarkerDefinition rest = new MarkerDefinition(0.5, 1.0, 0.5);
        rest.setType("REST");
        rest.getActions().add("SIT");
        blueprint.getMarkers().put("rest_default", rest);

        MarkerDefinition work = new MarkerDefinition(4.5, 1.0, 4.5);
        work.setType("WORK");
        work.getActions().add("SWING");
        blueprint.getMarkers().put("work_default", work);
    }

    public void saveBlueprint(String schematicName, WorkshopBlueprint blueprint) {
        String baseName = schematicName;
        if (baseName.endsWith(".schem")) {
            baseName = baseName.substring(0, baseName.length() - 6);
        } else if (baseName.endsWith(".schematic")) {
            baseName = baseName.substring(0, baseName.length() - 10);
        }

        File file = new File(schematicsFolder, baseName + ".workshop.yml");
        YamlConfiguration config = new YamlConfiguration();

        config.set("version", blueprint.getVersion());

        config.set("metadata.name", blueprint.getMetadata().getName());
        config.set("metadata.author", blueprint.getMetadata().getAuthor());
        config.set("metadata.createdAt", blueprint.getMetadata().getCreatedAt());

        for (Map.Entry<String, MarkerDefinition> entry : blueprint.getMarkers().entrySet()) {
            String path = "markers." + entry.getKey();
            MarkerDefinition m = entry.getValue();
            config.set(path + ".type", m.getType());
            config.set(path + ".x", m.getX());
            config.set(path + ".y", m.getY());
            config.set(path + ".z", m.getZ());
            config.set(path + ".radius", m.getRadius());
            config.set(path + ".weight", m.getWeight());
            config.set(path + ".actions", m.getActions());
            config.set(path + ".conditionTime", m.getConditionTime());
            config.set(path + ".conditionWeather", m.getConditionWeather());

            for (Map.Entry<String, List<String>> override : m.getProfessionOverrides().entrySet()) {
                config.set(path + ".professionOverrides." + override.getKey(), override.getValue());
            }
        }

        for (Map.Entry<String, List<String>> entry : blueprint.getPaths().entrySet()) {
            config.set("paths." + entry.getKey(), entry.getValue());
        }

        try {
            config.save(file);
            blueprintCache.put(baseName, blueprint);
        } catch (IOException e) {
            plugin.getLogger().severe("[WorkshopBlueprint] Failed to save blueprint for " + baseName + ": " + e.getMessage());
        }
    }

    public void clearCache() {
        blueprintCache.clear();
    }
}
