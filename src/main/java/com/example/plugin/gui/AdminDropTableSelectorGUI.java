package com.example.plugin.gui;

import com.example.plugin.bootstrap.BranzIdlePlugin;
import com.example.plugin.config.DropTableRegistry;
import com.example.plugin.gui.item.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin selector GUI (54 slots) listing all registered Drop Tables.
 * Clicking a table opens AdminDropTableEditorGUI to tune resource drop rates.
 * Supports duplicating tables, creating new tables, and filtering by type.
 */
public class AdminDropTableSelectorGUI implements InventoryProvider {

    private final Player player;
    private final Inventory inventory;
    private final Map<Integer, String> slotToTableMap = new HashMap<>();
    private String filterType = "ALL"; // ALL, MINING, LUMBER, FISHING

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

        // ── Row 1: Filter Buttons ──
        inventory.setItem(1, buildFilterButton("ALL", "ALL", Material.NETHER_STAR));
        inventory.setItem(2, buildFilterButton("MINING", "MINING", Material.IRON_PICKAXE));
        inventory.setItem(3, buildFilterButton("LUMBER", "LUMBER", Material.IRON_AXE));
        inventory.setItem(4, buildFilterButton("FISHING", "FISHING", Material.FISHING_ROD));

        DropTableRegistry registry = JavaPlugin.getPlugin(BranzIdlePlugin.class)
            .getServiceRegistry()
            .getRegistryManager()
            .getDropTableRegistry();

        int slot = 9;
        for (DropTableRegistry.DropTableDefinition def : registry.getAllDropTables().values()) {
            if (slot >= 45) break;

            String type = def.tableKey().split("_")[0].toUpperCase();
            if (!filterType.equals("ALL") && !type.equals(filterType)) {
                continue;
            }

            Material icon = switch (type) {
                case "MINING" -> Material.IRON_PICKAXE;
                case "LUMBER" -> Material.IRON_AXE;
                case "FISHING" -> Material.FISHING_ROD;
                default -> Material.BEACON;
            };

            inventory.setItem(slot, new ItemBuilder(icon)
                .name("§e§l" + def.tableKey())
                .lore(
                    "§7Min Exploration Level: §b" + def.minExplorationLevel(),
                    "§7Entries Registered: §a" + def.entries().size(),
                    "",
                    "§eLeft-Click: §fTune drop weights",
                    "§aShift-Left Click: §bDuplicate Pool",
                    "§cShift-Right Click: §cDelete Pool"
                ).build());

            slotToTableMap.put(slot, def.tableKey());
            slot++;
        }

        // Slot 45: Back to Admin Hub
        inventory.setItem(45, new ItemBuilder(Material.ARROW)
            .name("§c§lBack to Admin Hub").build());

        // Slot 46: Create New Drop Table
        inventory.setItem(46, new ItemBuilder(Material.WRITABLE_BOOK)
            .name("§a§lCreate Drop Table")
            .lore("§7Initiates chat prompt to build a", "§7new exploration drop table.").build());

        // Slot 49: Close
        inventory.setItem(49, new ItemBuilder(Material.BARRIER).name("§c§lClose Menu").build());
    }

    private ItemStack buildFilterButton(String label, String value, Material icon) {
        boolean active = filterType.equals(value);
        return new ItemBuilder(icon)
            .name((active ? "§a§l" : "§7") + label)
            .lore(active ? "§a§l[Selected]" : "§eClick to filter").build();
    }

    private void duplicateTable(String sourceKey) {
        player.closeInventory();
        player.sendMessage("§d§l[DUPLICATE POOL] §eType the new Drop Table ID key (lowercase, e.g. `mining_lv_200`) in chat. Or type `cancel`.");

        org.bukkit.event.Listener listener = new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onChat(org.bukkit.event.player.AsyncPlayerChatEvent e) {
                if (!e.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
                e.setCancelled(true);

                String message = e.getMessage().trim().toLowerCase();
                if (message.equals("cancel")) {
                    player.sendMessage("§cDuplication cancelled.");
                    org.bukkit.event.HandlerList.unregisterAll(this);
                    openSelectorLater();
                    return;
                }

                if (!message.matches("^[a-z0-9_]+$")) {
                    player.sendMessage("§cInvalid ID! Must be alphanumeric, lowercase with underscores.");
                    return;
                }

                org.bukkit.event.HandlerList.unregisterAll(this);
                Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(BranzIdlePlugin.class), () -> {
                    performDuplication(sourceKey, message);
                });
            }
        };

        Bukkit.getPluginManager().registerEvents(listener, JavaPlugin.getPlugin(BranzIdlePlugin.class));
    }

    private void performDuplication(String sourceKey, String targetKey) {
        BranzIdlePlugin plugin = JavaPlugin.getPlugin(BranzIdlePlugin.class);
        File file = new File(plugin.getDataFolder(), "drop_tables.yml");
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (config.contains(targetKey)) {
            player.sendMessage("§cTarget Drop Table ID '" + targetKey + "' already exists!");
            player.openInventory(getInventory());
            return;
        }

        ConfigurationSection sourceSec = config.getConfigurationSection(sourceKey);
        if (sourceSec != null) {
            config.set(targetKey, sourceSec);
            try {
                config.save(file);
                plugin.getServiceRegistry().getRegistryManager().reloadAll();
                player.sendMessage("§aSuccessfully duplicated §b" + sourceKey + " §ato §e" + targetKey + "§a!");
                player.openInventory(new AdminDropTableEditorGUI(player, targetKey).getInventory());
            } catch (IOException e) {
                player.sendMessage("§cFailed to save drop_tables.yml!");
                e.printStackTrace();
                player.openInventory(getInventory());
            }
        }
    }

    private void startTableCreation() {
        player.closeInventory();
        player.sendMessage("§d§l[CREATE POOL] §eType the new Drop Table ID key (lowercase, e.g. `mining_lv_500`) in chat. Or type `cancel`.");

        org.bukkit.event.Listener listener = new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onChat(org.bukkit.event.player.AsyncPlayerChatEvent e) {
                if (!e.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
                e.setCancelled(true);

                String message = e.getMessage().trim().toLowerCase();
                if (message.equals("cancel")) {
                    player.sendMessage("§cCreation cancelled.");
                    org.bukkit.event.HandlerList.unregisterAll(this);
                    openSelectorLater();
                    return;
                }

                if (!message.matches("^[a-z0-9_]+$")) {
                    player.sendMessage("§cInvalid ID! Must be alphanumeric, lowercase with underscores.");
                    return;
                }

                org.bukkit.event.HandlerList.unregisterAll(this);
                Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(BranzIdlePlugin.class), () -> {
                    createDropTableInFile(message);
                });
            }
        };

        Bukkit.getPluginManager().registerEvents(listener, JavaPlugin.getPlugin(BranzIdlePlugin.class));
    }

    private void createDropTableInFile(String tableKey) {
        BranzIdlePlugin plugin = JavaPlugin.getPlugin(BranzIdlePlugin.class);
        File file = new File(plugin.getDataFolder(), "drop_tables.yml");
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (config.contains(tableKey)) {
            player.sendMessage("§cDrop Table '" + tableKey + "' already exists!");
            player.openInventory(getInventory());
            return;
        }

        ConfigurationSection section = config.createSection(tableKey);
        section.set("min_exploration_level", 1);
        section.set("entries", new ArrayList<>());

        try {
            config.save(file);
            plugin.getServiceRegistry().getRegistryManager().reloadAll();
            player.sendMessage("§aSuccessfully created drop table §e" + tableKey + "§a!");
            player.openInventory(new AdminDropTableEditorGUI(player, tableKey).getInventory());
        } catch (IOException e) {
            player.sendMessage("§cFailed to save drop_tables.yml!");
            e.printStackTrace();
            player.openInventory(getInventory());
        }
    }

    private void deleteTableFromFile(String tableKey) {
        BranzIdlePlugin plugin = JavaPlugin.getPlugin(BranzIdlePlugin.class);
        File file = new File(plugin.getDataFolder(), "drop_tables.yml");
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (config.contains(tableKey)) {
            config.set(tableKey, null);
            try {
                config.save(file);
                plugin.getServiceRegistry().getRegistryManager().reloadAll();
                player.sendMessage("§cDeleted drop table §e" + tableKey + "§c!");
                populate();
            } catch (IOException e) {
                player.sendMessage("§cFailed to save drop_tables.yml!");
                e.printStackTrace();
            }
        }
    }

    private void openSelectorLater() {
        Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(BranzIdlePlugin.class), () -> {
            player.openInventory(new AdminDropTableSelectorGUI(player).getInventory());
        });
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!player.hasPermission("branzidle.admin")) {
            player.sendMessage("§cYou do not have permission to access this menu!");
            player.closeInventory();
            return;
        }

        int slot = event.getRawSlot();

        switch (slot) {
            case 1 -> { filterType = "ALL"; populate(); return; }
            case 2 -> { filterType = "MINING"; populate(); return; }
            case 3 -> { filterType = "LUMBER"; populate(); return; }
            case 4 -> { filterType = "FISHING"; populate(); return; }
            case 45 -> { player.openInventory(new AdminHubGUI(player).getInventory()); return; }
            case 46 -> { startTableCreation(); return; }
            case 49 -> { player.closeInventory(); return; }
        }

        String tableKey = slotToTableMap.get(slot);
        if (tableKey != null) {
            ClickType click = event.getClick();
            if (click == ClickType.SHIFT_LEFT) {
                duplicateTable(tableKey);
            } else if (click == ClickType.SHIFT_RIGHT) {
                deleteTableFromFile(tableKey);
            } else {
                player.openInventory(new AdminDropTableEditorGUI(player, tableKey).getInventory());
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
