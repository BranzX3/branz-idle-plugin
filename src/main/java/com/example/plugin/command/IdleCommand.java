package com.example.plugin.command;

import com.example.plugin.config.RegistryManager;
import com.example.plugin.economy.service.EconomyService;
import com.example.plugin.gui.AdminHubGUI;
import com.example.plugin.gui.GachaPullGUI;
import com.example.plugin.gui.MainHubGUI;
import com.example.plugin.gui.MarketplaceGUI;
import com.example.plugin.gui.NodeSelectionGUI;
import com.example.plugin.gui.TerritoryMapGUI;
import com.example.plugin.gui.WarehouseGUI;
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
                    int currentNodes = nodeService.getPlayerNodes(player.getUniqueId()).size();
                    int unlockedLimit = economyService.getProfile(player.getUniqueId()).map(com.example.plugin.economy.model.PlayerProfile::getUnlockedSlots).orElse(4);
                    if (currentNodes >= unlockedLimit) {
                        player.sendMessage("§cYou have reached your active node slot limit (" + currentNodes + "/" + unlockedLimit + ")!");
                        player.sendMessage("§eUnlock more node slots in the Hub menu to build more nodes.");
                        return true;
                    }
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
            case "workshop" -> {
                if (!player.hasPermission("branzidle.admin")) {
                    player.sendMessage("§cYou do not have permission to use workshop commands.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /idle workshop <edit/save/cancel/validate/preview> [schematic]");
                    return true;
                }
                String action = args[1].toLowerCase();
                switch (action) {
                    case "edit" -> {
                        if (args.length < 3) {
                            player.sendMessage("§cUsage: /idle workshop edit <schematic_name>");
                            return true;
                        }
                        String schemName = args[2];
                        org.bukkit.Chunk chunk = player.getLocation().getChunk();
                        Location origin = new Location(player.getWorld(), (chunk.getX() * 16) + 8.0, player.getLocation().getY(), (chunk.getZ() * 16) + 8.0);

                        com.example.plugin.visual.model.VisualEditSession session = new com.example.plugin.visual.model.VisualEditSession(schemName, origin);

                        try {
                            com.example.plugin.bootstrap.BranzIdlePlugin mainPlugin = org.bukkit.plugin.java.JavaPlugin.getPlugin(com.example.plugin.bootstrap.BranzIdlePlugin.class);
                            var manager = mainPlugin.getServiceRegistry().getRequiredService(com.example.plugin.visual.service.WorkshopBlueprintManager.class);
                            com.example.plugin.visual.model.WorkshopBlueprint existing = manager.getBlueprint(schemName);
                            if (existing != null && !existing.getMarkers().isEmpty()) {
                                session.getMarkers().putAll(existing.getMarkers());
                                session.getPaths().putAll(existing.getPaths());
                                player.sendMessage("§a[Editor] Loaded " + existing.getMarkers().size() + " existing markers for " + schemName);
                            }
                        } catch (Exception ignored) {}

                        com.example.plugin.visual.listener.VisualEditListener.startSession(player.getUniqueId(), session);

                        org.bukkit.inventory.ItemStack wand = new org.bukkit.inventory.ItemStack(Material.BLAZE_ROD);
                        org.bukkit.inventory.meta.ItemMeta meta = wand.getItemMeta();
                        if (meta != null) {
                            meta.setDisplayName("§d§lVisual Editor Wand");
                            java.util.List<String> lore = java.util.List.of(
                                "§7Use to set NPC pathfinding waypoints.",
                                "§aLeft-Click Block: §7Add WORK Marker",
                                "§dRight-Click Block: §7Cycle marker type (WORK/REST/STORAGE/QUEUE)",
                                "§bShift + Left-Click: §7Cycle allowed animations/actions",
                                "§cShift + Right-Click: §7Delete nearest marker"
                            );
                            meta.setLore(lore);
                            wand.setItemMeta(meta);
                        }
                        player.getInventory().addItem(wand);

                        player.sendMessage("§a§lEntered Workshop Edit Mode for §e" + schemName);
                        player.sendMessage("§7Origin location set at center of your chunk: " + String.format("%.1f, %.1f, %.1f", origin.getX(), origin.getY(), origin.getZ()));
                        player.sendMessage("§7You have been given a §dVisual Editor Wand§7. Use click actions to place markers.");
                    }
                    case "save" -> {
                        com.example.plugin.visual.model.VisualEditSession session = com.example.plugin.visual.listener.VisualEditListener.removeSession(player.getUniqueId());
                        if (session == null) {
                            player.sendMessage("§cYou are not in an active workshop edit session.");
                            return true;
                        }

                        boolean hasRest = false;
                        boolean hasWork = false;
                        boolean duplicateFound = false;
                        boolean outOfBounds = false;
                        java.util.Set<String> coords = new java.util.HashSet<>();

                        for (Map.Entry<String, com.example.plugin.visual.model.WorkshopBlueprint.MarkerDefinition> entry : session.getMarkers().entrySet()) {
                            var m = entry.getValue();
                            if ("REST".equalsIgnoreCase(m.getType())) hasRest = true;
                            if ("WORK".equalsIgnoreCase(m.getType())) hasWork = true;

                            String hash = String.format("%.2f,%.2f,%.2f", m.getX(), m.getY(), m.getZ());
                            if (!coords.add(hash)) duplicateFound = true;

                            if (Math.abs(m.getX()) > 8.0 || Math.abs(m.getZ()) > 8.0) outOfBounds = true;
                        }

                        if (!hasRest) player.sendMessage("§e[Warning] Blueprint lacks a REST type marker. Workers cannot rest.");
                        if (!hasWork) player.sendMessage("§e[Warning] Blueprint lacks a WORK type marker. Workers cannot work.");
                        if (duplicateFound) player.sendMessage("§e[Warning] Found multiple markers occupying the exact same coordinates.");
                        if (outOfBounds) player.sendMessage("§e[Warning] Some markers are placed outside the chunk boundary limits (-8 to +8 offsets).");

                        try {
                            com.example.plugin.bootstrap.BranzIdlePlugin mainPlugin = org.bukkit.plugin.java.JavaPlugin.getPlugin(com.example.plugin.bootstrap.BranzIdlePlugin.class);
                            var manager = mainPlugin.getServiceRegistry().getRequiredService(com.example.plugin.visual.service.WorkshopBlueprintManager.class);
                            com.example.plugin.visual.model.WorkshopBlueprint blueprint = new com.example.plugin.visual.model.WorkshopBlueprint();
                            blueprint.setVersion(2);
                            blueprint.getMetadata().setName(session.getSchematicName());
                            blueprint.getMetadata().setAuthor(player.getName());
                            blueprint.getMarkers().putAll(session.getMarkers());
                            blueprint.getPaths().putAll(session.getPaths());

                            manager.saveBlueprint(session.getSchematicName(), blueprint);
                            player.sendMessage("§a§l[Editor] Workshop Blueprint for " + session.getSchematicName() + " saved successfully!");
                        } catch (Exception e) {
                            player.sendMessage("§cError saving workshop blueprint: " + e.getMessage());
                        }

                        player.getInventory().remove(Material.BLAZE_ROD);
                    }
                    case "cancel" -> {
                        com.example.plugin.visual.model.VisualEditSession session = com.example.plugin.visual.listener.VisualEditListener.removeSession(player.getUniqueId());
                        if (session == null) {
                            player.sendMessage("§cYou are not in an active workshop edit session.");
                            return true;
                        }
                        player.sendMessage("§c[Editor] Cancelled edit session without saving.");
                        player.getInventory().remove(Material.BLAZE_ROD);
                    }
                    case "validate" -> {
                        if (args.length < 3) {
                            player.sendMessage("§cUsage: /idle workshop validate <schematic_name>");
                            return true;
                        }
                        String schemName = args[2];
                        try {
                            com.example.plugin.bootstrap.BranzIdlePlugin mainPlugin = org.bukkit.plugin.java.JavaPlugin.getPlugin(com.example.plugin.bootstrap.BranzIdlePlugin.class);
                            var manager = mainPlugin.getServiceRegistry().getRequiredService(com.example.plugin.visual.service.WorkshopBlueprintManager.class);
                            com.example.plugin.visual.model.WorkshopBlueprint bp = manager.getBlueprint(schemName);

                            boolean hasRest = false;
                            boolean hasWork = false;
                            boolean duplicateFound = false;
                            boolean outOfBounds = false;
                            java.util.Set<String> coords = new java.util.HashSet<>();

                            for (var m : bp.getMarkers().values()) {
                                if ("REST".equalsIgnoreCase(m.getType())) hasRest = true;
                                if ("WORK".equalsIgnoreCase(m.getType())) hasWork = true;

                                String hash = String.format("%.2f,%.2f,%.2f", m.getX(), m.getY(), m.getZ());
                                if (!coords.add(hash)) duplicateFound = true;

                                if (Math.abs(m.getX()) > 8.0 || Math.abs(m.getZ()) > 8.0) outOfBounds = true;
                            }

                            player.sendMessage("§b§l[Validation Report: " + schemName + "]");
                            player.sendMessage("§7Markers checked: §e" + bp.getMarkers().size());
                            player.sendMessage("§7REST markers: " + (hasRest ? "§aYes" : "§cNo"));
                            player.sendMessage("§7WORK markers: " + (hasWork ? "§aYes" : "§cNo"));
                            player.sendMessage("§7Coordinate overlap: " + (duplicateFound ? "§cOverlap Detected" : "§aClean"));
                            player.sendMessage("§7Fit chunk bounds: " + (outOfBounds ? "§cOffsets Out of Bounds" : "§aFit"));
                        } catch (Exception e) {
                            player.sendMessage("§cError validating blueprint: " + e.getMessage());
                        }
                    }
                    case "preview" -> {
                        if (args.length < 3) {
                            player.sendMessage("§cUsage: /idle workshop preview <schematic_name>");
                            return true;
                        }
                        String schemName = args[2];
                        try {
                            com.example.plugin.bootstrap.BranzIdlePlugin mainPlugin = org.bukkit.plugin.java.JavaPlugin.getPlugin(com.example.plugin.bootstrap.BranzIdlePlugin.class);
                            var visualService = mainPlugin.getServiceRegistry().getRequiredService(com.example.plugin.visual.VisualService.class);
                            var provider = visualService.getProvider();
                            if (provider != null) {
                                provider.spawnDummyPreview(player, schemName);
                            } else {
                                player.sendMessage("§cVisual provider is not active.");
                            }
                        } catch (Exception e) {
                            player.sendMessage("§cError starting blueprint preview: " + e.getMessage());
                        }
                    }
                    default -> player.sendMessage("§cUnknown workshop action. Use: edit, save, cancel, validate, preview");
                }
            }
            case "node" -> {
                // Open Node Selection GUI (Overview mode) — player picks which node to view
                player.openInventory(new NodeSelectionGUI(player, NodeSelectionGUI.Mode.OVERVIEW, territoryService, nodeService, workerService, storageService, economyService, onboardingService, registryManager).getInventory());
            }
            case "expand" -> {
                // Open Node Selection GUI in OVERVIEW mode to select a node to expand / upgrade
                player.openInventory(new NodeSelectionGUI(player, NodeSelectionGUI.Mode.OVERVIEW, territoryService, nodeService, workerService, storageService, economyService, onboardingService, registryManager).getInventory());
            }
            case "storage" -> {
                // Open WarehouseGUI
                player.openInventory(new WarehouseGUI(player, storageService, economyService, territoryService, nodeService, workerService, onboardingService, registryManager).getInventory());
            }
            case "settings" -> {
                // Settings cycles the style of the node the player is standing on
                org.bukkit.Chunk chunk = player.getLocation().getChunk();
                Optional<com.example.plugin.territory.model.ChunkClaim> claimOpt = territoryService.getClaim(chunk.getX(), chunk.getZ());
                Optional<ProductionNode> nodeOpt = Optional.empty();
                if (claimOpt.isPresent() && claimOpt.get().getNodeId() != null) {
                    nodeOpt = nodeService.getNode(claimOpt.get().getNodeId());
                }
                if (nodeOpt.isPresent()) {
                    ProductionNode node = nodeOpt.get();
                    if (!node.getOwnerId().equals(player.getUniqueId()) && !player.hasPermission("branzidle.admin")) {
                        player.sendMessage("§cYou do not own this production node!");
                        return true;
                    }

                    // Define style pools
                    List<String> styles = switch (node.getNodeType()) {
                        case MINING -> List.of("default", "cave", "mine", "dwarf_mine", "crystal_mine", "industrial_mine");
                        case LUMBER -> List.of("default", "forest", "outpost", "elderwood");
                        case FISHING -> List.of("default", "pond", "lake", "ocean");
                    };

                    int currentIdx = styles.indexOf(node.getStyleId());
                    int nextIdx = (currentIdx + 1) % styles.size();
                    if (nextIdx < 0) nextIdx = 0;
                    String nextStyle = styles.get(nextIdx);

                    node.setStyleId(nextStyle);
                    nodeService.updateNode(node);

                    // Re-paste schematic for updated style!
                    try {
                        com.example.plugin.bootstrap.BranzIdlePlugin idlePlugin = org.bukkit.plugin.java.JavaPlugin.getPlugin(com.example.plugin.bootstrap.BranzIdlePlugin.class);
                        Optional<com.example.plugin.integration.provider.StructureProvider> spOpt = idlePlugin.getServiceRegistry().getService(com.example.plugin.integration.provider.StructureProvider.class);
                        if (spOpt.isPresent()) {
                            org.bukkit.World world = player.getWorld();
                            double cx = (chunk.getX() * 16) + 8.0;
                            double cz = (chunk.getZ() * 16) + 8.0;
                            double cy = world.getHighestBlockYAt((int) cx, (int) cz);
                            if (cy <= 0) cy = 64;
                            org.bukkit.Location loc = new org.bukkit.Location(world, cx, cy, cz);

                            String tierKey = node.getTierKey();
                            Optional<com.example.plugin.config.NodeRegistry.NodeDefinition> defOpt = registryManager.getNodeRegistry().getNodeDefinition(tierKey);
                            if (defOpt.isPresent()) {
                                String schematicFile = defOpt.get().schematic();
                                org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                                    boolean success = spOpt.get().pasteSchematic(node.getNodeType().name().toLowerCase() + "_" + nextStyle, node.getLevel(), loc);
                                    if (!success) {
                                        // fallback to default schematic file
                                        spOpt.get().pasteSchematic(schematicFile, -1, loc);
                                    }
                                });
                            }
                        }
                    } catch (Exception ignored) {}

                    player.sendMessage("§aChanged node style to: §e" + nextStyle);
                } else {
                    player.sendMessage("§cStand inside one of your production nodes to change its style settings!");
                }
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
