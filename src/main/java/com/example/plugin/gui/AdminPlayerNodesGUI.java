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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin GUI (54 slots) showing all production nodes owned by a player.
 * Selecting a node opens AdminNodeExplorationGUI to edit exploration level.
 */
public class AdminPlayerNodesGUI implements InventoryProvider {

    private final Player player;
    private final Player target;
    private final Inventory inventory;
    private final Map<Integer, UUID> slotToNodeMap = new HashMap<>();

    private final NodeService nodeService;
    private final ExplorationService explorationService;

    public AdminPlayerNodesGUI(Player player, Player target) {
        this.player = player;
        this.target = target;
        this.inventory = Bukkit.createInventory(this, 54, "§8Admin: " + target.getName() + "'s Nodes");

        ServiceRegistry registry = JavaPlugin.getPlugin(BranzIdlePlugin.class).getServiceRegistry();
        this.nodeService = registry.getRequiredService(NodeService.class);
        this.explorationService = registry.getRequiredService(ExplorationService.class);

        populate();
    }

    private void populate() {
        inventory.clear();
        slotToNodeMap.clear();

        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("§r").build();
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 45; i < 54; i++) inventory.setItem(i, border);

        inventory.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
            .name("§e§l" + target.getName() + "'s Nodes")
            .lore("§7Click a production node below to edit its exploration level.").build());

        List<ProductionNode> nodes = nodeService.getPlayerNodes(target.getUniqueId());
        int slot = 9;

        for (ProductionNode node : nodes) {
            if (slot >= 45) break;

            Material icon = switch (node.getNodeType()) {
                case MINING -> Material.IRON_PICKAXE;
                case LUMBER -> Material.IRON_AXE;
                case FISHING -> Material.FISHING_ROD;
            };

            NodeExploration exp = explorationService.getExploration(node.getNodeId());

            inventory.setItem(slot, new ItemBuilder(icon)
                .name("§6§l" + node.getNodeType().name() + " Node")
                .lore(
                    "§7Level: §e" + node.getLevel(),
                    "§7Exploration Level: §bLv." + exp.getExplorationLevel(),
                    "§7Mastery XP: §f" + exp.getExperience() + " XP",
                    "",
                    "§eClick to adjust exploration level!"
                ).build());

            slotToNodeMap.put(slot, node.getNodeId());
            slot++;
        }

        // Slot 45: Back to Target Player Profile
        inventory.setItem(45, new ItemBuilder(Material.ARROW)
            .name("§c§lBack to Profile").build());
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
            player.openInventory(new AdminPlayerEditorGUI(player, target).getInventory());
            return;
        }

        UUID nodeId = slotToNodeMap.get(slot);
        if (nodeId != null) {
            player.openInventory(new AdminNodeExplorationGUI(player, target, nodeId).getInventory());
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
