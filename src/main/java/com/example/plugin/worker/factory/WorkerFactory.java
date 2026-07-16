package com.example.plugin.worker.factory;

import com.example.plugin.config.RegistryManager;
import com.example.plugin.config.WorkerRegistry;
import com.example.plugin.worker.biography.WorkerBiographyService;
import com.example.plugin.worker.model.WorkerInstance;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class WorkerFactory {

    private final JavaPlugin plugin;
    private final RegistryManager registryManager;
    private final WorkerBiographyService biographyService;

    public WorkerFactory(JavaPlugin plugin, RegistryManager registryManager, WorkerBiographyService biographyService) {
        this.plugin = plugin;
        this.registryManager = registryManager;
        this.biographyService = biographyService;
    }

    public WorkerInstance generateWorker(UUID ownerId, String templateId) {
        UUID workerId = UUID.randomUUID();
        ThreadLocalRandom rand = ThreadLocalRandom.current();

        // 1. Roll potentials
        double speedPotential = 0.9 + rand.nextDouble() * 0.3; // 0.9 to 1.2
        double yieldPotential = 0.8 + rand.nextDouble() * 0.5; // 0.8 to 1.3
        double rarePotential = 0.7 + rand.nextDouble() * 0.7;  // 0.7 to 1.4

        // 2. Pick random personality from config path
        String[] defaultPersonalities = {"CHEERFUL", "LAZY", "SERIOUS", "BRAVE"};
        String personality = defaultPersonalities[rand.nextInt(defaultPersonalities.length)];
        var personalitiesSec = plugin.getConfig().getConfigurationSection("worker_settings.personalities");
        if (personalitiesSec != null) {
            var keys = personalitiesSec.getKeys(false);
            if (!keys.isEmpty()) {
                personality = keys.toArray(new String[0])[rand.nextInt(keys.size())].toUpperCase();
            }
        }

        // 3. Set Born Location based on config or template profession
        Optional<WorkerRegistry.WorkerTemplate> tplOpt = registryManager.getWorkerRegistry().getTemplate(templateId);
        String bornLocation = "Guild";
        if (tplOpt.isPresent()) {
            WorkerRegistry.WorkerTemplate tpl = tplOpt.get();
            String profession = tpl.profession().toUpperCase();
            bornLocation = plugin.getConfig().getString("worker_settings.born_locations." + profession, "Guild");
            if ("Guild".equals(bornLocation)) {
                bornLocation = switch (profession) {
                    case "MINING" -> "Mining Guild";
                    case "LUMBER" -> "Lumbercamp Outpost";
                    case "FISHING" -> "Deepsea Harbor";
                    default -> "Guild";
                };
            }
        }

        // 4. Construct worker instance
        WorkerInstance worker = new WorkerInstance(
            workerId, ownerId, templateId, null, 1, 0L,
            0, speedPotential, yieldPotential, rarePotential,
            personality, bornLocation, 1, 0, 0L, null
        );

        // 5. Apply custom titles
        biographyService.updateWorkerTitle(worker);

        return worker;
    }
}
