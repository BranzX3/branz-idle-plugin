package com.example.plugin.gui;

import com.example.plugin.config.RegistryManager;
import com.example.plugin.economy.service.EconomyService;
import com.example.plugin.gui.item.ItemBuilder;
import com.example.plugin.node.model.ProductionNode;
import com.example.plugin.node.service.NodeService;
import com.example.plugin.onboarding.service.OnboardingService;
import com.example.plugin.storage.service.StorageService;
import com.example.plugin.territory.service.TerritoryService;
import com.example.plugin.worker.model.WorkerInstance;
import com.example.plugin.worker.service.WorkerService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Node selection GUI (54 slots) displaying all production nodes owned by the player.
 * Supports two modes: OVERVIEW opens NodeOverviewGUI, WORKER opens WorkerManagementGUI.
 */
public class NodeSelectionGUI implements InventoryProvider {

    /**
     * Determines which GUI opens when a node is selected.
     */
    public enum Mode {
        /** Opens NodeOverviewGUI for the selected node */
        OVERVIEW,
        /** Opens WorkerManagementGUI for the selected node */
        WORKER
    }

    private final Player player;
    private final Mode mode;
    private final TerritoryService territoryService;
    private final NodeService nodeService;
    private final WorkerService workerService;
    private final StorageService storageService;
    private final EconomyService economyService;
    private final OnboardingService onboardingService;
    private final RegistryManager registryManager;
    private final Inventory inventory;
    private final Map<Integer, UUID> slotToNodeMap = new HashMap<>();

    public NodeSelectionGUI(
        Player player,
        Mode mode,
        TerritoryService territoryService,
        NodeService nodeService,
        WorkerService workerService,
        StorageService storageService,
        EconomyService economyService,
        OnboardingService onboardingService,
        RegistryManager registryManager
    ) {
        this.player = player;
        this.mode = mode;
        this.territoryService = territoryService;
        this.nodeService = nodeService;
        this.workerService = workerService;
        this.storageService = storageService;
        this.economyService = economyService;
        this.onboardingService = onboardingService;
        this.registryManager = registryManager;

        String title = mode == Mode.OVERVIEW ? "§8Select Node to View" : "§8Select Node for Workers";
        this.inventory = Bukkit.createInventory(this, 54, title);
        populate();
    }

    private void populate() {
        inventory.clear();
        slotToNodeMap.clear();

        // Border rows
        ItemStack glass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("§r").build();
        for (int i = 0; i < 9; i++) inventory.setItem(i, glass);
        for (int i = 45; i < 54; i++) inventory.setItem(i, glass);

        // Header
        String headerName = mode == Mode.OVERVIEW ? "§6§lYour Production Nodes" : "§d§lSelect Node for Worker Management";
        inventory.setItem(4, new ItemBuilder(Material.BEACON)
            .name(headerName)
            .lore("§7Click a node below to " + (mode == Mode.OVERVIEW ? "view its details." : "manage its workers.")).build());

        // List player's nodes in slots 9-44
        List<ProductionNode> nodes = nodeService.getPlayerNodes(player.getUniqueId());
        int slot = 9;

        com.example.plugin.exploration.service.ExplorationService explorationService =
            org.bukkit.plugin.java.JavaPlugin.getPlugin(com.example.plugin.bootstrap.BranzIdlePlugin.class)
                .getServiceRegistry()
                .getRequiredService(com.example.plugin.exploration.service.ExplorationService.class);

        for (ProductionNode node : nodes) {
            if (slot >= 45) break;

            List<WorkerInstance> assignedWorkers = workerService.getNodeWorkers(node.getNodeId());
            Material icon = getNodeIcon(node.getNodeType());
            com.example.plugin.exploration.model.NodeExploration exp = explorationService.getExploration(node.getNodeId());

            String eventStatus = null;
            if (node.getActiveEventKey() != null) {
                String prefix = node.getNodeType().name().toLowerCase();
                String eventDisplayName = registryManager.getEventDropRegistry().getEvent(prefix, node.getActiveEventKey())
                    .map(com.example.plugin.config.EventDropRegistry.EventDefinition::displayName)
                    .orElse(node.getActiveEventKey());
                eventStatus = "§d§l★ Event Active: §d" + org.bukkit.ChatColor.translateAlternateColorCodes('&', eventDisplayName) + " §7(" + node.getEventProgress() + " cycles left)";
            }

            ItemBuilder builder = new ItemBuilder(icon)
                .name("§e§l" + node.getNodeType().name() + " Node")
                .lore(
                    "§7Tier: §f" + node.getTierKey(),
                    "§7Level: §a" + node.getLevel(),
                    "§7Exploration Level: §bLv." + exp.getExplorationLevel(),
                    "§7Workers: §d" + assignedWorkers.size()
                );

            if (eventStatus != null) {
                builder.addLore("", eventStatus);
            }

            builder.addLore("", "§eClick to " + (mode == Mode.OVERVIEW ? "view details!" : "manage workers!"));
            inventory.setItem(slot, builder.build());

            slotToNodeMap.put(slot, node.getNodeId());
            slot++;
        }

        if (nodes.isEmpty()) {
            inventory.setItem(22, new ItemBuilder(Material.BARRIER)
                .name("§c§lNo Nodes Found")
                .lore(
                    "§7You don't have any production nodes yet.",
                    "§7Use §e/idle map §7to claim territory and build nodes!"
                ).build());
        }

        // Back to Hub button
        inventory.setItem(49, new ItemBuilder(Material.ARROW)
            .name("§c§lBack to Hub").build());
    }

    private Material getNodeIcon(com.example.plugin.node.model.NodeType type) {
        return switch (type) {
            case MINING -> Material.IRON_PICKAXE;
            case LUMBER -> Material.IRON_AXE;
            case FISHING -> Material.FISHING_ROD;
        };
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot == 49) {
            // Back to Hub
            player.openInventory(new MainHubGUI(player, territoryService, nodeService, workerService, storageService, economyService, onboardingService, registryManager).getInventory());
            return;
        }

        UUID nodeId = slotToNodeMap.get(slot);
        if (nodeId != null) {
            nodeService.getNode(nodeId).ifPresent(node -> {
                if (mode == Mode.OVERVIEW) {
                    player.openInventory(new NodeOverviewGUI(player, node, nodeService, workerService, storageService, economyService, registryManager, territoryService, onboardingService).getInventory());
                } else {
                    player.openInventory(new WorkerManagementGUI(player, node, workerService, registryManager, nodeService, storageService, economyService, territoryService, onboardingService).getInventory());
                }
            });
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
