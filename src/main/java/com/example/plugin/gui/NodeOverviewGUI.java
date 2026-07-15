package com.example.plugin.gui;

import com.example.plugin.config.NodeRegistry;
import com.example.plugin.economy.service.EconomyService;
import com.example.plugin.gui.item.ItemBuilder;
import com.example.plugin.node.model.ProductionNode;
import com.example.plugin.node.service.NodeService;
import com.example.plugin.storage.model.NodeStorage;
import com.example.plugin.storage.service.StorageService;
import com.example.plugin.worker.model.WorkerInstance;
import com.example.plugin.worker.service.WorkerService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interactive GUI providing an overview of a production node's tier, worker slots, yield buffer, and upgrades.
 */
public class NodeOverviewGUI implements InventoryProvider {

    private final Player player;
    private final ProductionNode node;
    private final NodeService nodeService;
    private final WorkerService workerService;
    private final StorageService storageService;
    private final EconomyService economyService;
    private final com.example.plugin.config.RegistryManager registryManager;
    private final Inventory inventory;

    public NodeOverviewGUI(
        Player player,
        ProductionNode node,
        NodeService nodeService,
        WorkerService workerService,
        StorageService storageService,
        EconomyService economyService,
        com.example.plugin.config.RegistryManager registryManager
    ) {
        this.player = player;
        this.node = node;
        this.nodeService = nodeService;
        this.workerService = workerService;
        this.storageService = storageService;
        this.economyService = economyService;
        this.registryManager = registryManager;
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

        inventory.setItem(4, new ItemBuilder(Material.BEACON)
            .name("§6§lProduction Node: §e" + node.getNodeType())
            .lore(
                "§7Tier Key: §f" + node.getTierKey(),
                "§7Level: §a" + node.getLevel(),
                "§7Stored Resources: §b" + totalStored + " items"
            ).build());

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
        inventory.setItem(26, new ItemBuilder(Material.ANVIL)
            .name("§a§lUpgrade Production Node")
            .lore(
                "§7Current Level: §f" + node.getLevel(),
                "§7Upgrade Cost: §6" + upgradeCostCoins + " Coins §7/ §b" + upgradeCostDiamonds + " Diamonds",
                "§eClick to attempt node upgrade!"
            ).build());

        // Slot 31: Close
        inventory.setItem(31, new ItemBuilder(Material.BARRIER).name("§c§lClose Menu").build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 31) {
            player.closeInventory();
            return;
        }

        if (slot >= 11 && slot <= 15) {
            player.openInventory(new WorkerManagementGUI(player, node, workerService, registryManager).getInventory());
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

            if (coins < coinsCost) {
                player.sendMessage("§cYou need " + coinsCost + " coins to upgrade! You currently have " + coins + ".");
                return;
            }
            if (diamonds < diamondsCost) {
                player.sendMessage("§cYou need " + diamondsCost + " diamonds to upgrade! You currently have " + diamonds + ".");
                return;
            }

            boolean upgraded = nodeService.upgradeNode(player, node.getNodeId());
            if (upgraded) {
                populate();
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
