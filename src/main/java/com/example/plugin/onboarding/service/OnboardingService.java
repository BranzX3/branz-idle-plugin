package com.example.plugin.onboarding.service;

import com.example.plugin.onboarding.StarterPack;
import org.bukkit.entity.Player;

/**
 * Service interface governing new player onboarding, initial 2x2 territory claims, and starter pack rewards.
 */
public interface OnboardingService {

    /**
     * Retrieves the configured starter pack defaults.
     */
    StarterPack getStarterPack();

    /**
     * Checks if the player has already completed their onboarding flow.
     */
    boolean isPlayerOnboarded(Player player);

    /**
     * Initiates the onboarding sequence at the given origin chunk coordinate.
     * Claims 4 chunks (1 Residential + 3 Production) and distributes starter rewards.
     *
     * @return true if onboarding completed successfully, false if area occupied or already onboarded
     */
    boolean startOnboarding(Player player, int originChunkX, int originChunkZ);
}
