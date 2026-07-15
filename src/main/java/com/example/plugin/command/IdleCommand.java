package com.example.plugin.command;

import com.example.plugin.config.RegistryManager;
import com.example.plugin.economy.service.EconomyService;
import com.example.plugin.gui.AdminHubGUI;
import com.example.plugin.gui.GachaPullGUI;
import com.example.plugin.gui.MainHubGUI;
import com.example.plugin.gui.MarketplaceGUI;
import com.example.plugin.gui.NodeSelectionGUI;
import com.example.plugin.gui.TerritoryMapGUI;
import com.example.plugin.node.model.NodeType;
import com.example.plugin.node.model.ProductionNode;
import com.example.plugin.node.service.NodeService;
import com.example.plugin.onboarding.service.OnboardingService;
import com.example.plugin.storage.service.StorageService;
import com.example.plugin.territory.service.TerritoryService;
import com.example.plugin.visual.service.VisualService;
import com.example.plugin.worker.service.WorkerService;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Main /idle command handler routing subcommands to GUIs and domain services.
 */
public class IdleCommand extends BaseCommand {

    private final JavaPlugin plugin;
    private final TerritoryService territoryService;
    private final NodeService nodeService;
    private final WorkerService workerService;
    private final StorageService storageService;
    private final EconomyService economyService;
    private final OnboardingService onboardingService;
    private final RegistryManager registryManager;
    private final VisualService visualService;

    public IdleCommand(
        JavaPlugin plugin,
        TerritoryService territoryService,
        NodeService nodeService,
        WorkerService workerService,
        StorageService storageService,
        EconomyService economyService,
        OnboardingService onboardingService,
        RegistryManager registryManager,
        VisualService visualService
    ) {
        this.plugin = plugin;
        this.territoryService = territoryService;
        this.nodeService = nodeService;
        this.workerService = workerService;
        this.storageService = storageService;
        this.economyService = economyService;
        this.onboardingService = onboardingService;
        this.registryManager = registryManager;
        this.visualService = visualService;
    }

    @Override
    public boolean execute(Player player, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("menu")) {
            // Open Main Hub GUI as central entry point
            player.openInventory(new MainHubGUI(player, territoryService, nodeService, workerService, storageService, economyService, onboardingService, registryManager).getInventory());
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "claim" -> {
                NodeType type = NodeType.MINING;
                if (args.length >= 2) {
                    try {
                        type = NodeType.valueOf(args[1].toUpperCase());
                    } catch (IllegalArgumentException e) {
                        player.sendMessage("§cInvalid node type! Available: MINING, LUMBER, FISHING.");
                        return true;
                    }
                }
                Chunk chunk = player.getLocation().getChunk();
                boolean success = territoryService.claimChunk(player, chunk.getX(), chunk.getZ(), com.example.plugin.territory.model.ChunkType.PRODUCTION);
                if (success) {
                    nodeService.createNode(player.getUniqueId(), type, chunk.getX(), chunk.getZ());
                }
            }
            case "unclaim" -> {
                Chunk chunk = player.getLocation().getChunk();
                Optional<com.example.plugin.territory.model.ChunkClaim> claimOpt = territoryService.getClaim(chunk.getX(), chunk.getZ());
                if (claimOpt.isEmpty() || (!claimOpt.get().getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("branzidle.admin"))) {
                    player.sendMessage("§cYou do not own the territory at your current location!");
                    return true;
                }
                territoryService.unclaimChunk(player, chunk.getX(), chunk.getZ());
            }
            case "node" -> {
                // Open Node Selection GUI (Overview mode) — player picks which node to view
                player.openInventory(new NodeSelectionGUI(player, NodeSelectionGUI.Mode.OVERVIEW, territoryService, nodeService, workerService, storageService, economyService, onboardingService, registryManager).getInventory());
            }
            case "worker" -> {
                // Open Node Selection GUI (Worker mode) — player picks which node to manage workers for
                player.openInventory(new NodeSelectionGUI(player, NodeSelectionGUI.Mode.WORKER, territoryService, nodeService, workerService, storageService, economyService, onboardingService, registryManager).getInventory());
            }
            case "gacha" -> player.openInventory(new GachaPullGUI(player, workerService, economyService, registryManager, territoryService, nodeService, storageService, onboardingService).getInventory());
            case "map" -> {
                if (!territoryService.isDedicatedWorld(player.getWorld())) {
                    player.sendMessage("§cYou can only view the Territory Map in the dedicated Idle world!");
                    return true;
                }
                player.openInventory(new TerritoryMapGUI(player, territoryService, nodeService, workerService, storageService, economyService, onboardingService, registryManager).getInventory());
            }
            case "market" -> player.openInventory(new MarketplaceGUI(player, storageService, economyService, territoryService, nodeService, workerService, onboardingService, registryManager).getInventory());
            case "collect" -> {
                Chunk chunk = player.getLocation().getChunk();
                Optional<com.example.plugin.territory.model.ChunkClaim> claimOpt = territoryService.getClaim(chunk.getX(), chunk.getZ());
                Optional<ProductionNode> nodeOpt = Optional.empty();
                if (claimOpt.isPresent() && claimOpt.get().getNodeId() != null) {
                    nodeOpt = nodeService.getNode(claimOpt.get().getNodeId());
                }
                if (nodeOpt.isPresent()) {
                    storageService.collectFromNode(player, nodeOpt.get().getNodeId());
                } else {
                    player.sendMessage("§cStand inside one of your production nodes to collect accumulated yields!");
                }
            }
            case "admin" -> {
                if (!player.hasPermission("branzidle.admin")) {
                    player.sendMessage("§cYou do not have permission to access the admin panel!");
                    return true;
                }
                player.openInventory(new AdminHubGUI(player).getInventory());
            }
            case "reload" -> {
                if (!player.hasPermission("branzidle.admin")) {
                    player.sendMessage("§cYou do not have permission to reload Branz.Idle!");
                    return true;
                }
                player.sendMessage("§eReloading Branz.Idle configuration registries and visuals...");
                plugin.reloadConfig();
                registryManager.loadAll();
                visualService.clearAllVisuals();
                player.sendMessage("§aBranz.Idle registries and visuals reloaded successfully!");
            }
            default -> player.sendMessage("§cUnknown subcommand! Use §6/idle help §cfor a list of commands.");
        }
        return true;
    }

    @Override
    public List<String> tabComplete(Player player, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("help", "menu", "claim", "unclaim", "node", "worker", "gacha", "map", "market", "collect"));
            if (player.hasPermission("branzidle.admin")) {
                subs.add("reload");
                subs.add("admin");
            }
            String prefix = args[0].toLowerCase();
            return subs.stream().filter(s -> s.startsWith(prefix)).collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("claim")) {
            String prefix = args[1].toUpperCase();
            return Arrays.stream(NodeType.values()).map(Enum::name).filter(s -> s.startsWith(prefix)).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
