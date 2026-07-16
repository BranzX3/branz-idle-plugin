package com.example.plugin.gui;

import com.example.plugin.bootstrap.BranzIdlePlugin;
import com.example.plugin.config.NodeRegistry;
import com.example.plugin.gui.item.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Read-only admin browser (54 slots) for browsing all node definitions.
 */
public class AdminNodeBrowserGUI implements InventoryProvider {

    private final Player player;
    private final Inventory inventory;

    public AdminNodeBrowserGUI(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, "§8Admin: §6Node Browser");
        populate();
    }

    private void populate() {
        inventory.clear();

        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("§r").build();
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 45; i < 54; i++) inventory.setItem(i, border);

        // Gather all node definitions
        NodeRegistry registry = JavaPlugin.getPlugin(BranzIdlePlugin.class)
            .getServiceRegistry().getRegistryManager().getNodeRegistry();

        List<NodeRegistry.NodeDefinition> definitions = new ArrayList<>(registry.getAllNodes().values());
        
        // Sort by type then tier key
        definitions.sort(Comparator
            .comparing(NodeRegistry.NodeDefinition::nodeType)
            .thenComparing(NodeRegistry.NodeDefinition::tierKey));

        int slot = 9;
        for (NodeRegistry.NodeDefinition def : definitions) {
            if (slot >= 45) break;
            inventory.setItem(slot, buildNodeItem(def));
            slot++;
        }

        // ── Navigation Buttons ──
        inventory.setItem(45, new ItemBuilder(Material.ARROW)
            .name("§c§lBack to Admin Hub").build());

        inventory.setItem(49, new ItemBuilder(Material.PAPER)
            .name("§f§lOverview")
            .lore("§7Total Node Tiers: §b" + definitions.size()).build());

        inventory.setItem(53, new ItemBuilder(Material.BARRIER).name("§c§lClose Menu").build());
    }

    private ItemStack buildNodeItem(NodeRegistry.NodeDefinition def) {
        Material material = switch (def.nodeType()) {
            case "MINING" -> Material.GOLDEN_PICKAXE;
            case "LUMBER" -> Material.GOLDEN_AXE;
            case "FISHING" -> Material.FISHING_ROD;
            default -> Material.GRASS_BLOCK;
        };

        List<String> lore = new ArrayList<>();
        lore.add("§7Tier Key: §8" + def.tierKey());
        lore.add("§7Type: §e" + def.nodeType());
        lore.add("§7Worker Slots: §a" + def.workerSlots());
        lore.add("§7Base Tick: §b" + def.baseTickSeconds() + "s");
        lore.add("§7Max Storage: §f" + def.maxStorageCapacity());
        lore.add("§7Size Chunks: §b" + def.sizeChunks() + "x" + def.sizeChunks());
        lore.add("§7Schematic: §8" + def.schematic());
        lore.add("");
        lore.add("§6§lUpgrade Cost:");

        if (def.upgradeCost().isEmpty()) {
            lore.add("§7- §aNone (Max Tier)");
        } else {
            for (Map.Entry<String, Long> cost : def.upgradeCost().entrySet()) {
                lore.add("§7- §f" + cost.getKey() + ": §e" + cost.getValue());
            }
        }

        return new ItemBuilder(material)
            .name("§e§l" + def.tierKey().replace("_", " ").toUpperCase())
            .lore(lore.toArray(new String[0])).build();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 45) {
            player.openInventory(new AdminHubGUI(player).getInventory());
        } else if (slot == 53) {
            player.closeInventory();
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
