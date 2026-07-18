package com.example.plugin.analytics.service;

import java.util.Map;

public interface AnalyticsService {
    void trackProduction(String resourceKey, long amount);
    void trackCoinTransaction(double amount, boolean isGain);
    void trackDiamondTransaction(long amount, boolean isGain);
    void trackFusion();
    void trackGachaPull();

    long getProductionCount(String resourceKey);
    Map<String, Long> getProductionStats();
    double getCoinsGained();
    double getCoinsSpent();
    long getDiamondsGained();
    long getDiamondsSpent();
    long getFusionCount();
    long getGachaPullCount();
}
