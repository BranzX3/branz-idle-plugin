package com.example.plugin.economy.service;

import com.example.plugin.economy.model.PlayerProfile;

import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for player profile and currency operations.
 */
public interface EconomyService {

    /**
     * Retrieves a cached or persisted player profile.
     *
     * @param playerId Player UUID
     * @return Optional containing profile if found
     */
    Optional<PlayerProfile> getProfile(UUID playerId);

    /**
     * Retrieves or creates a new player profile if it does not exist.
     *
     * @param playerId Player UUID
     * @param username Player username
     * @return active PlayerProfile
     */
    PlayerProfile getOrCreateProfile(UUID playerId, String username);

    /**
     * Adds coins to a player's profile and queues an async save.
     */
    void addCoins(UUID playerId, double amount);

    /**
     * Attempts to remove coins from a player's profile.
     *
     * @return true if successful, false if insufficient funds
     */
    boolean removeCoins(UUID playerId, double amount);

    /**
     * Adds diamonds to a player's profile and queues an async save.
     */
    void addDiamonds(UUID playerId, long amount);

    /**
     * Attempts to remove diamonds from a player's profile.
     *
     * @return true if successful, false if insufficient funds
     */
    boolean removeDiamonds(UUID playerId, long amount);

    /**
     * Updates player's onboarding completion status.
     */
    void setOnboardingCompleted(UUID playerId, boolean completed);

    /**
     * Unloads a player profile from cache when they disconnect.
     */
    void unloadProfile(UUID playerId);

    /**
     * Flushes all cached profiles to database on shutdown.
     */
    void flushAll();
}
