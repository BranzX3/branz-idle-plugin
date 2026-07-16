package com.example.plugin.onboarding.service;

import com.example.plugin.economy.model.PlayerProfile;
import com.example.plugin.economy.service.EconomyService;
import com.example.plugin.node.model.ProductionNode;
import com.example.plugin.node.service.NodeService;
import com.example.plugin.onboarding.StarterPack;
import com.example.plugin.onboarding.event.PlayerOnboardingCompletedEvent;
import com.example.plugin.territory.model.ChunkClaim;
import com.example.plugin.territory.model.ChunkType;
import com.example.plugin.territory.service.TerritoryService;
import com.example.plugin.worker.model.WorkerInstance;
import com.example.plugin.worker.service.WorkerService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of OnboardingService coordinating economy rewards, 2x2 territory initialization,
 * and full base reset functionality.
 */
public class OnboardingServiceImpl implements OnboardingService {

    private final JavaPlugin plugin;
    private final EconomyService economyService;
    private final TerritoryService territoryService;
    private final NodeService nodeService;
    private final WorkerService workerService;
    private final StarterPack starterPack;

    public OnboardingServiceImpl(
        JavaPlugin plugin,
        EconomyService economyService,
        TerritoryService territoryService,
        NodeService nodeService,
        WorkerService workerService
    ) {
        this.plugin = plugin;
        this.economyService = economyService;
        this.territoryService = territoryService;
        this.nodeService = nodeService;
        this.workerService = workerService;

        double coins = plugin.getConfig().getDouble("onboarding.starter_coins", 1000.0);
        int chunks = plugin.getConfig().getInt("onboarding.free_chunks", 4);
        List<String> workers = plugin.getConfig().getStringList("onboarding.starter_worker_templates");
        if (workers.isEmpty()) {
            workers = List.of("miner_common_1", "lumberjack_common_1", "fisher_common_1");
        }
        this.starterPack = new StarterPack(chunks, coins, workers);
    }

    @Override
    public StarterPack getStarterPack() {
        return starterPack;
    }

    @Override
    public boolean isPlayerOnboarded(Player player) {
        Optional<PlayerProfile> profile = economyService.getProfile(player.getUniqueId());
        return profile.map(PlayerProfile::isOnboardingCompleted).orElse(false);
    }

    @Override
    public boolean startOnboarding(Player player, int originChunkX, int originChunkZ) {
        if (isPlayerOnboarded(player)) {
            player.sendMessage("§cYou have already completed the Branz.Idle onboarding!");
            return false;
        }

        if (!territoryService.isDedicatedWorld(player.getWorld())) {
            player.sendMessage("§cYou must be in the dedicated Idle world to initialize your base!");
            return false;
        }

        if (territoryService.getClaim(originChunkX, originChunkZ).isPresent()) {
            player.sendMessage("§cThe target chunk at (" + originChunkX + ", " + originChunkZ + ") is already claimed. Please choose an open area!");
            return false;
        }

        UUID playerId = player.getUniqueId();

        // Claim only the 1 Residential Hub chunk
        territoryService.claimChunkInternal(playerId, originChunkX, originChunkZ, ChunkType.RESIDENTIAL);

        // Grant initial currency balance
        economyService.addCoins(playerId, starterPack.getStarterCoins());
        economyService.setOnboardingCompleted(playerId, true);

        // Dispatch completion event so worker instances can be assigned
        Bukkit.getScheduler().runTask(plugin, () ->
            Bukkit.getPluginManager().callEvent(new PlayerOnboardingCompletedEvent(player, 1, starterPack.getStarterWorkerTemplates()))
        );

        player.sendMessage("§6==========================================");
        player.sendMessage("§e§lWelcome to Branz.Idle MMORPG!");
        player.sendMessage("§aYour starting base territory has been secured!");
        player.sendMessage("§f+ §e" + starterPack.getStarterCoins() + " Coins granted to your profile.");
        player.sendMessage("§f+ §b1 Territory Chunk claimed at (" + originChunkX + ", " + originChunkZ + ") as a Residential base.");
        player.sendMessage("§6==========================================");
        return true;
    }

    @Override
    public boolean resetBase(Player player) {
        UUID playerId = player.getUniqueId();

        // 1. Unassign all workers owned by this player (keep workers, just detach from nodes)
        List<WorkerInstance> playerWorkers = workerService.getPlayerWorkers(playerId);
        for (WorkerInstance worker : playerWorkers) {
            if (worker.getAssignedNodeId() != null) {
                worker.setAssignedNodeId(null);
                workerService.updateWorker(worker);
            }
        }

        // 2. Delete all production nodes owned by this player
        List<ProductionNode> playerNodes = nodeService.getPlayerNodes(playerId);
        for (ProductionNode node : playerNodes) {
            nodeService.deleteNode(node.getNodeId());
        }

        // 3. Unclaim all territory chunks owned by this player (including residential and production)
        List<ChunkClaim> playerClaims = territoryService.getPlayerClaims(playerId);
        for (ChunkClaim claim : playerClaims) {
            territoryService.unclaimChunkInternal(claim.getChunkX(), claim.getChunkZ());
        }

        // 4. Reset onboarding flag so player can re-onboard
        economyService.setOnboardingCompleted(playerId, false);

        player.sendMessage("§6==========================================");
        player.sendMessage("§c§lBase Reset Complete!");
        player.sendMessage("§7All territory, nodes, and worker assignments have been cleared.");
        player.sendMessage("§7Your workers and currency balance are preserved.");
        player.sendMessage("§eUse §6/idle menu §eto set up a new base!");
        player.sendMessage("§6==========================================");
        return true;
    }
}

