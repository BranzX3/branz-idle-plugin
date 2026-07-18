package com.example.plugin.visual.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Domain model representing a schematic's Workshop Blueprint (.workshop.yml).
 * Governs marker waypoints, paths, occupancy rules, and behavior actions.
 */
public class WorkshopBlueprint {

    public static class MarkerDefinition {
        private String type = "WORK"; // WORK, REST, STORAGE, QUEUE
        private double x;
        private double y;
        private double z;
        private double radius = 0.0;
        private double weight = 50.0;
        private List<String> actions = new ArrayList<>();
        private Map<String, List<String>> professionOverrides = new HashMap<>();
        private String conditionTime = "ANY"; // DAY, NIGHT, ANY
        private String conditionWeather = "ANY"; // SUN, RAIN, ANY

        public MarkerDefinition() {}

        public MarkerDefinition(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public double getX() { return x; }
        public void setX(double x) { this.x = x; }

        public double getY() { return y; }
        public void setY(double y) { this.y = y; }

        public double getZ() { return z; }
        public void setZ(double z) { this.z = z; }

        public double getRadius() { return radius; }
        public void setRadius(double radius) { this.radius = radius; }

        public double getWeight() { return weight; }
        public void setWeight(double weight) { this.weight = weight; }

        public List<String> getActions() { return actions; }
        public void setActions(List<String> actions) { this.actions = actions; }

        public Map<String, List<String>> getProfessionOverrides() { return professionOverrides; }
        public void setProfessionOverrides(Map<String, List<String>> professionOverrides) { this.professionOverrides = professionOverrides; }

        public String getConditionTime() { return conditionTime; }
        public void setConditionTime(String conditionTime) { this.conditionTime = conditionTime; }

        public String getConditionWeather() { return conditionWeather; }
        public void setConditionWeather(String conditionWeather) { this.conditionWeather = conditionWeather; }
    }

    public static class Metadata {
        private String name = "";
        private String author = "";
        private long createdAt = System.currentTimeMillis();

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }

        public long getCreatedAt() { return createdAt; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    }

    private int version = 2;
    private Map<String, MarkerDefinition> markers = new ConcurrentHashMap<>();
    private Map<String, List<String>> paths = new ConcurrentHashMap<>();
    private Metadata metadata = new Metadata();

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public Map<String, MarkerDefinition> getMarkers() { return markers; }
    public void setMarkers(Map<String, MarkerDefinition> markers) { this.markers = markers; }

    public Map<String, List<String>> getPaths() { return paths; }
    public void setPaths(Map<String, List<String>> paths) { this.paths = paths; }

    public Metadata getMetadata() { return metadata; }
    public void setMetadata(Metadata metadata) { this.metadata = metadata; }
}
