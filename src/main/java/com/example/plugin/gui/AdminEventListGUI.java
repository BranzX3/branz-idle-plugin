package com.example.plugin.gui;

import com.example.plugin.bootstrap.BranzIdlePlugin;
import com.example.plugin.config.EventDropRegistry;
import com.example.plugin.gui.item.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
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
 * Admin GUI (54 slots) listing all configured events for Mining, Lumber, or Fishing.
 * Routes click to AdminEventEditorGUI or initiates Chat Conversation to create new events.
 */
public class AdminEventListGUI implements InventoryProvider {

    private final Player player;
    private final Inventory inventory;
    private String selectedCategory = "mining"; // mining, lumber, fishing
    private final Map<Integer, String> slotToEventKeyMap = new HashMap<>();

    public AdminEventListGUI(Player player, String selectedCategory) {
        this.player = player;
        if (selectedCategory != null) {
            this.selectedCategory = selectedCategory;
        }
        this.inventory = Bukkit.createInventory(this, 54, "§8Admin: Special Events");
        populate();
    }

    private void populate() {
        inventory.clear();
        slotToEventKeyMap.clear();

        // Gray pane borders
        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("§r").build();
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 45; i < 54; i++) inventory.setItem(i, border);

        // Category selectors
        inventory.setItem(2, new ItemBuilder(selectedCategory.equals("mining") ? Material.GOLDEN_PICKAXE : Material.IRON_PICKAXE)
            .name("§e§lMining Events")
            .lore("§7Click to view Mining events.", selectedCategory.equals("mining") ? "§a§l[Selected]" : "§eClick to select").build());

        inventory.setItem(4, new ItemBuilder(selectedCategory.equals("lumber") ? Material.GOLDEN_AXE : Material.IRON_AXE)
            .name("§e§lLumber Events")
            .lore("§7Click to view Lumber events.", selectedCategory.equals("lumber") ? "§a§l[Selected]" : "§eClick to select").build());

        inventory.setItem(6, new ItemBuilder(selectedCategory.equals("fishing") ? Material.FISHING_ROD : Material.FISHING_ROD)
            .name("§e§lFishing Events")
            .lore("§7Click to view Fishing events.", selectedCategory.equals("fishing") ? "§a§l[Selected]" : "§eClick to select").build());

        // Event List
        EventDropRegistry registry = JavaPlugin.getPlugin(BranzIdlePlugin.class)
            .getServiceRegistry()
            .getRegistryManager()
            .getEventDropRegistry();

        List<EventDropRegistry.EventDefinition> events = registry.getEvents(selectedCategory);
        int slot = 9;

        Material icon = switch (selectedCategory) {
            case "mining" -> Material.IRON_ORE;
            case "lumber" -> Material.OAK_LOG;
            default -> Material.COD;
        };

        for (EventDropRegistry.EventDefinition eventDef : events) {
            if (slot >= 45) break;

            inventory.setItem(slot, new ItemBuilder(icon)
                .name("§d§l" + org.bukkit.ChatColor.translateAlternateColorCodes('&', eventDef.displayName()))
                .lore(
                    "§7Event Key: §8" + eventDef.eventKey(),
                    "§7Required Exploration: §bLv." + eventDef.minExplorationLevel(),
                    "§7Trigger Chance: §a" + String.format("%.1f", eventDef.eventChance() * 100.0) + "%",
                    "§7Duration: §f" + eventDef.durationCycles() + " cycles",
                    "§7Drop Items: §e" + eventDef.entries().size() + " types",
                    "",
                    "§aClick to configure this event!",
                    "§cShift-Right Click to delete event."
                ).build());

            slotToEventKeyMap.put(slot, eventDef.eventKey());
            slot++;
        }

        // Slot 45: Back to Admin Hub
        inventory.setItem(45, new ItemBuilder(Material.ARROW)
            .name("§c§lBack to Admin Hub").build());

        // Slot 46: Create New Event
        inventory.setItem(46, new ItemBuilder(Material.WRITABLE_BOOK)
            .name("§a§lCreate New Event")
            .lore("§7Initiates chat prompt to build a", "§7new special event in this category.").build());

        // Slot 49: Close
        inventory.setItem(49, new ItemBuilder(Material.BARRIER).name("§c§lClose Menu").build());
    }

    private void deleteEventFromFile(String eventKey) {
        BranzIdlePlugin plugin = JavaPlugin.getPlugin(BranzIdlePlugin.class);
        File file = new File(plugin.getDataFolder(), "event_drops.yml");
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection(selectedCategory + ".events");
        if (section != null && section.contains(eventKey)) {
            section.set(eventKey, null);
            try {
                config.save(file);
                plugin.getServiceRegistry().getRegistryManager().reloadAll();
                player.sendMessage("§cDeleted event §e" + eventKey + " §cfrom category §b" + selectedCategory + "§c!");
                populate();
            } catch (IOException e) {
                player.sendMessage("§cFailed to save event_drops.yml!");
                e.printStackTrace();
            }
        }
    }

    private void startChatConversation(String category) {
        player.closeInventory();
        player.sendMessage("§d§l[EVENT CREATION] §eType the new Event ID key (lowercase, alphanumeric, e.g. `coal_seam`) in chat to create it. Or type `cancel`.");

        org.bukkit.event.Listener listener = new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onChat(org.bukkit.event.player.AsyncPlayerChatEvent e) {
                if (!e.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
                e.setCancelled(true);

                String message = e.getMessage().trim().toLowerCase();
                if (message.equals("cancel")) {
                    player.sendMessage("§cEvent creation cancelled.");
                    org.bukkit.event.HandlerList.unregisterAll(this);
                    Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(BranzIdlePlugin.class), () -> {
                        player.openInventory(new AdminEventListGUI(player, category).getInventory());
                    });
                    return;
                }

                if (!message.matches("^[a-z0-9_]+$")) {
                    player.sendMessage("§cInvalid ID! Must be alphanumeric, lowercase with underscores.");
                    return;
                }

                org.bukkit.event.HandlerList.unregisterAll(this);

                Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(BranzIdlePlugin.class), () -> {
                    createEventInFile(category, message);
                });
            }
        };

        Bukkit.getPluginManager().registerEvents(listener, JavaPlugin.getPlugin(BranzIdlePlugin.class));
    }

    private void createEventInFile(String category, String eventKey) {
        BranzIdlePlugin plugin = JavaPlugin.getPlugin(BranzIdlePlugin.class);
        File file = new File(plugin.getDataFolder(), "event_drops.yml");
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection(category + ".events");
        if (section == null) {
            section = config.createSection(category + ".events");
        }

        if (section.contains(eventKey)) {
            player.sendMessage("§cAn event with ID '" + eventKey + "' already exists!");
            player.openInventory(new AdminEventListGUI(player, category).getInventory());
            return;
        }

        ConfigurationSection eventSec = section.createSection(eventKey);
        eventSec.set("display_name", "&e&l" + eventKey.replace("_", " "));
        eventSec.set("min_exploration_level", 1);
        eventSec.set("event_chance", 0.05);
        eventSec.set("duration_cycles", 50);
        eventSec.set("entries", new ArrayList<>());

        try {
            config.save(file);
            plugin.getServiceRegistry().getRegistryManager().reloadAll();
            player.sendMessage("§aSuccessfully created new event §b" + eventKey + " §ain category §e" + category + "§a!");
            player.openInventory(new AdminEventEditorGUI(player, category, eventKey).getInventory());
        } catch (IOException e) {
            player.sendMessage("§cFailed to save event_drops.yml!");
            e.printStackTrace();
            player.openInventory(new AdminEventListGUI(player, category).getInventory());
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!player.hasPermission("branzidle.admin")) {
            player.sendMessage("§cYou do not have permission to access this menu!");
            player.closeInventory();
            return;
        }

        int slot = event.getRawSlot();

        if (slot == 2) {
            selectedCategory = "mining";
            populate();
            return;
        }
        if (slot == 4) {
            selectedCategory = "lumber";
            populate();
            return;
        }
        if (slot == 6) {
            selectedCategory = "fishing";
            populate();
            return;
        }

        if (slot == 45) {
            player.openInventory(new AdminHubGUI(player).getInventory());
            return;
        }

        if (slot == 46) {
            startChatConversation(selectedCategory);
            return;
        }

        if (slot == 49) {
            player.closeInventory();
            return;
        }

        String eventKey = slotToEventKeyMap.get(slot);
        if (eventKey != null) {
            if (event.getClick() == org.bukkit.event.inventory.ClickType.SHIFT_RIGHT) {
                deleteEventFromFile(eventKey);
            } else {
                player.openInventory(new AdminEventEditorGUI(player, selectedCategory, eventKey).getInventory());
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
