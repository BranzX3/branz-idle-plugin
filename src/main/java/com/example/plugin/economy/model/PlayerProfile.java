package com.example.plugin.economy.model;

import java.util.UUID;

/**
 * Domain model representing a player's overarching account and currency balance.
 */
public class PlayerProfile {

    private final UUID playerId;
    private String username;
    private double coins;
    private long diamonds;
    private boolean onboardingCompleted;
    private final long createdAt;
    private long updatedAt;
    
    // Decoupled node slots limit
    private int unlockedSlots;

    public PlayerProfile(
        UUID playerId,
        String username,
        double coins,
        long diamonds,
        boolean onboardingCompleted,
        long createdAt,
        long updatedAt
    ) {
        this.playerId = playerId;
        this.username = username;
        this.coins = coins;
        this.diamonds = diamonds;
        this.onboardingCompleted = onboardingCompleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.unlockedSlots = 4;
    }

    public PlayerProfile(
        UUID playerId,
        String username,
        double coins,
        long diamonds,
        boolean onboardingCompleted,
        long createdAt,
        long updatedAt,
        int unlockedSlots
    ) {
        this.playerId = playerId;
        this.username = username;
        this.coins = coins;
        this.diamonds = diamonds;
        this.onboardingCompleted = onboardingCompleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.unlockedSlots = unlockedSlots > 0 ? unlockedSlots : 4;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
        touch();
    }

    public double getCoins() {
        return coins;
    }

    public void setCoins(double coins) {
        this.coins = Math.max(0.0, coins);
        touch();
    }

    public void addCoins(double amount) {
        if (amount > 0) {
            this.coins += amount;
            touch();
        }
    }

    public boolean removeCoins(double amount) {
        if (amount > 0 && this.coins >= amount) {
            this.coins -= amount;
            touch();
            return true;
        }
        return false;
    }

    public long getDiamonds() {
        return diamonds;
    }

    public void setDiamonds(long diamonds) {
        this.diamonds = Math.max(0L, diamonds);
        touch();
    }

    public void addDiamonds(long amount) {
        if (amount > 0) {
            this.diamonds += amount;
            touch();
        }
    }

    public boolean removeDiamonds(long amount) {
        if (amount > 0 && this.diamonds >= amount) {
            this.diamonds -= amount;
            touch();
            return true;
        }
        return false;
    }

    public boolean isOnboardingCompleted() {
        return onboardingCompleted;
    }

    public void setOnboardingCompleted(boolean onboardingCompleted) {
        this.onboardingCompleted = onboardingCompleted;
        touch();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public int getUnlockedSlots() {
        return unlockedSlots;
    }

    public void setUnlockedSlots(int unlockedSlots) {
        this.unlockedSlots = unlockedSlots;
        touch();
    }

    private void touch() {
        this.updatedAt = System.currentTimeMillis();
    }
}
