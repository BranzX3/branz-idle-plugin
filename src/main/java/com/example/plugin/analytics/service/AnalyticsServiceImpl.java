package com.example.plugin.analytics.service;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

public class AnalyticsServiceImpl implements AnalyticsService {

    private final Map<String, Long> productionStats = new ConcurrentHashMap<>();
    private final DoubleAdder coinsGained = new DoubleAdder();
    private final DoubleAdder coinsSpent = new DoubleAdder();
    private final AtomicLong diamondsGained = new AtomicLong(0);
    private final AtomicLong diamondsSpent = new AtomicLong(0);
    private final AtomicLong fusionCount = new AtomicLong(0);
    private final AtomicLong gachaPullCount = new AtomicLong(0);

    @Override
    public void trackProduction(String resourceKey, long amount) {
        if (amount <= 0) return;
        productionStats.merge(resourceKey, amount, Long::sum);
    }

    @Override
    public void trackCoinTransaction(double amount, boolean isGain) {
        if (amount <= 0) return;
        if (isGain) {
            coinsGained.add(amount);
        } else {
            coinsSpent.add(amount);
        }
    }

    @Override
    public void trackDiamondTransaction(long amount, boolean isGain) {
        if (amount <= 0) return;
        if (isGain) {
            diamondsGained.addAndGet(amount);
        } else {
            diamondsSpent.addAndGet(amount);
        }
    }

    @Override
    public void trackFusion() {
        fusionCount.incrementAndGet();
    }

    @Override
    public void trackGachaPull() {
        gachaPullCount.incrementAndGet();
    }

    @Override
    public long getProductionCount(String resourceKey) {
        return productionStats.getOrDefault(resourceKey, 0L);
    }

    @Override
    public Map<String, Long> getProductionStats() {
        return Collections.unmodifiableMap(productionStats);
    }

    @Override
    public double getCoinsGained() {
        return coinsGained.sum();
    }

    @Override
    public double getCoinsSpent() {
        return coinsSpent.sum();
    }

    @Override
    public long getDiamondsGained() {
        return diamondsGained.get();
    }

    @Override
    public long getDiamondsSpent() {
        return diamondsSpent.get();
    }

    @Override
    public long getFusionCount() {
        return fusionCount.get();
    }

    @Override
    public long getGachaPullCount() {
        return gachaPullCount.get();
    }
}
