package com.example.plugin.worker.gui;

import com.example.plugin.config.NodeRegistry;
import com.example.plugin.config.RegistryManager;
import com.example.plugin.economy.service.EconomyService;
import com.example.plugin.gui.ConfirmationGUI;
import com.example.plugin.gui.InventoryProvider;
import com.example.plugin.gui.MainHubGUI;
import com.example.plugin.gui.NodeOverviewGUI;
import com.example.plugin.gui.NodeSelectionGUI;
import com.example.plugin.gui.item.ItemBuilder;
import com.example.plugin.node.model.ProductionNode;
import com.example.plugin.node.service.NodeService;
import com.example.plugin.onboarding.service.OnboardingService;
import com.example.plugin.storage.service.StorageService;
import com.example.plugin.territory.service.TerritoryService;
import com.example.plugin.worker.biography.WorkerBiographyService;
import com.example.plugin.worker.model.WorkerInstance;
import com.example.plugin.worker.model.WorkerStats;
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
import java.util.Optional;
import java.util.UUID;

public class WorkerManagementGUI implements InventoryProvider {

    private final Player player;
    private final ProductionNode node;
    private final WorkerService workerService;
    private final RegistryManager registryManager;
    private final NodeService nodeService;
    private final StorageService storageService;
    private final EconomyService economyService;
    private final TerritoryService territoryService;
    private final OnboardingService onboardingService;
    private final Inventory inventory;
    private final Map<Integer, UUID> slotToWorkerMap = new HashMap<>();

    public WorkerManagementGUI(
        Player player,
        ProductionNode node,
        WorkerService workerService,
        RegistryManager registryManager,
        NodeService nodeService,
        StorageService storageService,
        EconomyService economyService,
        TerritoryService territoryService,
        OnboardingService onboardingService
    ) {
        this.player = player;
        this.node = node;
        this.workerService = workerService;
        this.registryManager = registryManager;
        this.nodeService = nodeService;
        this.storageService = storageService;
        this.economyService = economyService;
        this.territoryService = territoryService;
        this.onboardingService = onboardingService;
        this.inventory = Bukkit.createInventory(this, 54, "§8Manage Workers: §a" + node.getTierKey());
        populate();
    }

    private void populate() {
        inventory.clear();
        slotToWorkerMap.clear();

        ItemStack glass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("§r").build();
        for (int i = 0; i < 9; i++) inventory.setItem(i, glass);
        for (int i = 45; i < 54; i++) inventory.setItem(i, glass);

        inventory.setItem(4, new ItemBuilder(Material.NAME_TAG)
            .name("§e§lWorker Management")
            .lore(
                "§7Click an unassigned worker below to assign to this node.",
                "§7Click an assigned worker to unassign it."
            ).build());

        List<WorkerInstance> allWorkers = workerService.getPlayerWorkers(player.getUniqueId());
        int slot = 9;

        WorkerBiographyService bioService = workerService.getBiographyService();

        for (WorkerInstance w : allWorkers) {
            if (slot >= 45) break;

            boolean isAssignedHere = node.getNodeId().equals(w.getAssignedNodeId());
            boolean isAssignedElsewhere = w.getAssignedNodeId() != null && !isAssignedHere;

            String rarityStr = "COMMON";
            String templateDisplayName = w.getTemplateId();
            if (registryManager != null) {
                var templateOpt = registryManager.getWorkerRegistry().getTemplate(w.getTemplateId());
                if (templateOpt.isPresent()) {
                    rarityStr = templateOpt.get().rarity().name();
                    templateDisplayName = org.bukkit.ChatColor.translateAlternateColorCodes('&', templateOpt.get().displayName());
                }
            }

            WorkerStats stats = workerService.getEffectiveStats(w);

            // Format potentials as cosmetic grades
            String speedGrade = bioService.getPotentialGrade(w.getSpeedPotential());
            String yieldGrade = bioService.getPotentialGrade(w.getYieldPotential());
            String rareGrade = bioService.getPotentialGrade(w.getRarePotential());

            // Format biography
            long hoursWorked = w.getSecondsWorked() / 3600L;
            String personalityStr = w.getPersonality().substring(0, 1).toUpperCase() + w.getPersonality().substring(1).toLowerCase();
            String pDisplayName = bioService.getPersonalityDisplayName(w.getPersonality());

            ItemBuilder builder = new ItemBuilder(Material.PLAYER_HEAD)
                .name((w.getCustomTitle() != null ? "§d" + w.getCustomTitle() + " " : "") + "§e" + templateDisplayName + " §8(Lv." + w.getLevel() + ")")
                .lore(
                    "§7Rarity: §d" + rarityStr,
                    "§7Serial: §bBID-" + String.format("%06d", w.getSerialId()),
                    "",
                    "§e§l【 STATS 】",
                    "§7Speed Bonus: §a+" + String.format("%.0f", stats.speedBonus() * 100) + "% §8(Grade: " + speedGrade + "§8)",
                    "§7Yield Bonus: §b+" + String.format("%.0f", stats.yieldBonus() * 100) + "% §8(Grade: " + yieldGrade + "§8)",
                    "§7Rare Drop Bonus: §d+" + String.format("%.0f", stats.rareDropBonus() * 100) + "% §8(Grade: " + rareGrade + "§8)",
                    "§7Experience: §e" + w.getExperience() + " §7/ §e" + (w.getLevel() * 500) + " XP",
                    "",
                    "§e§l【 BIOGRAPHY 】",
                    "§7Personality: §6" + pDisplayName,
                    "§7Born In: §f" + w.getBornLocation(),
                    "§7Generation: §aGen " + w.getGeneration(),
                    "§7Fusion Count: §a" + w.getFusionCount(),
                    "§7Hours Worked: §b" + hoursWorked + " Hrs"
                );

            // Append assignment status
            if (isAssignedHere) {
                builder.addLore("", "§a§l[ASSIGNED TO THIS NODE]", "§cClick to unassign.");
            } else if (isAssignedElsewhere) {
                builder.addLore("", "§c§l[ASSIGNED TO ANOTHER NODE]", "§7Cannot assign here unless unassigned.");
            } else {
                builder.addLore("", "§e§l[AVAILABLE]", "§aClick to assign to this node.");
            }

            inventory.setItem(slot, builder.build());
            slotToWorkerMap.put(slot, w.getWorkerId());
            slot++;
        }

        // Slot 45: Back to Hub
        inventory.setItem(45, new ItemBuilder(Material.ARROW)
            .name("§c§lBack to Hub").build());

        // Slot 46: Back to Node Overview
        inventory.setItem(46, new ItemBuilder(Material.BEACON)
            .name("§e§lBack to Node").build());

        // Slot 47: Open Worker Fusion GUI
        inventory.setItem(47, new ItemBuilder(Material.ANVIL)
            .name("§d§lWorker Fusion")
            .lore("§7Combine 3 duplicate workers", "§7to upgrade rarity and inherit potentials.").build());

        // Slot 49: Close Menu
        inventory.setItem(49, new ItemBuilder(Material.BARRIER)
            .name("§c§lClose Menu").build());
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot == 45) {
            player.openInventory(new MainHubGUI(player, territoryService, nodeService, workerService, storageService, economyService, onboardingService, registryManager).getInventory());
            return;
        }

        if (slot == 46) {
            player.openInventory(new NodeOverviewGUI(player, node, nodeService, workerService, storageService, economyService, registryManager, territoryService, onboardingService).getInventory());
            return;
        }

        if (slot == 47) {
            player.openInventory(new WorkerFusionGUI(player, node, workerService, registryManager, nodeService, storageService, economyService, territoryService, onboardingService).getInventory());
            return;
        }

        if (slot == 49) {
            player.closeInventory();
            return;
        }

        UUID workerId = slotToWorkerMap.get(slot);
        if (workerId != null) {
            Optional<WorkerInstance> workerOpt = workerService.getWorker(workerId);
            if (workerOpt.isPresent()) {
                WorkerInstance w = workerOpt.get();
                if (node.getNodeId().equals(w.getAssignedNodeId())) {
                    player.openInventory(new ConfirmationGUI(
                        player,
                        "Unassign Worker",
                        List.of(
                            "§7Worker: §e" + w.getTemplateId(),
                            "§7This will remove the worker from this node.",
                            "§7The worker can be reassigned later."
                        ),
                        () -> {
                            workerService.unassignWorker(player, workerId);
                            player.openInventory(new WorkerManagementGUI(player, node, workerService, registryManager, nodeService, storageService, economyService, territoryService, onboardingService).getInventory());
                        },
                        () -> player.openInventory(new WorkerManagementGUI(player, node, workerService, registryManager, nodeService, storageService, economyService, territoryService, onboardingService).getInventory())
                    ).getInventory());
                } else if (w.getAssignedNodeId() == null) {
                    workerService.assignWorker(player, workerId, node.getNodeId());
                    populate();
                } else {
                    player.sendMessage("§cThis worker is currently assigned to another node!");
                }
            }
        }
    }
}
