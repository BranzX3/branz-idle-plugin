package com.example.plugin.gui;

import com.example.plugin.analytics.service.AnalyticsService;
import com.example.plugin.bootstrap.BranzIdlePlugin;
import com.example.plugin.gui.item.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Admin GUI showcasing live server analytics (resources, coins, gems, fusion, and gacha).
 */
public class AdminAnalyticsGUI implements InventoryProvider {

    private final Player player;
    private final Inventory inventory;
    private final AnalyticsService analyticsService;

    public AdminAnalyticsGUI(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 45, "§8Admin: Live Server Analytics");
        this.analyticsService = JavaPlugin.getPlugin(BranzIdlePlugin.class).getServiceRegistry().getRequiredService(AnalyticsService.class);
        populate();
    }

    private void populate() {
        inventory.clear();

        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("§r").build();
        for (int i = 0; i < 45; i++) {
            inventory.setItem(i, border);
        }

        // Title block
        inventory.setItem(4, new ItemBuilder(Material.SPYGLASS)
            .name("§b§lLive Server Analytics")
            .lore("§7Tracks server metrics since last reload/boot.")
            .build());

        // 1. Production Stats Card (Slot 11)
        List<String> prodLore = new ArrayList<>();
        prodLore.add("§7Total resource generation counter:");
        prodLore.add("");
        Map<String, Long> stats = analyticsService.getProductionStats();
        if (stats.isEmpty()) {
            prodLore.add("§cNo resources produced yet.");
        } else {
            stats.forEach((k, v) -> prodLore.add("§7- §e" + k + ": §f" + v + " items"));
        }
        inventory.setItem(11, new ItemBuilder(Material.CHEST)
            .name("§a§lProduction Outputs")
            .lore(prodLore)
            .build());

        // 2. Economy Stats Card (Slot 13)
        inventory.setItem(13, new ItemBuilder(Material.GOLD_INGOT)
            .name("§6§lEconomy Flow (Coins)")
            .lore(
                "§7Total Coins Gained: §a" + String.format("%.2f", analyticsService.getCoinsGained()) + " Coins",
                "§7Total Coins Spent: §c" + String.format("%.2f", analyticsService.getCoinsSpent()) + " Coins",
                "§7Net Circulation Change: §e" + String.format("%.2f", analyticsService.getCoinsGained() - analyticsService.getCoinsSpent()) + " Coins"
            ).build());

        // 3. Diamonds Stats Card (Slot 15)
        inventory.setItem(15, new ItemBuilder(Material.DIAMOND)
            .name("§b§lPremium Economy (Diamonds)")
            .lore(
                "§7Total Diamonds Gained: §a" + analyticsService.getDiamondsGained() + " Diamonds",
                "§7Total Diamonds Spent: §c" + analyticsService.getDiamondsSpent() + " Diamonds",
                "§7Net Circulation: §e" + (analyticsService.getDiamondsGained() - analyticsService.getDiamondsSpent())
            ).build());

        // 4. Fusion Stats Card (Slot 29)
        inventory.setItem(29, new ItemBuilder(Material.ANVIL)
            .name("§d§lWorker Fusion Stats")
            .lore(
                "§7Total successful worker fusions: §e" + analyticsService.getFusionCount() + " fusions"
            ).build());

        // 5. Gacha Stats Card (Slot 31)
        inventory.setItem(31, new ItemBuilder(Material.ENDER_CHEST)
            .name("§5§lGacha Pull Stats")
            .lore(
                "§7Total worker gacha rolls: §e" + analyticsService.getGachaPullCount() + " pulls"
            ).build());

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

        if (slot == 36) {
            player.openInventory(new AdminHubGUI(player).getInventory());
        } else if (slot == 40) {
            player.closeInventory();
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
