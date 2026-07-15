package com.example.plugin.visual.service;

import com.example.plugin.bootstrap.ProviderManager;
import com.example.plugin.visual.provider.CitizensVisualProvider;
import com.example.plugin.visual.provider.NoOpVisualProvider;
import com.example.plugin.visual.provider.VisualProvider;
import com.example.plugin.worker.model.WorkerInstance;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Implementation of VisualService dynamically adopting Citizens API if detected or falling back safely.
 */
public class VisualServiceImpl implements VisualService {

    private final JavaPlugin plugin;
    private final ProviderManager providerManager;
    private VisualProvider visualProvider;

    public VisualServiceImpl(JavaPlugin plugin, ProviderManager providerManager) {
        this.plugin = plugin;
        this.providerManager = providerManager;
    }

    @Override
    public void initialize() {
        if (providerManager.hasCitizens()) {
            this.visualProvider = new CitizensVisualProvider(plugin);
            plugin.getLogger().info("[VisualService] Using Citizens NPC visual provider for worker rendering.");
        } else {
            this.visualProvider = new NoOpVisualProvider();
            plugin.getLogger().info("[VisualService] Citizens not found; running with NoOp visual provider.");
        }
    }

    @Override
    public void showWorker(WorkerInstance worker, Location location) {
        if (visualProvider != null && worker != null && location != null) {
            visualProvider.spawnWorkerNPC(worker.getWorkerId(), worker.getTemplateId(), location);
        }
    }

    @Override
    public void hideWorker(UUID workerId) {
        if (visualProvider != null && workerId != null) {
            visualProvider.despawnWorkerNPC(workerId);
        }
    }

    @Override
    public void clearAllVisuals() {
        if (visualProvider != null) {
            visualProvider.despawnAll();
        }
    }
}
