package com.example.plugin.gui;

import com.example.plugin.gui.item.ItemBuilder;
import com.example.plugin.node.model.ProductionNode;
import com.example.plugin.worker.model.WorkerInstance;
import com.example.plugin.worker.service.WorkerService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Interactive GUI listing available worker instances and enabling slot assignment or unassignment.
 */
public class WorkerManagementGUI implements InventoryProvider {

    private final Player player;
    private final ProductionNode node;
    private final WorkerService workerService;
    private final com.example.plugin.config.RegistryManager registryManager;
    private final Inventory inventory;
    private final Map<Integer, UUID> slotToWorkerMap = new HashMap<>();

    public WorkerManagementGUI(Player player, ProductionNode node, WorkerService workerService, com.example.plugin.config.RegistryManager registryManager) {
        this.player = player;
        this.node = node;
        this.workerService = workerService;
        this.registryManager = registryManager;
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

        for (WorkerInstance w : allWorkers) {
            if (slot >= 45) break;

            boolean isAssignedHere = node.getNodeId().equals(w.getAssignedNodeId());
            boolean isAssignedElsewhere = w.getAssignedNodeId() != null && !isAssignedHere;

            String rarityStr = "COMMON";
            if (registryManager != null) {
                var templateOpt = registryManager.getWorkerRegistry().getTemplate(w.getTemplateId());
                if (templateOpt.isPresent()) {
                    rarityStr = templateOpt.get().rarity().name();
                }
            }

            ItemBuilder builder = new ItemBuilder(Material.PLAYER_HEAD)
                .name("§e" + w.getTemplateId() + " §8(Lv." + w.getLevel() + ")")
                .lore(
                    "§7Rarity: §d" + rarityStr,
                    "§7Speed Bonus: §a+" + (w.getLevel() * 2) + "%",
                    "§7Yield Bonus: §b+" + (w.getLevel() * 3) + "%"
                );

            if (isAssignedHere) {
                builder.lore("§a§l[ASSIGNED TO THIS NODE]", "§cClick to unassign.");
            } else if (isAssignedElsewhere) {
                builder.lore("§c§l[ASSIGNED TO ANOTHER NODE]", "§7Cannot assign here unless unassigned.");
            } else {
                builder.lore("§e§l[AVAILABLE]", "§aClick to assign to this node.");
            }

            inventory.setItem(slot, builder.build());
            slotToWorkerMap.put(slot, w.getWorkerId());
            slot++;
        }

        inventory.setItem(49, new ItemBuilder(Material.BARRIER).name("§c§lBack to Node").build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
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
                    boolean success = workerService.unassignWorker(player, workerId);
                    if (success) populate();
                } else if (w.getAssignedNodeId() == null) {
                    boolean success = workerService.assignWorker(player, workerId, node.getNodeId());
                    if (success) populate();
                } else {
                    player.sendMessage("§cThis worker is already working at another node!");
                }
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
