package com.example.plugin.gui;

import com.example.plugin.bootstrap.BranzIdlePlugin;
import com.example.plugin.config.EventDropRegistry;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Admin GUI (54 slots) enabling real-time editing of a specific event's parameters
 * (Exploration level, chance, duration) and its custom drop pool (Drag-and-Click).
 */
public class AdminEventEditorGUI implements InventoryProvider {

    private final Player player;
    private final String category;
    private final String eventKey;
    private final Inventory inventory;
    private final Map<Integer, String> slotToResourceMap = new HashMap<>();

    public AdminEventEditorGUI(Player player, String category, String eventKey) {
        this.player = player;
        this.category = category;
        this.eventKey = eventKey;
        this.inventory = Bukkit.createInventory(this, 54, "§8Event Editor: " + eventKey);
        populate();
    }

    private void populate() {
        inventory.clear();
        slotToResourceMap.clear();

        // Gray pane borders
        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("§r").build();
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 45; i < 54; i++) inventory.setItem(i, border);

        EventDropRegistry registry = JavaPlugin.getPlugin(BranzIdlePlugin.class)
            .getServiceRegistry()
            .getRegistryManager()
            .getEventDropRegistry();

        Optional<EventDropRegistry.EventDefinition> eventOpt = registry.getEvent(category, eventKey);
        if (eventOpt.isPresent()) {
            EventDropRegistry.EventDefinition eventDef = eventOpt.get();

            // Header information card
            inventory.setItem(4, new ItemBuilder(Material.WRITABLE_BOOK)
                .name("§d§lEvent: " + org.bukkit.ChatColor.translateAlternateColorCodes('&', eventDef.displayName()))
                .lore("§7Key: §8" + eventDef.eventKey(), "§7Category: §e" + category.toUpperCase()).build());

            // Slot 11: Edit Min Exploration Level
            inventory.setItem(11, new ItemBuilder(Material.COMPASS)
                .name("§e§lMinimum Exploration Level")
                .lore(
                    "§7Current Level: §bLv." + eventDef.minExplorationLevel(),
                    "",
                    "§aLeft-Click: §f+1 Level    §bRight-Click: §f-1 Level",
                    "§2Shift-Left: §f+10 Levels  §dShift-Right: §f-10 Levels"
                ).build());

            // Slot 12: Edit Max Exploration Level
            inventory.setItem(12, new ItemBuilder(Material.COMPASS)
                .name("§e§lMaximum Exploration Level")
                .lore(
                    "§7Current Level: §bLv." + eventDef.maxExplorationLevel(),
                    "",
                    "§aLeft-Click: §f+1 Level    §bRight-Click: §f-1 Level",
                    "§2Shift-Left: §f+10 Levels  §dShift-Right: §f-10 Levels"
                ).build());

            // Slot 13: Edit Trigger Chance
            inventory.setItem(13, new ItemBuilder(Material.CLOCK)
                .name("§e§lEvent Trigger Chance")
                .lore(
                    "§7Current Chance: §a" + String.format("%.1f", eventDef.eventChance() * 100.0) + "%",
                    "",
                    "§aLeft-Click: §f+1%          §bRight-Click: §f-1%",
                    "§2Shift-Left: §f+5%          §dShift-Right: §f-5%"
                ).build());

            // Slot 15: Edit Duration Cycles
            inventory.setItem(15, new ItemBuilder(Material.REDSTONE)
                .name("§e§lDuration Cycles")
                .lore(
                    "§7Current Duration: §f" + eventDef.durationCycles() + " cycles",
                    "",
                    "§aLeft-Click: §f+5 cycles    §bRight-Click: §f-5 cycles",
                    "§2Shift-Left: §f+20 cycles   §dShift-Right: §f-20 cycles"
                ).build());

            // Drop pool entries (slots 18-44)
            int slot = 18;
            for (EventDropRegistry.EventDropEntry entry : eventDef.entries()) {
                if (slot >= 45) break;

                Material mat = getMaterial(entry.resourceKey());
                String displayName = getDisplayName(entry.resourceKey());

                inventory.setItem(slot, new ItemBuilder(mat)
                    .name(displayName)
                    .lore(
                        "§7Resource Key: §8" + entry.resourceKey(),
                        "§7Weight: §b" + entry.weight(),
                        "§7Qty Range: §f" + entry.minQty() + " - " + entry.maxQty(),
                        "",
                        "§7Click to adjust weight. Shift-Right to delete."
                    ).build());

                slotToResourceMap.put(slot, entry.resourceKey());
                slot++;
            }
        }

        // Slot 45: Back to Event List
        inventory.setItem(45, new ItemBuilder(Material.ARROW)
            .name("§c§lBack to Event List").build());

        // Slot 49: Close
        inventory.setItem(49, new ItemBuilder(Material.BARRIER).name("§c§lClose Menu").build());
    }

    private Material getMaterial(String resKey) {
        return JavaPlugin.getPlugin(BranzIdlePlugin.class).getServiceRegistry().getRegistryManager().getResourceRegistry()
            .getResource(resKey)
            .map(com.example.plugin.config.ResourceRegistry.ResourceDefinition::material)
            .orElseGet(() -> {
                Material mat = Material.matchMaterial(resKey.toUpperCase());
                return mat != null ? mat : Material.PAPER;
            });
    }

    private String getDisplayName(String resKey) {
        return JavaPlugin.getPlugin(BranzIdlePlugin.class).getServiceRegistry().getRegistryManager().getResourceRegistry()
            .getResource(resKey)
            .map(def -> org.bukkit.ChatColor.translateAlternateColorCodes('&', def.displayName()))
            .orElseGet(() -> "§f" + resKey);
    }

    private String getResourceKey(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;

        // Fetch from custom resource mappings if available
        var registry = JavaPlugin.getPlugin(BranzIdlePlugin.class).getServiceRegistry().getRegistryManager().getResourceRegistry();
        for (var def : registry.getAllResources().values()) {
            if (def.material() == item.getType()) {
                if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                    String name = item.getItemMeta().getDisplayName();
                    if (name.contains(org.bukkit.ChatColor.translateAlternateColorCodes('&', def.displayName()))) {
                        return def.key();
                    }
                }
            }
        }

        return item.getType().name().toLowerCase();
    }

    private void updateEventParameter(String param, Object value) {
        BranzIdlePlugin plugin = JavaPlugin.getPlugin(BranzIdlePlugin.class);
        File file = new File(plugin.getDataFolder(), "event_drops.yml");
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String path = category + ".events." + eventKey + "." + param;
        config.set(path, value);
        try {
            config.save(file);
            plugin.getServiceRegistry().getRegistryManager().reloadAll();
            populate();
        } catch (IOException e) {
            player.sendMessage("§cFailed to save event_drops.yml!");
            e.printStackTrace();
        }
    }

    private void addEventDropToFile(String resourceKey, int amount) {
        BranzIdlePlugin plugin = JavaPlugin.getPlugin(BranzIdlePlugin.class);
        File file = new File(plugin.getDataFolder(), "event_drops.yml");
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection(category + ".events." + eventKey);
        if (section != null) {
            List<Map<?, ?>> rawList = section.getMapList("entries");

            for (Map<?, ?> rawMap : rawList) {
                if (resourceKey.equals(rawMap.get("resource_key"))) {
                    player.sendMessage("§cThis item is already in the drop pool!");
                    return;
                }
            }

            Map<Object, Object> entry = new HashMap<>();
            entry.put("resource_key", resourceKey);
            entry.put("weight", 50.0);
            entry.put("min_qty", 1);
            entry.put("max_qty", Math.max(1, amount));

            rawList.add(entry);
            section.set("entries", rawList);
            try {
                config.save(file);
                plugin.getServiceRegistry().getRegistryManager().reloadAll();
                player.sendMessage("§aAdded §e" + resourceKey + " §ato event drop pool!");
                populate();
            } catch (IOException e) {
                player.sendMessage("§cFailed to save event_drops.yml!");
                e.printStackTrace();
            }
        }
    }

    private void removeEventDropFromFile(String resourceKey) {
        BranzIdlePlugin plugin = JavaPlugin.getPlugin(BranzIdlePlugin.class);
        File file = new File(plugin.getDataFolder(), "event_drops.yml");
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection(category + ".events." + eventKey);
        if (section != null) {
            List<Map<?, ?>> rawList = section.getMapList("entries");
            boolean found = false;
            for (Map<?, ?> rawMap : rawList) {
                if (resourceKey.equals(rawMap.get("resource_key"))) {
                    rawList.remove(rawMap);
                    found = true;
                    break;
                }
            }
            if (found) {
                section.set("entries", rawList);
                try {
                    config.save(file);
                    plugin.getServiceRegistry().getRegistryManager().reloadAll();
                    player.sendMessage("§cRemoved §e" + resourceKey + " §cfrom event drop pool!");
                    populate();
                } catch (IOException e) {
                    player.sendMessage("§cFailed to save event_drops.yml!");
                    e.printStackTrace();
                }
            }
        }
    }

    private void updateEventDropWeightInFile(String resourceKey, double delta) {
        BranzIdlePlugin plugin = JavaPlugin.getPlugin(BranzIdlePlugin.class);
        File file = new File(plugin.getDataFolder(), "event_drops.yml");
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection(category + ".events." + eventKey);
        if (section != null) {
            List<Map<?, ?>> rawList = section.getMapList("entries");
            boolean found = false;
            for (Map<?, ?> rawMap : rawList) {
                if (resourceKey.equals(rawMap.get("resource_key"))) {
                    double currentWeight = 50.0;
                    Object weightVal = rawMap.get("weight");
                    if (weightVal instanceof Number) {
                        currentWeight = ((Number) weightVal).doubleValue();
                    }
                    double newWeight = Math.max(1.0, currentWeight + delta);

                    Map<Object, Object> updatedMap = new HashMap<>(rawMap);
                    updatedMap.put("weight", newWeight);

                    int idx = rawList.indexOf(rawMap);
                    rawList.set(idx, updatedMap);
                    found = true;
                    player.sendMessage("§aUpdated §e" + resourceKey + " §aweight: §b" + currentWeight + " §a-> §b" + newWeight);
                    break;
                }
            }
            if (found) {
                section.set("entries", rawList);
                try {
                    config.save(file);
                    plugin.getServiceRegistry().getRegistryManager().reloadAll();
                    populate();
                } catch (IOException e) {
                    player.sendMessage("§cFailed to save event_drops.yml!");
                    e.printStackTrace();
                }
            }
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

        if (slot == 45) {
            player.openInventory(new AdminEventListGUI(player, category).getInventory());
            return;
        }

        if (slot == 49) {
            player.closeInventory();
            return;
        }

        EventDropRegistry registry = JavaPlugin.getPlugin(BranzIdlePlugin.class)
            .getServiceRegistry()
            .getRegistryManager()
            .getEventDropRegistry();

        Optional<EventDropRegistry.EventDefinition> eventOpt = registry.getEvent(category, eventKey);
        if (eventOpt.isEmpty()) return;
        EventDropRegistry.EventDefinition eventDef = eventOpt.get();

        ClickType click = event.getClick();

        if (slot == 11) {
            int delta = 0;
            if (click == ClickType.SHIFT_LEFT) delta = 10;
            else if (click == ClickType.SHIFT_RIGHT) delta = -10;
            else if (click.isLeftClick()) delta = 1;
            else if (click.isRightClick()) delta = -1;

            if (delta != 0) {
                int newLvl = Math.max(1, eventDef.minExplorationLevel() + delta);
                updateEventParameter("min_exploration_level", newLvl);
            }
            return;
        }

        if (slot == 12) {
            int delta = 0;
            if (click == ClickType.SHIFT_LEFT) delta = 10;
            else if (click == ClickType.SHIFT_RIGHT) delta = -10;
            else if (click.isLeftClick()) delta = 1;
            else if (click.isRightClick()) delta = -1;

            if (delta != 0) {
                int newLvl = Math.max(1, eventDef.maxExplorationLevel() + delta);
                updateEventParameter("max_exploration_level", newLvl);
            }
            return;
        }

        if (slot == 13) {
            double delta = 0.0;
            if (click == ClickType.SHIFT_LEFT) delta = 0.05;
            else if (click == ClickType.SHIFT_RIGHT) delta = -0.05;
            else if (click.isLeftClick()) delta = 0.01;
            else if (click.isRightClick()) delta = -0.01;

            if (delta != 0.0) {
                double newChance = Math.max(0.0, Math.min(1.0, eventDef.eventChance() + delta));
                updateEventParameter("event_chance", newChance);
            }
            return;
        }

        if (slot == 15) {
            int delta = 0;
            if (click == ClickType.SHIFT_LEFT) delta = 20;
            else if (click == ClickType.SHIFT_RIGHT) delta = -20;
            else if (click.isLeftClick()) delta = 5;
            else if (click.isRightClick()) delta = -5;

            if (delta != 0) {
                int newDur = Math.max(1, eventDef.durationCycles() + delta);
                updateEventParameter("duration_cycles", newDur);
            }
            return;
        }

        // Drag & click item addition (slots 18 to 44)
        if (slot >= 18 && slot < 45) {
            ItemStack cursorItem = event.getCursor();
            String resKey = slotToResourceMap.get(slot);

            if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                event.setCancelled(true);
                String addedKey = getResourceKey(cursorItem);
                if (addedKey != null) {
                    addEventDropToFile(addedKey, cursorItem.getAmount());
                }
                return;
            }

            if (resKey != null) {
                if (click == ClickType.SHIFT_RIGHT) {
                    removeEventDropFromFile(resKey);
                } else {
                    double delta = 0.0;
                    if (click == ClickType.SHIFT_LEFT) delta = 20.0;
                    else if (click.isLeftClick()) delta = 5.0;
                    else if (click.isRightClick()) delta = -5.0;

                    if (delta != 0.0) {
                        updateEventDropWeightInFile(resKey, delta);
                    }
                }
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
