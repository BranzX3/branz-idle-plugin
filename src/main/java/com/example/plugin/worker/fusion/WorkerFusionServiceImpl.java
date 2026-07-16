package com.example.plugin.worker.fusion;

import com.example.plugin.config.RegistryManager;
import com.example.plugin.config.ResourceRegistry;
import com.example.plugin.config.WorkerRegistry;
import com.example.plugin.database.SaveQueueService;
import com.example.plugin.worker.biography.WorkerBiographyService;
import com.example.plugin.worker.model.WorkerInstance;
import com.example.plugin.worker.repository.WorkerRepository;
import com.example.plugin.worker.service.WorkerService;
import org.bukkit.entity.Player;

import java.util.Optional;

public class WorkerFusionServiceImpl implements WorkerFusionService {

    private final WorkerService workerService;
    private final RegistryManager registryManager;
    private final WorkerBiographyService biographyService;
    private final WorkerRepository repository;
    private final SaveQueueService saveQueue;

    public WorkerFusionServiceImpl(
        WorkerService workerService,
        RegistryManager registryManager,
        WorkerBiographyService biographyService,
        WorkerRepository repository,
        SaveQueueService saveQueue
    ) {
        this.workerService = workerService;
        this.registryManager = registryManager;
        this.biographyService = biographyService;
        this.repository = repository;
        this.saveQueue = saveQueue;
    }

    @Override
    public boolean fuseWorkers(Player player, WorkerInstance target, WorkerInstance w1, WorkerInstance w2) {
        Optional<WorkerRegistry.WorkerTemplate> targetTplOpt = registryManager.getWorkerRegistry().getTemplate(target.getTemplateId());
        Optional<WorkerRegistry.WorkerTemplate> w1TplOpt = registryManager.getWorkerRegistry().getTemplate(w1.getTemplateId());
        Optional<WorkerRegistry.WorkerTemplate> w2TplOpt = registryManager.getWorkerRegistry().getTemplate(w2.getTemplateId());

        if (targetTplOpt.isEmpty() || w1TplOpt.isEmpty() || w2TplOpt.isEmpty()) {
            player.sendMessage("§cWorker templates not found!");
            return false;
        }

        WorkerRegistry.WorkerTemplate targetTpl = targetTplOpt.get();
        WorkerRegistry.WorkerTemplate w1Tpl = w1TplOpt.get();
        WorkerRegistry.WorkerTemplate w2Tpl = w2TplOpt.get();

        // 1. Verify same Rarity
        if (targetTpl.rarity() != w1Tpl.rarity() || targetTpl.rarity() != w2Tpl.rarity()) {
            player.sendMessage("§cAll three workers must have the exact same Rarity tier to fuse!");
            return false;
        }

        // 2. Verify same Profession
        if (!targetTpl.profession().equalsIgnoreCase(w1Tpl.profession()) || !targetTpl.profession().equalsIgnoreCase(w2Tpl.profession())) {
            player.sendMessage("§cAll three workers must have the same Profession to fuse!");
            return false;
        }

        // 3. Find next rarity template
        Optional<WorkerRegistry.WorkerTemplate> nextTplOpt = getNextTierTemplate(targetTpl.profession(), targetTpl.rarity());
        if (nextTplOpt.isEmpty()) {
            player.sendMessage("§cThis worker has already reached the maximum Rarity tier (" + targetTpl.rarity() + ")!");
            return false;
        }
        WorkerRegistry.WorkerTemplate nextTpl = nextTplOpt.get();

        // 4. Genetic potential inheritance (Average potential, no RNG/mutation)
        double speedPotential = (target.getSpeedPotential() + w1.getSpeedPotential() + w2.getSpeedPotential()) / 3.0;
        double yieldPotential = (target.getYieldPotential() + w1.getYieldPotential() + w2.getYieldPotential()) / 3.0;
        double rarePotential = (target.getRarePotential() + w1.getRarePotential() + w2.getRarePotential()) / 3.0;

        // 5. Update target worker fields
        int nextGen = Math.max(target.getGeneration(), Math.max(w1.getGeneration(), w2.getGeneration())) + 1;
        int totalFusions = target.getFusionCount() + w1.getFusionCount() + w2.getFusionCount() + 1;
        long totalSecondsWorked = target.getSecondsWorked() + w1.getSecondsWorked() + w2.getSecondsWorked();

        // Unassign ingredients first
        if (w1.getAssignedNodeId() != null) {
            workerService.unassignWorker(player, w1.getWorkerId());
        }
        if (w2.getAssignedNodeId() != null) {
            workerService.unassignWorker(player, w2.getWorkerId());
        }

        // Delete ingredients from cache & database
        workerService.deleteWorker(w1.getWorkerId());
        workerService.deleteWorker(w2.getWorkerId());

        // Update target in-place
        target.setTemplateId(nextTpl.templateId());
        target.setSpeedPotential(speedPotential);
        target.setYieldPotential(yieldPotential);
        target.setRarePotential(rarePotential);
        target.setGeneration(nextGen);
        target.setFusionCount(totalFusions);
        target.setSecondsWorked(totalSecondsWorked);

        // Update title based on new potentials
        biographyService.updateWorkerTitle(target);

        // Save target updates
        workerService.updateWorker(target);

        player.sendMessage("§a§l[FUSION SUCCESS] §fYour worker has been upgraded to a §b" + nextTpl.displayName() + "§f!");
        player.sendMessage("§aInherited Potentials: §fSpeed: §e" + String.format("%.2f", speedPotential) + "§f, Yield: §e" + String.format("%.2f", yieldPotential) + "§f, Rare: §e" + String.format("%.2f", rarePotential));
        if (target.getCustomTitle() != null) {
            player.sendMessage("§dTitle Unlocked: §e" + target.getCustomTitle());
        }
        return true;
    }

    private Optional<WorkerRegistry.WorkerTemplate> getNextTierTemplate(String profession, ResourceRegistry.Rarity currentRarity) {
        ResourceRegistry.Rarity nextRarity = switch (currentRarity) {
            case COMMON -> ResourceRegistry.Rarity.UNCOMMON;
            case UNCOMMON -> ResourceRegistry.Rarity.RARE;
            case RARE -> ResourceRegistry.Rarity.EPIC;
            case EPIC -> ResourceRegistry.Rarity.LEGENDARY;
            case LEGENDARY -> ResourceRegistry.Rarity.MYTHIC;
            case MYTHIC -> null;
        };
        if (nextRarity == null) return Optional.empty();

        return registryManager.getWorkerRegistry().getAllTemplates().values().stream()
            .filter(t -> t.profession().equalsIgnoreCase(profession) && t.rarity() == nextRarity)
            .findFirst();
    }
}
