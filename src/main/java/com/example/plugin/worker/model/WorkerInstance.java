package com.example.plugin.worker.model;

import java.util.UUID;

/**
 * Domain entity representing an individual worker instance owned by a player.
 */
public class WorkerInstance {

    private final UUID workerId;
    private final UUID ownerId;
    private String templateId;
    private UUID assignedNodeId;
    private int level;
    private long experience;

    // Potential growth multipliers
    private double speedPotential;
    private double yieldPotential;
    private double rarePotential;

    // Aesthetic Biographies
    private int serialId;
    private String personality;
    private String bornLocation;
    private int generation;
    private int fusionCount;
    private long secondsWorked;
    private String customTitle;

    public WorkerInstance(
        UUID workerId,
        UUID ownerId,
        String templateId,
        UUID assignedNodeId,
        int level,
        long experience,
        int serialId,
        double speedPotential,
        double yieldPotential,
        double rarePotential,
        String personality,
        String bornLocation,
        int generation,
        int fusionCount,
        long secondsWorked,
        String customTitle
    ) {
        this.workerId = workerId;
        this.ownerId = ownerId;
        this.templateId = templateId;
        this.assignedNodeId = assignedNodeId;
        this.level = level;
        this.experience = experience;
        this.serialId = serialId;
        this.speedPotential = speedPotential;
        this.yieldPotential = yieldPotential;
        this.rarePotential = rarePotential;
        this.personality = personality;
        this.bornLocation = bornLocation;
        this.generation = generation;
        this.fusionCount = fusionCount;
        this.secondsWorked = secondsWorked;
        this.customTitle = customTitle;
    }

    public UUID getWorkerId() {
        return workerId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public UUID getAssignedNodeId() {
        return assignedNodeId;
    }

    public void setAssignedNodeId(UUID assignedNodeId) {
        this.assignedNodeId = assignedNodeId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public long getExperience() {
        return experience;
    }

    public void setExperience(long experience) {
        this.experience = experience;
    }

    public int getSerialId() {
        return serialId;
    }

    public void setSerialId(int serialId) {
        this.serialId = serialId;
    }

    public double getSpeedPotential() {
        return speedPotential;
    }

    public void setSpeedPotential(double speedPotential) {
        this.speedPotential = speedPotential;
    }

    public double getYieldPotential() {
        return yieldPotential;
    }

    public void setYieldPotential(double yieldPotential) {
        this.yieldPotential = yieldPotential;
    }

    public double getRarePotential() {
        return rarePotential;
    }

    public void setRarePotential(double rarePotential) {
        this.rarePotential = rarePotential;
    }

    public String getPersonality() {
        return personality;
    }

    public void setPersonality(String personality) {
        this.personality = personality;
    }

    public String getBornLocation() {
        return bornLocation;
    }

    public void setBornLocation(String bornLocation) {
        this.bornLocation = bornLocation;
    }

    public int getGeneration() {
        return generation;
    }

    public void setGeneration(int generation) {
        this.generation = generation;
    }

    public int getFusionCount() {
        return fusionCount;
    }

    public void setFusionCount(int fusionCount) {
        this.fusionCount = fusionCount;
    }

    public long getSecondsWorked() {
        return secondsWorked;
    }

    public void setSecondsWorked(long secondsWorked) {
        this.secondsWorked = secondsWorked;
    }

    public String getCustomTitle() {
        return customTitle;
    }

    public void setCustomTitle(String customTitle) {
        this.customTitle = customTitle;
    }

    public synchronized boolean addExperience(long amount) {
        if (amount <= 0) return false;
        this.experience += amount;
        boolean leveledUp = false;
        long required = (long) level * 500L;
        while (this.experience >= required) {
            this.experience -= required;
            this.level++;
            leveledUp = true;
            required = (long) level * 500L;
        }
        return leveledUp;
    }
}
