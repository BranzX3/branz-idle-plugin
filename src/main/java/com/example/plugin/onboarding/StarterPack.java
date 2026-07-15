package com.example.plugin.onboarding;

import java.util.Collections;
import java.util.List;

/**
 * Data object defining initial rewards and territory grants during onboarding.
 */
public class StarterPack {

    private final int freeChunksGranted;
    private final double starterCoins;
    private final List<String> starterWorkerTemplates;

    public StarterPack(int freeChunksGranted, double starterCoins, List<String> starterWorkerTemplates) {
        this.freeChunksGranted = freeChunksGranted;
        this.starterCoins = starterCoins;
        this.starterWorkerTemplates = starterWorkerTemplates != null ? starterWorkerTemplates : Collections.emptyList();
    }

    public int getFreeChunksGranted() {
        return freeChunksGranted;
    }

    public double getStarterCoins() {
        return starterCoins;
    }

    public List<String> getStarterWorkerTemplates() {
        return Collections.unmodifiableList(starterWorkerTemplates);
    }
}
