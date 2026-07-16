package com.example.plugin.onboarding.listener;

import com.example.plugin.node.model.NodeType;
import com.example.plugin.node.service.NodeService;
import com.example.plugin.onboarding.event.PlayerOnboardingCompletedEvent;
import com.example.plugin.worker.service.WorkerService;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

/**
 * Listener executing startup node and worker generation once onboarding finishes.
 */
public class OnboardingCompletedListener implements Listener {

    private final NodeService nodeService;
    private final WorkerService workerService;

    public OnboardingCompletedListener(NodeService nodeService, WorkerService workerService) {
        this.nodeService = nodeService;
        this.workerService = workerService;
    }

    @EventHandler
    public void onOnboardingComplete(PlayerOnboardingCompletedEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // 1. Spawn starting workers from config templates
        for (String templateId : event.getStarterWorkerTemplates()) {
            workerService.spawnWorker(playerId, templateId);
        }

        player.sendMessage("§a§lStarter Base established: You have claimed your first Residential plot!");
        player.sendMessage("§a§lStarter Workers hired: Manage them via §e/idle worker§a!");
    }
}
