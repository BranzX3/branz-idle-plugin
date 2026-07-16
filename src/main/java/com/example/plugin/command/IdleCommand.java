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
import com.example.plugin.worker.model.WorkerInstance;
import com.example.plugin.worker.service.WorkerService;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
                if (!onboardingService.isPlayerOnboarded(player)) {
                    player.sendMessage("§cYou must establish your base first by typing /idle to complete onboarding!");
                    return true;
                }
                boolean isResidential = false;
                NodeType type = null;
                if (args.length >= 2) {
                    String typeArg = args[1].toUpperCase();
                    if (typeArg.equals("RESIDENTIAL")) {
                        isResidential = true;
                    } else {
                        try {
                            type = NodeType.valueOf(typeArg);
                        } catch (IllegalArgumentException e) {
                            player.sendMessage("§cInvalid type! Available: MINING, LUMBER, FISHING, RESIDENTIAL.");
                            return true;
                        }
                    }
                } else {
                    type = NodeType.MINING;
                }
                Chunk chunk = player.getLocation().getChunk();
                if (isResidential) {
                    territoryService.claimChunk(player, chunk.getX(), chunk.getZ(), com.example.plugin.territory.model.ChunkType.RESIDENTIAL);
                } else {
                    boolean success = territoryService.claimChunk(player, chunk.getX(), chunk.getZ(), com.example.plugin.territory.model.ChunkType.PRODUCTION);
                    if (success && type != null) {
                        nodeService.createNode(player.getUniqueId(), type, chunk.getX(), chunk.getZ());
                    }
                }
            }
            case "unclaim" -> {
                Chunk chunk = player.getLocation().getChunk();
                Optional<com.example.plugin.territory.model.ChunkClaim> claimOpt = territoryService.getClaim(chunk.getX(), chunk.getZ());
                if (claimOpt.isEmpty() || (!claimOpt.get().getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("branzidle.admin"))) {
                    player.sendMessage("§cYou do not own the territory at your current location!");
                    return true;
                }
                com.example.plugin.territory.model.ChunkClaim claim = claimOpt.get();
                if (claim.getChunkType() == com.example.plugin.territory.model.ChunkType.RESIDENTIAL) {
                    economyService.setOnboardingCompleted(player.getUniqueId(), false);
                    player.sendMessage("§eYour Residential Base Hub has been unclaimed. Onboarding reset.");
                } else if (claim.getNodeId() != null) {
                    UUID nodeId = claim.getNodeId();
                    for (WorkerInstance w : workerService.getNodeWorkers(nodeId)) {
                        w.setAssignedNodeId(null);
                        workerService.updateWorker(w);
                    }
                    nodeService.deleteNode(nodeId);
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
            case "fuse" -> {
                if (!player.hasPermission("branzidle.admin")) {
                    player.sendMessage("§cYou do not have permission to use the debug fusion command!");
                    return true;
                }
                if (args.length < 4) {
                    player.sendMessage("§cUsage: /idle fuse <targetSerial> <ingredientSerial1> <ingredientSerial2>");
                    return true;
                }
                try {
                    int targetSerial = Integer.parseInt(args[1]);
                    int ing1Serial = Integer.parseInt(args[2]);
                    int ing2Serial = Integer.parseInt(args[3]);

                    List<WorkerInstance> workers = workerService.getPlayerWorkers(player.getUniqueId());
                    WorkerInstance target = null;
                    WorkerInstance w1 = null;
                    WorkerInstance w2 = null;

                    for (WorkerInstance w : workers) {
                        if (w.getSerialId() == targetSerial) {
                            target = w;
                        } else if (w.getSerialId() == ing1Serial) {
                            w1 = w;
                        } else if (w.getSerialId() == ing2Serial) {
                            w2 = w;
                        }
                    }

                    if (target == null || w1 == null || w2 == null) {
                        player.sendMessage("§cCould not find one or more workers with those serial IDs!");
                        return true;
                    }

                    workerService.fuseWorkers(player, target.getWorkerId(), w1.getWorkerId(), w2.getWorkerId());
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid serial IDs! They must be integers.");
                }
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
                subs.add("fuse");
            }
            String prefix = args[0].toLowerCase();
            return subs.stream().filter(s -> s.startsWith(prefix)).collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("claim")) {
            String prefix = args[1].toUpperCase();
            List<String> types = new ArrayList<>();
            for (NodeType t : NodeType.values()) {
                types.add(t.name());
            }
            types.add("RESIDENTIAL");
            return types.stream().filter(s -> s.startsWith(prefix)).collect(Collectors.toList());
        }
        if (args.length >= 2 && args.length <= 4 && args[0].equalsIgnoreCase("fuse")) {
            if (!player.hasPermission("branzidle.admin")) {
                return Collections.emptyList();
            }
            String prefix = args[args.length - 1];
            return workerService.getPlayerWorkers(player.getUniqueId()).stream()
                .map(w -> String.valueOf(w.getSerialId()))
                .filter(s -> s.startsWith(prefix))
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
