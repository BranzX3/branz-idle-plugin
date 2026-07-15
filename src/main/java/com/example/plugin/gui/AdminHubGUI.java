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
 * Main Hub GUI (54 slots) for administrators and server staff.
 * Restricts access to players with 'branzidle.admin' permission.
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

        inventory.setItem(4, new ItemBuilder(Material.COMMAND_BLOCK)
            .name("§c§lAdministrator Settings")
            .lore(
                "§7Quick access to game parameters, reload commands,",
                "§7and live player account modifications."
            ).build());

        // Slot 11: Reload configuration
        inventory.setItem(11, new ItemBuilder(Material.REDSTONE_LAMP)
            .name("§a§lReload Configurations")
            .lore(
                "§7Reload all registry configuration files",
                "§7and clear active visual NPC skins.",
                "",
                "§eClick to trigger reload!"
            ).build());

        // Slot 13: Manage online players
        inventory.setItem(13, new ItemBuilder(Material.PLAYER_HEAD)
            .name("§e§lPlayer Management")
            .lore(
                "§7View and edit online player profiles,",
                "§7grant coins/gems, spawn workers, or reset bases.",
                "",
                "§eClick to select a player!"
            ).build());

        // Slot 15: Manage drop tables
        inventory.setItem(15, new ItemBuilder(Material.BEACON)
            .name("§b§lDrop Table Tuning")
            .lore(
                "§7Directly adjust resource drop table weights.",
                "§7Edits are saved directly to drop_tables.yml.",
                "",
                "§eClick to tune drop rates!"
            ).build());

        // Slot 16: Manage event drop pools
        inventory.setItem(16, new ItemBuilder(Material.GOLDEN_APPLE)
            .name("§d§lEvent Drop Pools")
            .lore(
                "§7Directly adjust event drop pools using drag-and-click.",
                "§7Edits are saved directly to event_drops.yml.",
                "",
                "§eClick to tune event drops!"
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
        switch (slot) {
            case 11 -> {
                // Reload configuration
                player.sendMessage("§eReloading Branz.Idle configuration registries and visuals...");
                BranzIdlePlugin plugin = JavaPlugin.getPlugin(BranzIdlePlugin.class);
                plugin.reloadConfig();
                plugin.getServiceRegistry().getRegistryManager().reloadAll();
                plugin.getServiceRegistry().getRequiredService(VisualService.class).clearAllVisuals();
                player.sendMessage("§aBranz.Idle registries and visuals reloaded successfully!");
            }
            case 13 -> {
                // Open Player selector
                player.openInventory(new AdminPlayerSelectorGUI(player).getInventory());
            }
            case 15 -> {
                // Open Drop Table selector
                player.openInventory(new AdminDropTableSelectorGUI(player).getInventory());
            }
            case 16 -> {
                // Open Event List GUI
                player.openInventory(new AdminEventListGUI(player, "mining").getInventory());
            }
            case 49 -> player.closeInventory();
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
