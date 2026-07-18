package com.example.plugin.visual.model;

import org.bukkit.Location;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores details about an active visual editor session for a Workshop Blueprint.
 */
public class VisualEditSession {

    private final String schematicName;
    private final Location originLocation;
    private final Map<String, WorkshopBlueprint.MarkerDefinition> markers = new ConcurrentHashMap<>();
    private final Map<String, List<String>> paths = new ConcurrentHashMap<>();

    public VisualEditSession(String schematicName, Location originLocation) {
        this.schematicName = schematicName;
        this.originLocation = originLocation;
    }

    public String getSchematicName() {
        return schematicName;
    }

    public Location getOriginLocation() {
        return originLocation;
    }

    public Map<String, WorkshopBlueprint.MarkerDefinition> getMarkers() {
        return markers;
    }

    public Map<String, List<String>> getPaths() {
        return paths;
    }
}
