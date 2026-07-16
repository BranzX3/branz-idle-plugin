package com.example.plugin.gui;

import com.example.plugin.bootstrap.BranzIdlePlugin;
import com.example.plugin.gui.item.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Admin GUI (54 slots) enabling real-time editing of production engine parameters,
 * tick intervals, offline progress hours limit, and mathematical yield formula multipliers.
 */
public class AdminProductionEditorGUI implements InventoryProvider {

    private final Player player;
    private final Inventory inventory;

    public AdminProductionEditorGUI(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, "§8Admin: Production Editor");
        populate();
    }

    private void populate() {
        inventory.clear();

        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("§r").build();
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 45; i < 54; i++) inventory.setItem(i, border);

        BranzIdlePlugin plugin = JavaPlugin.getPlugin(BranzIdlePlugin.class);

        int tickerInterval = plugin.getConfig().getInt("production.ticker_interval_seconds", 5);
        int offlineMaxHours = plugin.getConfig().getInt("production.offline_max_hours", 24);
        double scaleExponent = plugin.getConfig().getDouble("production.yield_scale_exponent", 1.2);
        double scaleMultiplier = plugin.getConfig().getDouble("production.yield_scale_multiplier", 0.05);

        // Slot 4: Info card
        inventory.setItem(4, new ItemBuilder(Material.FURNACE)
            .name("§e§lProduction Engine Formula")
            .lore(
                "§7Tune formulas governing idle gathering speeds,",
                "§7offline simulation duration limits, and level scaling.",
                "",
                "§dFormula: §7Base Yield * (1.0 + Yield Bonus)",
                "§7For Common Items: §7Above Yield * (1.0 + (Level-1)^Exponent * Multiplier)"
            ).build());

        // Slot 11: Production Ticker Interval
        inventory.setItem(11, new ItemBuilder(Material.CLOCK)
            .name("§b§lTicker Interval")
            .lore(
                "§7Base frequency of idle gathering ticks.",
                "§7Current: §a" + tickerInterval + " seconds",
                "",
                "§aLeft-Click: §f+1 second     §bRight-Click: §f-1 second",
                "§2Shift-Left: §f+5 seconds    §dShift-Right: §f-5 seconds"
            ).build());

        // Slot 13: Offline Max Hours
        inventory.setItem(13, new ItemBuilder(Material.BEDROCK)
            .name("§7§lOffline Calculation Cap")
            .lore(
                "§7Maximum hours simulated when players relog.",
                "§7Current: §e" + offlineMaxHours + " hours",
                "",
                "§aLeft-Click: §f+1 hour       §bRight-Click: §f-1 hour",
                "§2Shift-Left: §f+6 hours      §dShift-Right: §f-6 hours"
            ).build());

        // Slot 15: Yield Scale Exponent
        inventory.setItem(15, new ItemBuilder(Material.REDSTONE)
            .name("§d§lYield Scale Exponent")
            .lore(
                "§7Controls progression yield curve steepness.",
                "§7Current: §a" + String.format("%.2f", scaleExponent),
                "",
                "§aLeft-Click: §f+0.05         §bRight-Click: §f-0.05",
                "§2Shift-Left: §f+0.20         §dShift-Right: §f-0.20"
            ).build());

        // Slot 16: Yield Scale Multiplier
        inventory.setItem(16, new ItemBuilder(Material.GLOWSTONE_DUST)
            .name("§d§lYield Scale Multiplier")
            .lore(
                "§7Scales the effect of the yield level-exponent.",
                "§7Current: §a" + String.format("%.3f", scaleMultiplier),
                "",
                "§aLeft-Click: §f+0.005        §bRight-Click: §f-0.005",
                "§2Shift-Left: §f+0.020        §dShift-Right: §f-0.020"
            ).build());

        inventory.setItem(45, new ItemBuilder(Material.ARROW)
            .name("§c§lBack to Admin Hub").build());
        inventory.setItem(49, new ItemBuilder(Material.BARRIER).name("§c§lClose Menu").build());
    }

    private void updateParam(String path, double val) {
        BranzIdlePlugin plugin = JavaPlugin.getPlugin(BranzIdlePlugin.class);
        plugin.getConfig().set(path, val);
        plugin.saveConfig();
        plugin.getServiceRegistry().getRegistryManager().reloadAll();
        populate();
    }

    private void updateParamInt(String path, int val) {
        BranzIdlePlugin plugin = JavaPlugin.getPlugin(BranzIdlePlugin.class);
        plugin.getConfig().set(path, val);
        plugin.saveConfig();
        plugin.getServiceRegistry().getRegistryManager().reloadAll();
        populate();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!player.hasPermission("branzidle.admin")) {
            player.sendMessage("§cYou do not have permission to access this menu!");
            player.closeInventory();
            return;
        }

        int slot = event.getRawSlot();
        ClickType click = event.getClick();
        BranzIdlePlugin plugin = JavaPlugin.getPlugin(BranzIdlePlugin.class);

        if (slot == 45) {
            player.openInventory(new AdminHubGUI(player).getInventory());
            return;
        }

        if (slot == 49) {
            player.closeInventory();
            return;
        }

        if (slot == 11) {
            int current = plugin.getConfig().getInt("production.ticker_interval_seconds", 5);
            int delta = 0;
            if (click == ClickType.SHIFT_LEFT) delta = 5;
            else if (click == ClickType.SHIFT_RIGHT) delta = -5;
            else if (click.isLeftClick()) delta = 1;
            else if (click.isRightClick()) delta = -1;

            if (delta != 0) {
                updateParamInt("production.ticker_interval_seconds", Math.max(1, current + delta));
            }
            return;
        }

        if (slot == 13) {
            int current = plugin.getConfig().getInt("production.offline_max_hours", 24);
            int delta = 0;
            if (click == ClickType.SHIFT_LEFT) delta = 6;
            else if (click == ClickType.SHIFT_RIGHT) delta = -6;
            else if (click.isLeftClick()) delta = 1;
            else if (click.isRightClick()) delta = -1;

            if (delta != 0) {
                updateParamInt("production.offline_max_hours", Math.max(1, current + delta));
            }
            return;
        }

        if (slot == 15) {
            double current = plugin.getConfig().getDouble("production.yield_scale_exponent", 1.2);
            double delta = 0.0;
            if (click == ClickType.SHIFT_LEFT) delta = 0.20;
            else if (click == ClickType.SHIFT_RIGHT) delta = -0.20;
            else if (click.isLeftClick()) delta = 0.05;
            else if (click.isRightClick()) delta = -0.05;

            if (delta != 0.0) {
                updateParam("production.yield_scale_exponent", Math.max(0.1, current + delta));
            }
            return;
        }

        if (slot == 16) {
            double current = plugin.getConfig().getDouble("production.yield_scale_multiplier", 0.05);
            double delta = 0.0;
            if (click == ClickType.SHIFT_LEFT) delta = 0.020;
            else if (click == ClickType.SHIFT_RIGHT) delta = -0.020;
            else if (click.isLeftClick()) delta = 0.005;
            else if (click.isRightClick()) delta = -0.005;

            if (delta != 0.0) {
                updateParam("production.yield_scale_multiplier", Math.max(0.001, current + delta));
            }
            return;
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
