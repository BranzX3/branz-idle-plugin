package com.example.plugin.gui;

import com.example.plugin.bootstrap.BranzIdlePlugin;
import com.example.plugin.config.DropTableRegistry;
import com.example.plugin.gui.item.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin selector GUI (54 slots) listing all registered Drop Tables.
 * Clicking a table opens AdminDropTableEditorGUI to tune resource drop rates.
 */
public class AdminDropTableSelectorGUI implements InventoryProvider {

    private final Player player;
    private final Inventory inventory;
    private final Map<Integer, String> slotToTableMap = new HashMap<>();

    public AdminDropTableSelectorGUI(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, "§8Admin: Select Drop Table");
        populate();
    }

    private void populate() {
        inventory.clear();
        slotToTableMap.clear();

        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("§r").build();
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 45; i < 54; i++) inventory.setItem(i, border);

        inventory.setItem(4, new ItemBuilder(Material.BEACON)
            .name("§6§lSelect Drop Table")
            .lore("§7Click an active drop table below to edit its item drop weights.").build());

        DropTableRegistry registry = JavaPlugin.getPlugin(BranzIdlePlugin.class)
            .getServiceRegistry()
            .getRegistryManager()
            .getDropTableRegistry();

        int slot = 9;
        for (DropTableRegistry.DropTableDefinition def : registry.getAllDropTables().values()) {
            if (slot >= 45) break;

            Material icon = switch (def.tableKey().split("_")[0].toLowerCase()) {
                case "mining" -> Material.IRON_PICKAXE;
                case "lumber" -> Material.IRON_AXE;
                case "fishing" -> Material.FISHING_ROD;
                default -> Material.BEACON;
            };

            inventory.setItem(slot, new ItemBuilder(icon)
                .name("§e§l" + def.tableKey())
                .lore(
                    "§7Min Exploration Level: §b" + def.minExplorationLevel(),
                    "§7Entries Registered: §a" + def.entries().size(),
                    "",
                    "§eClick to edit weights!"
                ).build());

            slotToTableMap.put(slot, def.tableKey());
            slot++;
        }

        // Slot 45: Back to Admin Hub
        inventory.setItem(45, new ItemBuilder(Material.ARROW)
            .name("§c§lBack to Admin Hub").build());

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
            player.openInventory(new AdminHubGUI(player).getInventory());
            return;
        }

        if (slot == 49) {
            player.closeInventory();
            return;
        }

        String tableKey = slotToTableMap.get(slot);
        if (tableKey != null) {
            player.openInventory(new AdminDropTableEditorGUI(player, tableKey).getInventory());
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
