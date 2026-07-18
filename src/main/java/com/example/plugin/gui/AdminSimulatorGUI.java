package com.example.plugin.gui;

import com.example.plugin.bootstrap.BranzIdlePlugin;
import com.example.plugin.config.DropTableRegistry;
import com.example.plugin.config.NodeRegistry;
import com.example.plugin.config.RegistryManager;
import com.example.plugin.config.ResourceRegistry;
import com.example.plugin.gui.item.ItemBuilder;
import com.example.plugin.node.model.NodeType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Admin GUI allowing live drop simulation testing to balance MMORPG reward weights.
 */
public class AdminSimulatorGUI implements InventoryProvider {

    private final Player player;
    private final Inventory inventory;
    private final RegistryManager registryManager;

    // Simulation settings
    private NodeType selectedNodeType = NodeType.MINING;
    private int selectedExplorationLevel = 1;
    private int selectedCycles = 1000;

    // Simulation results
    private Map<String, Long> simulationResults = null;
    private long totalItemsSimulated = 0;

    public AdminSimulatorGUI(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 45, "§8Admin: Balance Simulator");
        this.registryManager = JavaPlugin.getPlugin(BranzIdlePlugin.class).getServiceRegistry().getRegistryManager();
        populate();
    }

    private void populate() {
        inventory.clear();

        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("§r").build();
        for (int i = 0; i < 45; i++) {
            inventory.setItem(i, border);
        }

        // Title Card (Slot 4)
        inventory.setItem(4, new ItemBuilder(Material.COMMAND_BLOCK)
            .name("§c§lMMORPG Drop Simulator")
            .lore(
                "§7Simulate node production in memory",
                "§7to verify drop tables and weight balances."
            ).build());

        // 1. Node Type Selector (Slot 10)
        Material typeIcon = switch (selectedNodeType) {
            case MINING -> Material.IRON_PICKAXE;
            case LUMBER -> Material.IRON_AXE;
            case FISHING -> Material.FISHING_ROD;
        };
        inventory.setItem(10, new ItemBuilder(typeIcon)
            .name("§e§l1. Select Node Type")
            .lore(
                "§7Current: §a" + selectedNodeType.name(),
                "",
                "§eClick to cycle NodeType!"
            ).build());

        // 2. Exploration Level (Slot 12)
        inventory.setItem(12, new ItemBuilder(Material.COMPASS)
            .name("§e§l2. Exploration Level")
            .lore(
                "§7Current Level: §bLv." + selectedExplorationLevel,
                "",
                "§aLeft-Click: §f+1 Level    §bRight-Click: §f-1 Level",
                "§2Shift-Left: §f+10 Levels  §dShift-Right: §f-10 Levels"
            ).build());

        // 3. Simulation Cycles (Slot 14)
        inventory.setItem(14, new ItemBuilder(Material.CLOCK)
            .name("§e§l3. Simulation Cycles")
            .lore(
                "§7Cycles: §b" + selectedCycles + " cycles",
                "",
                "§aLeft-Click: §f+100 Cycles  §bRight-Click: §f-100",
                "§2Shift-Left: §f+1000       §dShift-Right: §f-1000"
            ).build());

        // 4. Run Simulation (Slot 16)
        inventory.setItem(16, new ItemBuilder(Material.REDSTONE_BLOCK)
            .name("§c§l▶ RUN SIMULATION ◀")
            .lore("§7Executes simulation loop instantly.")
            .build());

        // 5. Results Card (Slot 22)
        List<String> reportLore = new ArrayList<>();
        if (simulationResults == null) {
            reportLore.add("§7No simulation has been run yet.");
            reportLore.add("§7Click §cRun Simulation §7to start.");
        } else {
            reportLore.add("§7Simulation stats for §a" + selectedNodeType.name() + " §7(Exploration: §bLv." + selectedExplorationLevel + "§7)");
            reportLore.add("§7Total cycles: §e" + selectedCycles);
            reportLore.add("§7Total items rolled: §a" + totalItemsSimulated);
            reportLore.add("");
            reportLore.add("§b§lRoll breakdown:");
            simulationResults.forEach((k, v) -> {
                double pct = totalItemsSimulated > 0 ? (v * 100.0 / totalItemsSimulated) : 0.0;
                reportLore.add(String.format("§7- §e%s: §f%d rolls §8(%.2f%%)", k, v, pct));
            });
        }
        inventory.setItem(22, new ItemBuilder(Material.PAPER)
            .name("§a§lSimulation Report Card")
            .lore(reportLore)
            .build());

        // Back to Admin Hub (Slot 36)
        inventory.setItem(36, new ItemBuilder(Material.ARROW)
            .name("§c§lBack to Admin Panel").build());

        // Close (Slot 40)
        inventory.setItem(40, new ItemBuilder(Material.BARRIER)
            .name("§c§lClose Menu").build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ClickType click = event.getClick();

        if (slot == 36) {
            player.openInventory(new AdminHubGUI(player).getInventory());
            return;
        }

        if (slot == 40) {
            player.closeInventory();
            return;
        }

        // Cycle NodeType
        if (slot == 10) {
            NodeType[] types = NodeType.values();
            int nextIdx = (selectedNodeType.ordinal() + 1) % types.length;
            selectedNodeType = types[nextIdx];
            populate();
            return;
        }

        // Adjust Level
        if (slot == 12) {
            int delta = 0;
            if (click == ClickType.SHIFT_LEFT) delta = 10;
            else if (click == ClickType.SHIFT_RIGHT) delta = -10;
            else if (click.isLeftClick()) delta = 1;
            else if (click.isRightClick()) delta = -1;

            selectedExplorationLevel = Math.max(1, Math.min(1000, selectedExplorationLevel + delta));
            populate();
            return;
        }

        // Adjust Cycles
        if (slot == 14) {
            int delta = 0;
            if (click == ClickType.SHIFT_LEFT) delta = 1000;
            else if (click == ClickType.SHIFT_RIGHT) delta = -1000;
            else if (click.isLeftClick()) delta = 100;
            else if (click.isRightClick()) delta = -100;

            selectedCycles = Math.max(100, Math.min(10000, selectedCycles + delta));
            populate();
            return;
        }

        // Run Simulation
        if (slot == 16) {
            runSimulation();
            populate();
        }
    }

    private void runSimulation() {
        Map<String, Long> stats = new LinkedHashMap<>();
        long totalItems = 0;

        // Build active tables
        List<DropTableRegistry.DropEntry> activeTables = new java.util.ArrayList<>();
        for (DropTableRegistry.DropTableDefinition tableDef : registryManager.getDropTableRegistry().getAllDropTables().values()) {
            if (tableDef.minExplorationLevel() > selectedExplorationLevel) continue;
            for (DropTableRegistry.DropEntry entry : tableDef.entries()) {
                ResourceRegistry.ResourceDefinition resDef =
                    registryManager.getResourceRegistry().getResource(entry.resourceKey()).orElse(null);
                if (resDef == null || !resDef.enabled()) continue;
                java.util.List<String> allowed = resDef.allowedNodes();
                if (!allowed.isEmpty() && !allowed.contains(selectedNodeType.name())) continue;
                activeTables.add(entry);
            }
        }

        String defaultItem = switch (selectedNodeType) {
            case MINING -> "iron_ore";
            case LUMBER -> "oak_log";
            case FISHING -> "golden_fish";
        };

        double totalWeight = 0.0;
        List<Double> weights = new ArrayList<>();
        for (DropTableRegistry.DropEntry entry : activeTables) {
            double w = entry.weight();
            weights.add(w);
            totalWeight += w;
        }

        for (int c = 0; c < selectedCycles; c++) {
            if (activeTables.isEmpty() || totalWeight <= 0.0) {
                stats.merge(defaultItem, 1L, Long::sum);
                totalItems += 1;
            } else {
                double rand = ThreadLocalRandom.current().nextDouble() * totalWeight;
                double accum = 0.0;
                DropTableRegistry.DropEntry selected = null;
                for (int i = 0; i < activeTables.size(); i++) {
                    accum += weights.get(i);
                    if (rand <= accum) {
                        selected = activeTables.get(i);
                        break;
                    }
                }
                if (selected == null) selected = activeTables.get(0);

                long qty = ThreadLocalRandom.current().nextLong(selected.minQty(), selected.maxQty() + 1);
                stats.merge(selected.resourceKey(), qty, Long::sum);
                totalItems += qty;
            }
        }

        this.simulationResults = stats;
        this.totalItemsSimulated = totalItems;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
