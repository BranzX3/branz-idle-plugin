package com.example.plugin.production.task;

import com.example.plugin.production.service.ProductionService;

/**
 * Scheduled task executing periodic real-time production ticks across active nodes.
 */
public class ProductionTickerTask implements Runnable {

    private final ProductionService productionService;

    public ProductionTickerTask(ProductionService productionService) {
        this.productionService = productionService;
    }

    @Override
    public void run() {
        productionService.tickAllActiveNodes();
    }
}
