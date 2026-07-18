package com.example.plugin.gui;

import com.example.plugin.config.NodeRegistry;
import com.example.plugin.config.RegistryManager;
import com.example.plugin.economy.service.EconomyService;
import com.example.plugin.gui.item.ItemBuilder;
import com.example.plugin.node.model.ProductionNode;
import com.example.plugin.node.service.NodeService;
import com.example.plugin.onboarding.service.OnboardingService;
import com.example.plugin.storage.model.NodeStorage;
import com.example.plugin.storage.service.StorageService;
import com.example.plugin.territory.service.TerritoryService;
import com.example.plugin.worker.model.WorkerInstance;
import com.example.plugin.worker.service.WorkerService;
import com.example.plugin.worker.gui.WorkerManagementGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Interactive GUI providing an overview of a production node's tier, worker slots, yield buffer, and upgrades.
 * Navigates to WorkerManagementGUI for worker slots, and back to MainHubGUI or NodeSelectionGUI.
 */
public class NodeOverviewGUI implements InventoryProvider {

    private final Player player;
    private final ProductionNode node;
    private final NodeService nodeService;
    private final WorkerService workerService;
    private final StorageService storageService;
    private final EconomyService economyService;
    private final RegistryManager registryManager;
    private final TerritoryService territoryService;
    private final OnboardingService onboardingService;
    private final Inventory inventory;

    public NodeOverviewGUI(
        Player player,
        ProductionNode node,
        NodeService nodeService,
        WorkerService workerService,
        StorageService storageService,
        EconomyService economyService,
        RegistryManager registryManager,
        TerritoryService territoryService,
        OnboardingService onboardingService
    ) {
        this.player = player;
        this.node = node;
        this.nodeService = nodeService;
        this.workerService = workerService;
        this.storageService = storageService;
        this.economyService = economyService;
        this.registryManager = registryManager;
        this.territoryService = territoryService;
        this.onboardingService = onboardingService;
        this.inventory = Bukkit.createInventory(this, 36, "§8Node Overview: §a" + node.getTierKey());
        populate();
    }

    private void populate() {
        inventory.clear();

        // Background glass
        ItemStack glass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("§r").build();
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, glass);
        }

        // Slot 4: Node Info
        NodeStorage storage = storageService.getNodeStorage(node.getNodeId());
        long totalStored = storage.getResourceQuantities().values().stream().mapToLong(Long::longValue).sum();

        com.example.plugin.exploration.service.ExplorationService explorationService =
            org.bukkit.plugin.java.JavaPlugin.getPlugin(com.example.plugin.bootstrap.BranzIdlePlugin.class)
                .getServiceRegistry()
                .getRequiredService(com.example.plugin.exploration.service.ExplorationService.class);
        com.example.plugin.exploration.model.NodeExploration exp = explorationService.getExploration(node.getNodeId());
        long currentXp = exp.getExperience();
        long requiredXp = explorationService.getRequiredExperience(exp.getExplorationLevel());
        double percent = requiredXp > 0 ? (currentXp * 100.0 / requiredXp) : 0.0;
        String bar = getProgressBar(currentXp, requiredXp);

        String eventStatusName = null;
        String eventStatusCycles = null;
        if (node.getActiveEventKey() != null) {
            String prefix = node.getNodeType().name().toLowerCase();
            String eventDisplayName = registryManager.getEventDropRegistry().getEvent(prefix, node.getActiveEventKey())
                .map(com.example.plugin.config.EventDropRegistry.EventDefinition::displayName)
                .orElse(node.getActiveEventKey());
            eventStatusName = "§d§l★ Event Active: §d" + org.bukkit.ChatColor.translateAlternateColorCodes('&', eventDisplayName);
            eventStatusCycles = "§d§l★ Event Cycles Left: §b" + node.getEventProgress() + " cycles";
        }

        int expLvl = exp.getExplorationLevel();
        String zoneName;
        String nextMilestone;
        if (expLvl >= 900) {
            zoneName = switch (node.getNodeType()) {
                case LUMBER -> "Ancient Underworld";
                case FISHING -> "Deep Ocean Abyss";
                default -> "Primordial Core";
            };
            nextMilestone = "Max Zone Reached";
        } else if (expLvl >= 800) {
            zoneName = switch (node.getNodeType()) {
                case LUMBER -> "Warped Nether Forest";
                case FISHING -> "Abyssal Ruins";
                default -> "World Core";
            };
            nextMilestone = switch (node.getNodeType()) {
                case LUMBER -> "Lv.900 (Ancient Underworld)";
                case FISHING -> "Lv.900 (Deep Ocean Abyss)";
                default -> "Lv.900 (Primordial Core)";
            };
        } else if (expLvl >= 700) {
            zoneName = switch (node.getNodeType()) {
                case LUMBER -> "Crimson Nether Forest";
                case FISHING -> "Forgotten Trench";
                default -> "Crystal Core";
            };
            nextMilestone = switch (node.getNodeType()) {
                case LUMBER -> "Lv.800 (Warped Nether Forest)";
                case FISHING -> "Lv.800 (Abyssal Ruins)";
                default -> "Lv.800 (World Core)";
            };
        } else if (expLvl >= 600) {
            zoneName = switch (node.getNodeType()) {
                case LUMBER -> "Cherry Blossom Grove";
                case FISHING -> "Sunken Coral Reef";
                default -> "Forgotten Depths";
            };
            nextMilestone = switch (node.getNodeType()) {
                case LUMBER -> "Lv.700 (Crimson Nether Forest)";
                case FISHING -> "Lv.700 (Forgotten Trench)";
                default -> "Lv.700 (Crystal Core)";
            };
        } else if (expLvl >= 500) {
            zoneName = switch (node.getNodeType()) {
                case LUMBER -> "Mangrove Swamp";
                case FISHING -> "Abyssal Trench";
                default -> "Ancient Mine";
            };
            nextMilestone = switch (node.getNodeType()) {
                case LUMBER -> "Lv.600 (Cherry Blossom Grove)";
                case FISHING -> "Lv.600 (Sunken Coral Reef)";
                default -> "Lv.600 (Forgotten Depths)";
            };
        } else if (expLvl >= 400) {
            zoneName = switch (node.getNodeType()) {
                case LUMBER -> "Jungle Sanctuary";
                case FISHING -> "Open Sea";
                default -> "Volcanic Mine";
            };
            nextMilestone = switch (node.getNodeType()) {
                case LUMBER -> "Lv.500 (Mangrove Swamp)";
                case FISHING -> "Lv.500 (Abyssal Trench)";
                default -> "Lv.500 (Ancient Mine)";
            };
        } else if (expLvl >= 300) {
            zoneName = switch (node.getNodeType()) {
                case LUMBER -> "Dark Canopy";
                case FISHING -> "Reef Bay";
                default -> "Crystal Cavern";
            };
            nextMilestone = switch (node.getNodeType()) {
                case LUMBER -> "Lv.400 (Jungle Sanctuary)";
                case FISHING -> "Lv.400 (Open Sea)";
                default -> "Lv.400 (Volcanic Mine)";
            };
        } else if (expLvl >= 200) {
            zoneName = switch (node.getNodeType()) {
                case LUMBER -> "Dense Spruce Forest";
                case FISHING -> "Deep Lake";
                default -> "Deep Cave";
            };
            nextMilestone = switch (node.getNodeType()) {
                case LUMBER -> "Lv.300 (Dark Canopy)";
                case FISHING -> "Lv.300 (Reef Bay)";
                default -> "Lv.300 (Crystal Cavern)";
            };
        } else if (expLvl >= 100) {
            zoneName = switch (node.getNodeType()) {
                case LUMBER -> "Birch Woods";
                case FISHING -> "River Delta";
                default -> "Shallow Cave";
            };
            nextMilestone = switch (node.getNodeType()) {
                case LUMBER -> "Lv.200 (Dense Spruce Forest)";
                case FISHING -> "Lv.200 (Deep Lake)";
                default -> "Lv.200 (Deep Cave)";
            };
        } else {
            zoneName = switch (node.getNodeType()) {
                case LUMBER -> "Grassland Edge";
                case FISHING -> "Shallow Pond";
                default -> "Surface Mine";
            };
            nextMilestone = switch (node.getNodeType()) {
                case LUMBER -> "Lv.100 (Birch Woods)";
                case FISHING -> "Lv.100 (River Delta)";
                default -> "Lv.100 (Shallow Cave)";
            };
        }

        ItemBuilder builder = new ItemBuilder(Material.BEACON)
            .name("§6§lProduction Node: §e" + node.getNodeType())
            .lore(
                "§7Tier Key: §f" + node.getTierKey(),
                "§7Level: §a" + node.getLevel(),
                "§7Stored Resources: §b" + totalStored + " items",
                "",
                "§7Exploration Level: §bLv." + expLvl,
                "§7Active Zone: §d" + zoneName,
                "§7Next Milestone: §8" + nextMilestone,
                "",
                "§7Exploration Progress: §f" + currentXp + "§7/§f" + requiredXp + " XP §8(" + String.format("%.1f", percent) + "%)",
                "§8[" + bar + "§8]"
            );

        if (eventStatusName != null) {
            builder.addLore("", eventStatusName, eventStatusCycles);
        }

        inventory.setItem(4, builder.build());

        // Slots 11-15: Worker Slots
        List<WorkerInstance> assignedWorkers = workerService.getNodeWorkers(node.getNodeId());
        Optional<NodeRegistry.NodeDefinition> defOpt = registryManager != null
            ? registryManager.getNodeRegistry().getNodeDefinition(node.getTierKey())
            : Optional.empty();
        int maxSlots = defOpt.map(NodeRegistry.NodeDefinition::workerSlots).orElse(1);

        for (int i = 0; i < 5; i++) {
            int slotIndex = 11 + i;
            if (i < maxSlots) {
                if (i < assignedWorkers.size()) {
                    WorkerInstance w = assignedWorkers.get(i);
                    String rarityStr = "COMMON";
                    if (registryManager != null) {
                        var templateOpt = registryManager.getWorkerRegistry().getTemplate(w.getTemplateId());
                        if (templateOpt.isPresent()) {
                            rarityStr = templateOpt.get().rarity().name();
                        }
                    }

                    inventory.setItem(slotIndex, new ItemBuilder(Material.PLAYER_HEAD)
                        .name("§e§lWorker: §a" + w.getTemplateId())
                        .lore(
                            "§7Level: §f" + w.getLevel() + " §8(" + w.getExperience() + " XP)",
                            "§7Rarity: §d" + rarityStr,
                            "§eClick to manage or reassign workers"
                        ).build());
                } else {
                    inventory.setItem(slotIndex, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                        .name("§a§l[Empty Worker Slot]")
                        .lore("§eClick to assign an available worker").build());
                }
            } else {
                inventory.setItem(slotIndex, new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                    .name("§c§l[Locked Worker Slot]")
                    .lore("§7Upgrade this production node to unlock.").build());
            }
        }

        // Slot 22: Collect Yields
        inventory.setItem(22, new ItemBuilder(Material.CHEST)
            .name("§b§lCollect Yields")
            .lore(
                "§7Claim all accumulated resources",
                "§7directly into your Virtual Warehouse.",
                "§eClick to collect now!"
            ).build());

        // Slot 26: Upgrade Node
        long upgradeCostCoins = defOpt.map(d -> d.upgradeCost().getOrDefault("coins", 0L)).orElse(1000L);
        long upgradeCostDiamonds = defOpt.map(d -> d.upgradeCost().getOrDefault("diamonds", 0L)).orElse(0L);
        String coinsName = registryManager.getPlugin().getConfig().getString("economy.currency_name", "Coins");
        String diamondsName = registryManager.getPlugin().getConfig().getString("economy.diamonds_name", "Diamonds");

        inventory.setItem(26, new ItemBuilder(Material.ANVIL)
            .name("§a§lUpgrade Production Node")
            .lore(
                "§7Current Level: §f" + node.getLevel(),
                "§7Upgrade Cost: §6" + upgradeCostCoins + " " + coinsName + " §7/ §b" + upgradeCostDiamonds + " " + diamondsName,
                "§eClick to attempt node upgrade!"
            ).build());

        // Slot 27: Back to Hub
        inventory.setItem(27, new ItemBuilder(Material.ARROW)
            .name("§c§lBack to Hub").build());

        // Slot 31: Back to Node List
        inventory.setItem(31, new ItemBuilder(Material.BOOK)
            .name("§e§lBack to Node List").build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot == 27) {
            // Back to Hub
            player.openInventory(new MainHubGUI(player, territoryService, nodeService, workerService, storageService, economyService, onboardingService, registryManager).getInventory());
            return;
        }

        if (slot == 31) {
            // Back to Node Selection
            player.openInventory(new NodeSelectionGUI(player, NodeSelectionGUI.Mode.OVERVIEW, territoryService, nodeService, workerService, storageService, economyService, onboardingService, registryManager).getInventory());
            return;
        }

        if (slot >= 11 && slot <= 15) {
            player.openInventory(new WorkerManagementGUI(player, node, workerService, registryManager, nodeService, storageService, economyService, territoryService, onboardingService).getInventory());
            return;
        }

        if (slot == 22) {
            boolean collected = storageService.collectFromNode(player, node.getNodeId());
            if (collected) {
                populate();
            }
            return;
        }

        if (slot == 26) {
            // Upgrade Node — show confirmation
            Optional<NodeRegistry.NodeDefinition> defOpt = registryManager != null
                ? registryManager.getNodeRegistry().getNodeDefinition(node.getTierKey())
                : Optional.empty();
            if (defOpt.isEmpty()) {
                player.sendMessage("§cUpgrade info not found!");
                return;
            }

            long coinsCost = defOpt.get().upgradeCost().getOrDefault("coins", 0L);
            long diamondsCost = defOpt.get().upgradeCost().getOrDefault("diamonds", 0L);

            double coinsVal = economyService.getProfile(player.getUniqueId()).map(com.example.plugin.economy.model.PlayerProfile::getCoins).orElse(0.0);
            long coins = (long) coinsVal;
            long diamonds = economyService.getProfile(player.getUniqueId()).map(com.example.plugin.economy.model.PlayerProfile::getDiamonds).orElse(0L);

            String coinsName = registryManager.getPlugin().getConfig().getString("economy.currency_name", "Coins");
            String diamondsName = registryManager.getPlugin().getConfig().getString("economy.diamonds_name", "Diamonds");

            if (coins < coinsCost) {
                player.sendMessage("§cYou need " + coinsCost + " " + coinsName.toLowerCase() + " to upgrade! You currently have " + coins + ".");
                return;
            }
            if (diamonds < diamondsCost) {
                player.sendMessage("§cYou need " + diamondsCost + " " + diamondsName.toLowerCase() + " to upgrade! You currently have " + diamonds + ".");
                return;
            }

            player.openInventory(new ConfirmationGUI(
                player,
                "Upgrade Node",
                List.of(
                    "§7Node: §e" + node.getNodeType().name() + " (Lv." + node.getLevel() + ")",
                    "§7Cost: §6" + coinsCost + " " + coinsName + " §7/ §b" + diamondsCost + " " + diamondsName,
                    "",
                    "§7This will upgrade the node to the next level."
                ),
                () -> {
                    boolean upgraded = nodeService.upgradeNode(player, node.getNodeId());
                    if (upgraded) {
                        // Re-fetch the node to get updated level
                        nodeService.getNode(node.getNodeId()).ifPresentOrElse(
                            updatedNode -> player.openInventory(new NodeOverviewGUI(player, updatedNode, nodeService, workerService, storageService, economyService, registryManager, territoryService, onboardingService).getInventory()),
                            () -> player.openInventory(new MainHubGUI(player, territoryService, nodeService, workerService, storageService, economyService, onboardingService, registryManager).getInventory())
                        );
                    } else {
                        player.openInventory(new NodeOverviewGUI(player, node, nodeService, workerService, storageService, economyService, registryManager, territoryService, onboardingService).getInventory());
                    }
                },
                () -> player.openInventory(new NodeOverviewGUI(player, node, nodeService, workerService, storageService, economyService, registryManager, territoryService, onboardingService).getInventory())
            ).getInventory());
        }
    }

    private String getProgressBar(long current, long max) {
        int totalBars = 10;
        int filledBars = max > 0 ? (int) Math.min(totalBars, (current * totalBars) / max) : 0;
        StringBuilder sb = new StringBuilder("§a");
        for (int i = 0; i < filledBars; i++) {
            sb.append("■");
        }
        sb.append("§7");
        for (int i = filledBars; i < totalBars; i++) {
            sb.append("■");
        }
        return sb.toString();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
