package com.example.plugin.onboarding.service;

import com.example.plugin.economy.model.PlayerProfile;
import com.example.plugin.economy.service.EconomyService;
import com.example.plugin.onboarding.StarterPack;
import com.example.plugin.onboarding.event.PlayerOnboardingCompletedEvent;
import com.example.plugin.territory.model.ChunkType;
import com.example.plugin.territory.service.TerritoryService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of OnboardingService coordinating economy rewards and 2x2 territory initialization.
 */
public class OnboardingServiceImpl implements OnboardingService {

    private final JavaPlugin plugin;
    private final EconomyService economyService;
    private final TerritoryService territoryService;
    private final StarterPack starterPack;

    public OnboardingServiceImpl(JavaPlugin plugin, EconomyService economyService, TerritoryService territoryService) {
        this.plugin = plugin;
        this.economyService = economyService;
        this.territoryService = territoryService;

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

        if (!territoryService.is2x2AreaAvailable(originChunkX, originChunkZ)) {
            player.sendMessage("§cThe 2x2 chunk space starting at (" + originChunkX + ", " + originChunkZ + ") is obstructed by existing claims. Please choose an open area!");
            return false;
        }

        UUID playerId = player.getUniqueId();

        // Claim 4 contiguous chunks: 1 Residential Hub + 3 Production Plots
        territoryService.claimChunkInternal(playerId, originChunkX, originChunkZ, ChunkType.RESIDENTIAL);
        territoryService.claimChunkInternal(playerId, originChunkX + 1, originChunkZ, ChunkType.PRODUCTION);
        territoryService.claimChunkInternal(playerId, originChunkX, originChunkZ + 1, ChunkType.PRODUCTION);
        territoryService.claimChunkInternal(playerId, originChunkX + 1, originChunkZ + 1, ChunkType.PRODUCTION);

        // Grant initial currency balance
        economyService.addCoins(playerId, starterPack.getStarterCoins());
        economyService.setOnboardingCompleted(playerId, true);

        // Dispatch completion event so worker instances can be assigned
        Bukkit.getScheduler().runTask(plugin, () ->
            Bukkit.getPluginManager().callEvent(new PlayerOnboardingCompletedEvent(player, starterPack.getFreeChunksGranted(), starterPack.getStarterWorkerTemplates()))
        );

        player.sendMessage("§6==========================================");
        player.sendMessage("§e§lWelcome to Branz.Idle MMORPG!");
        player.sendMessage("§aYour starting 2x2 base territory has been secured!");
        player.sendMessage("§f+ §e" + starterPack.getStarterCoins() + " Coins granted to your profile.");
        player.sendMessage("§f+ §b4 Territory Chunks claimed at (" + originChunkX + ", " + originChunkZ + ").");
        player.sendMessage("§6==========================================");
        return true;
    }
}
