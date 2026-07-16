package com.example.plugin.gui;

import com.example.plugin.bootstrap.BranzIdlePlugin;
import com.example.plugin.gui.item.ItemBuilder;
import com.example.plugin.visual.service.VisualService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Central Admin Hub GUI (54 slots) — Live Game Editor entry point.
 * Organises all administrative modules into a grid layout.
 */
public class AdminHubGUI implements InventoryProvider {

    private final Player player;
    private final Inventory inventory;

    public AdminHubGUI(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, "§8Branz.Idle §c§lAdmin Panel");
        populate();
    }

    private void populate() {
        inventory.clear();

        ItemStack glass = new ItemBuilder(Material.RED_STAINED_GLASS_PANE).name("§r").build();
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, glass);
        }

        // ── Title ──
        inventory.setItem(4, new ItemBuilder(Material.COMMAND_BLOCK)
            .name("§c§lBranz.Idle Admin Panel")
            .lore(
                "§7Live Game Editor — Balance your MMORPG",
                "§7without restarting the server.",
                "",
                "§8Select a module below to begin."
            ).build());

        // ── Row 2: Module Grid ──

        // Slot 10: Resources (Read-Only)
        inventory.setItem(10, new ItemBuilder(Material.BOOKSHELF)
            .name("§a§lResources")
            .lore(
                "§7Browse all resource definitions.",
                "§8Read-Only — search, filter, sort.",
                "",
                "§eClick to browse!"
            ).build());

        // Slot 11: Drop Pools (Editable)
        inventory.setItem(11, new ItemBuilder(Material.BEACON)
            .name("§b§lDrop Pools")
            .lore(
                "§7Tune exploration drop table weights.",
                "§aLive Editable — saves to YAML instantly.",
                "",
                "§eClick to tune drop rates!"
            ).build());

        // Slot 12: Event Pools (Editable)
        inventory.setItem(12, new ItemBuilder(Material.GOLDEN_APPLE)
            .name("§d§lEvent Pools")
            .lore(
                "§7Manage special event drop pools.",
                "§aLive Editable — trigger chance, duration, drops.",
                "",
                "§eClick to manage events!"
            ).build());

        // Slot 13: Gacha (Phase 2)
        inventory.setItem(13, new ItemBuilder(Material.ENDER_CHEST)
            .name("§5§lGacha")
            .lore(
                "§7Configure gacha pools and rates.",
                "§8Coming in Phase 2.",
                "",
                "§7§oNot yet available."
            ).build());

        // Slot 14: Economy (Phase 2)
        inventory.setItem(14, new ItemBuilder(Material.GOLD_INGOT)
            .name("§6§lEconomy")
            .lore(
                "§7Adjust global economy parameters.",
                "§8Coming in Phase 2.",
                "",
                "§7§oNot yet available."
            ).build());

        // Slot 15: Production (Phase 2)
        inventory.setItem(15, new ItemBuilder(Material.FURNACE)
            .name("§e§lProduction")
            .lore(
                "§7Tune production formula and tick rates.",
                "§8Coming in Phase 2.",
                "",
                "§7§oNot yet available."
            ).build());

        // ── Row 3: Module Grid ──

        // Slot 19: Exploration (Read-Only)
        inventory.setItem(19, new ItemBuilder(Material.COMPASS)
            .name("§3§lExploration")
            .lore(
                "§7View exploration config and zone progression.",
                "§8Read-Only.",
                "",
                "§eClick to browse!"
            ).build());

        // Slot 20: Workers (Read-Only)
        inventory.setItem(20, new ItemBuilder(Material.PLAYER_HEAD)
            .name("§e§lWorkers")
            .lore(
                "§7Browse all worker templates.",
                "§8Read-Only — rarity, stats, growth.",
                "",
                "§eClick to browse!"
            ).build());

        // Slot 21: Nodes (Read-Only)
        inventory.setItem(21, new ItemBuilder(Material.CHEST)
            .name("§6§lNodes")
            .lore(
                "§7Browse all node tier definitions.",
                "§8Read-Only — storage, slots, tick.",
                "",
                "§eClick to browse!"
            ).build());

        // Slot 22: Shops (Pending)
        inventory.setItem(22, new ItemBuilder(Material.EMERALD)
            .name("§a§lShops")
            .lore(
                "§7Manage NPC shops and stock.",
                "§8Pending — awaiting shop registry.",
                "",
                "§7§oNot yet available."
            ).build());

        // Slot 23: Analytics (Phase 3)
        inventory.setItem(23, new ItemBuilder(Material.SPYGLASS)
            .name("§b§lAnalytics")
            .lore(
                "§7View production, economy, and drop statistics.",
                "§8Coming in Phase 3.",
                "",
                "§7§oNot yet available."
            ).build());

        // Slot 24: Simulator (Phase 3)
        inventory.setItem(24, new ItemBuilder(Material.COMMAND_BLOCK)
            .name("§c§lSimulator")
            .lore(
                "§7Run production simulations to balance drops.",
                "§8Coming in Phase 3.",
                "",
                "§7§oNot yet available."
            ).build());

        // ── Row 5: Utility Actions ──

        // Slot 37: Reload Config
        inventory.setItem(37, new ItemBuilder(Material.REDSTONE_LAMP)
            .name("§a§lReload Configurations")
            .lore(
                "§7Reload all registry configuration files",
                "§7and clear active visual NPC skins.",
                "",
                "§eClick to trigger reload!"
            ).build());

        // Slot 39: Player Management
        inventory.setItem(39, new ItemBuilder(Material.PLAYER_HEAD)
            .name("§e§lPlayer Management")
            .lore(
                "§7View and edit online player profiles,",
                "§7grant coins/gems, spawn workers, or reset bases.",
                "",
                "§eClick to select a player!"
            ).build());

        // Slot 41: Save All
        inventory.setItem(41, new ItemBuilder(Material.WRITABLE_BOOK)
            .name("§a§lSave All")
            .lore(
                "§7Force save all in-memory caches to disk.",
                "§7Workers, nodes, storage buffers.",
                "",
                "§eClick to save!"
            ).build());

        // Slot 49: Close
        inventory.setItem(49, new ItemBuilder(Material.BARRIER).name("§c§lClose Menu").build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!player.hasPermission("branzidle.admin")) {
            player.sendMessage("§cYou do not have permission to access the admin panel!");
            player.closeInventory();
            return;
        }

        int slot = event.getRawSlot();
        BranzIdlePlugin plugin = JavaPlugin.getPlugin(BranzIdlePlugin.class);

        switch (slot) {
            // ── Module Grid ──
            case 10 -> player.openInventory(new AdminResourceBrowserGUI(player).getInventory());
            case 11 -> player.openInventory(new AdminDropTableSelectorGUI(player).getInventory());
            case 12 -> player.openInventory(new AdminEventListGUI(player, "mining").getInventory());
            case 19 -> player.openInventory(new AdminExplorationBrowserGUI(player).getInventory());
            case 20 -> player.openInventory(new AdminWorkerBrowserGUI(player).getInventory());
            case 21 -> player.openInventory(new AdminNodeBrowserGUI(player).getInventory());

            // Phase 2/3 placeholders — no-op with feedback
            case 13, 14, 15, 22, 23, 24 -> player.sendMessage("§7§oThis module is not yet available.");

            // ── Utility Actions ──
            case 37 -> {
                player.sendMessage("§eReloading Branz.Idle configuration registries and visuals...");
                plugin.reloadConfig();
                plugin.getServiceRegistry().getRegistryManager().reloadAll();
                plugin.getServiceRegistry().getRequiredService(VisualService.class).clearAllVisuals();
                player.sendMessage("§aBranz.Idle registries and visuals reloaded successfully!");
            }
            case 39 -> player.openInventory(new AdminPlayerSelectorGUI(player).getInventory());
            case 41 -> {
                player.sendMessage("§eSaving all in-memory data to disk...");
                plugin.getServiceRegistry().getWorkerService().flushAll();
                player.sendMessage("§aAll data saved successfully!");
            }
            case 49 -> player.closeInventory();
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
