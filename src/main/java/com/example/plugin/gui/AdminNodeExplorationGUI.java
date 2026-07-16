package com.example.plugin.gui;

import com.example.plugin.bootstrap.BranzIdlePlugin;
import com.example.plugin.bootstrap.ServiceRegistry;
import com.example.plugin.exploration.model.NodeExploration;
import com.example.plugin.exploration.service.ExplorationService;
import com.example.plugin.gui.item.ItemBuilder;
import com.example.plugin.node.model.ProductionNode;
import com.example.plugin.node.service.NodeService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Admin GUI (54 slots) enabling staff to tune a node's exploration mastery level.
 * Uses Left/Right and Shift click adjustments.
 */
public class AdminNodeExplorationGUI implements InventoryProvider {

    private final Player player;
    private final Player target;
    private final UUID nodeId;
    private final Inventory inventory;

    private final NodeService nodeService;
    private final ExplorationService explorationService;

    public AdminNodeExplorationGUI(Player player, Player target, UUID nodeId) {
        this.player = player;
        this.target = target;
        this.nodeId = nodeId;
        this.inventory = Bukkit.createInventory(this, 54, "§8Admin: Exploration Tuning");

        ServiceRegistry registry = JavaPlugin.getPlugin(BranzIdlePlugin.class).getServiceRegistry();
        this.nodeService = registry.getRequiredService(NodeService.class);
        this.explorationService = registry.getRequiredService(ExplorationService.class);

        populate();
    }

    private void populate() {
        inventory.clear();

        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("§r").build();
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, border);
        }

        ProductionNode node = nodeService.getNode(nodeId).orElse(null);
        if (node != null) {
            NodeExploration exp = explorationService.getExploration(nodeId);

            Material icon = switch (node.getNodeType()) {
                case MINING -> Material.IRON_PICKAXE;
                case LUMBER -> Material.IRON_AXE;
                case FISHING -> Material.FISHING_ROD;
            };

            // Slot 13: Node Info Card
            inventory.setItem(13, new ItemBuilder(icon)
                .name("§6§l" + node.getNodeType().name() + " Node")
                .lore(
                    "§7Node Level: §e" + node.getLevel(),
                    "§7Current Exploration: §bLv." + exp.getExplorationLevel(),
                    "§7XP Progress: §f" + exp.getExperience() + " XP"
                ).build());

            // Slot 22: Tuning Button
            inventory.setItem(22, new ItemBuilder(Material.COMPASS)
                .name("§e§lAdjust Exploration Level")
                .lore(
                    "§7Click this compass to edit exploration level:",
                    "§aLeft-Click: §f+1 Level    §bRight-Click: §f-1 Level",
                    "§2Shift-Left: §f+10 Levels  §dShift-Right: §f-10 Levels",
                    "",
                    "§7§oTarget player is: " + target.getName()
                ).build());
        }

        // Slot 45: Back to Node List
        inventory.setItem(45, new ItemBuilder(Material.ARROW)
            .name("§c§lBack to Node List").build());

        // Slot 49: Close
        inventory.setItem(49, new ItemBuilder(Material.BARRIER).name("§c§lClose Menu").build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!player.hasPermission("branzidle.admin")) {
            player.sendMessage("§cYou do not have permission to access this menu!");
            player.closeInventory();
            return;
        }

        int slot = event.getRawSlot();

        if (slot == 45) {
            player.openInventory(new AdminPlayerNodesGUI(player, target).getInventory());
            return;
        }

        if (slot == 49) {
            player.closeInventory();
            return;
        }

        if (slot == 22) {
            NodeExploration exp = explorationService.getExploration(nodeId);
            ClickType click = event.getClick();
            int delta = 0;

            if (click == ClickType.SHIFT_LEFT) {
                delta = 10;
            } else if (click == ClickType.SHIFT_RIGHT) {
                delta = -10;
            } else if (click.isLeftClick()) {
                delta = 1;
            } else if (click.isRightClick()) {
                delta = -1;
            }

            if (delta != 0) {
                int newLevel = Math.max(1, exp.getExplorationLevel() + delta);
                explorationService.setExplorationLevel(nodeId, newLevel);
                player.sendMessage("§aUpdated exploration level for §e" + target.getName() + "§a's node: §bLv." + newLevel);
                populate();
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
