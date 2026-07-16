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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin editor GUI (54 slots) allowing tuning of a specific Drop Table's weights.
 * Saves modified weights directly back to drop_tables.yml and reloads the registry.
 */
public class AdminDropTableEditorGUI implements InventoryProvider {

    private final Player player;
    private final String tableKey;
    private final Inventory inventory;
    private final Map<Integer, String> slotToResourceMap = new HashMap<>();

    public AdminDropTableEditorGUI(Player player, String tableKey) {
        this.player = player;
        this.tableKey = tableKey;
        this.inventory = Bukkit.createInventory(this, 54, "§8Admin: §eTuning " + tableKey);
        populate();
    }

    private void populate() {
        inventory.clear();
        slotToResourceMap.clear();

        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("§r").build();
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 45; i < 54; i++) inventory.setItem(i, border);

        DropTableRegistry.DropTableDefinition def = JavaPlugin.getPlugin(BranzIdlePlugin.class)
            .getServiceRegistry()
            .getRegistryManager()
            .getDropTableRegistry()
            .getDropTable(tableKey)
            .orElse(null);

        if (def != null) {
            inventory.setItem(2, new ItemBuilder(Material.COMPASS)
                .name("§b§lMin Exploration Level")
                .lore(
                    "§7Current Requirement: §eLv. " + def.minExplorationLevel(),
                    "",
                    "§aLeft-Click: §f+1 Level    §bRight-Click: §f-1 Level",
                    "§2Shift-Left: §f+10 Levels  §dShift-Right: §f-10 Levels"
                ).build());
        }

        inventory.setItem(4, new ItemBuilder(Material.BEACON)
            .name("§6§lTuning: §e" + tableKey)
            .lore(
                "§7• §bDrag any item§7 from cursor and §bclick an empty slot§7 inside",
                "§7  the drop grid below (slots 9-44) to add it to the pool.",
                "§7• §aLeft-Click§7: +5 Weight    §7• §aShift-Left§7: +20 Weight",
                "§7• §bRight-Click§7: -5 Weight   §7• §dPress Q (Drop)§7: Edit Qty Range",
                "§7• §cShift-Right§7: §c§lDelete Item"
            ).build());

        if (def != null) {
            double totalWeight = 0.0;
            for (DropTableRegistry.DropEntry entry : def.entries()) {
                totalWeight += entry.weight();
            }

            int slot = 9;
            for (DropTableRegistry.DropEntry entry : def.entries()) {
                if (slot >= 45) break;

                Material mat = getMaterial(entry.resourceKey());
                String displayName = getDisplayName(entry.resourceKey());
                double probability = totalWeight > 0 ? (entry.weight() / totalWeight) * 100.0 : 0.0;

                inventory.setItem(slot, new ItemBuilder(mat)
                    .name(displayName)
                    .lore(
                        "§7Resource Key: §8" + entry.resourceKey(),
                        "§7Current Weight: §b" + entry.weight() + " §8(" + String.format("%.2f", probability) + "%)",
                        "§7Qty Range: §f" + entry.minQty() + " - " + entry.maxQty(),
                        "",
                        "§eLeft/Right Click: §7Adjust weight (+/-5)",
                        "§aShift-Left: §7+20 Weight",
                        "§bPress Q (Drop Key): §eEdit Qty Range",
                        "§cShift-Right: §cDelete item"
                    ).build());

                slotToResourceMap.put(slot, entry.resourceKey());
                slot++;
            }
        }

        // Slot 45: Back to Table List
        inventory.setItem(45, new ItemBuilder(Material.ARROW)
            .name("§c§lBack to Drop Tables").build());

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

    private void updateMinExplorationLevel(int delta) {
        BranzIdlePlugin plugin = JavaPlugin.getPlugin(BranzIdlePlugin.class);
        File file = new File(plugin.getDataFolder(), "drop_tables.yml");
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection(tableKey);
        if (section != null) {
            int currentLevel = section.getInt("min_exploration_level", 1);
            int newLevel = Math.max(1, currentLevel + delta);
            section.set("min_exploration_level", newLevel);
            try {
                config.save(file);
                plugin.getServiceRegistry().getRegistryManager().reloadAll();
                player.sendMessage("§aUpdated min exploration level requirement: §eLv. " + newLevel);
                populate();
            } catch (IOException e) {
                player.sendMessage("§cFailed to save drop_tables.yml!");
                e.printStackTrace();
            }
        }
    }

    private void addItemToDropTable(String resourceKey, int amount) {
        BranzIdlePlugin plugin = JavaPlugin.getPlugin(BranzIdlePlugin.class);
        File file = new File(plugin.getDataFolder(), "drop_tables.yml");
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection(tableKey);
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
            entry.put("drop_weight", 50.0);
            entry.put("min_qty", 1);
            entry.put("max_qty", Math.max(1, amount));

            rawList.add(entry);
            section.set("entries", rawList);
            try {
                config.save(file);
                plugin.getServiceRegistry().getRegistryManager().reloadAll();
                player.sendMessage("§aAdded §e" + resourceKey + " §ato drop pool!");
                populate();
            } catch (IOException e) {
                player.sendMessage("§cFailed to save drop_tables.yml!");
                e.printStackTrace();
            }
        }
    }

    private void removeItemFromDropTable(String resourceKey) {
        BranzIdlePlugin plugin = JavaPlugin.getPlugin(BranzIdlePlugin.class);
        File file = new File(plugin.getDataFolder(), "drop_tables.yml");
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection(tableKey);
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
                    player.sendMessage("§cRemoved §e" + resourceKey + " §cfrom drop pool!");
                    populate();
                } catch (IOException e) {
                    player.sendMessage("§cFailed to save drop_tables.yml!");
                    e.printStackTrace();
                }
            }
        }
    }

    private void updateWeightInFile(String resourceKey, double delta) {
        BranzIdlePlugin plugin = JavaPlugin.getPlugin(BranzIdlePlugin.class);
        File file = new File(plugin.getDataFolder(), "drop_tables.yml");
        if (!file.exists()) {
            player.sendMessage("§cError: drop_tables.yml does not exist!");
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection(tableKey);
        if (section != null) {
            List<Map<?, ?>> rawList = section.getMapList("entries");
            boolean found = false;

            for (Map<?, ?> rawMap : rawList) {
                if (resourceKey.equals(rawMap.get("resource_key"))) {
                    double currentWeight = 100.0;
                    // Read drop_weight first, then weight
                    Object weightVal = rawMap.get("drop_weight");
                    if (weightVal == null) weightVal = rawMap.get("weight");
                    if (weightVal instanceof Number) {
                        currentWeight = ((Number) weightVal).doubleValue();
                    }
                    double newWeight = Math.max(1.0, currentWeight + delta);

                    Map<Object, Object> updatedMap = new HashMap<>(rawMap);
                    updatedMap.remove("weight"); // Clean up old key if exists
                    updatedMap.put("drop_weight", newWeight);

                    int idx = rawList.indexOf(rawMap);
                    rawList.set(idx, updatedMap);
                    found = true;
                    player.sendMessage("§aUpdated §e" + resourceKey + " §adrop_weight: §b" + currentWeight + " §a-> §b" + newWeight);
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
                    player.sendMessage("§cFailed to save configuration changes to disk!");
                    e.printStackTrace();
                }
            }
        }
    }

    private void editQuantityRange(String resourceKey) {
        player.closeInventory();
        player.sendMessage("§d§l[QUANTITY RANGE] §eType min and max quantity (e.g., `5-15` or `10 20`), or type `cancel`.");

        org.bukkit.event.Listener listener = new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onChat(org.bukkit.event.player.AsyncPlayerChatEvent e) {
                if (!e.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
                e.setCancelled(true);

                String msg = e.getMessage().trim().toLowerCase();
                if (msg.equals("cancel")) {
                    player.sendMessage("§cCancelled quantity range edit.");
                    org.bukkit.event.HandlerList.unregisterAll(this);
                    openEditorLater();
                    return;
                }

                String[] parts = msg.split("[- ]+");
                if (parts.length != 2) {
                    player.sendMessage("§cInvalid format! Use format like `5-15` or `10 20`.");
                    return;
                }

                try {
                    long min = Long.parseLong(parts[0]);
                    long max = Long.parseLong(parts[1]);

                    if (min < 0 || max < min) {
                        player.sendMessage("§cInvalid range! Min must be >= 0 and Max >= Min.");
                        return;
                    }

                    org.bukkit.event.HandlerList.unregisterAll(this);
                    Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(BranzIdlePlugin.class), () -> {
                        updateQuantityRangeInFile(resourceKey, min, max);
                    });
                } catch (NumberFormatException ex) {
                    player.sendMessage("§cInvalid numbers! Please input valid integers.");
                }
            }
        };

        Bukkit.getPluginManager().registerEvents(listener, JavaPlugin.getPlugin(BranzIdlePlugin.class));
    }

    private void updateQuantityRangeInFile(String resourceKey, long min, long max) {
        BranzIdlePlugin plugin = JavaPlugin.getPlugin(BranzIdlePlugin.class);
        File file = new File(plugin.getDataFolder(), "drop_tables.yml");
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection(tableKey);
        if (section != null) {
            List<Map<?, ?>> rawList = section.getMapList("entries");
            boolean found = false;

            for (Map<?, ?> rawMap : rawList) {
                if (resourceKey.equals(rawMap.get("resource_key"))) {
                    Map<Object, Object> updatedMap = new HashMap<>(rawMap);
                    updatedMap.put("min_qty", min);
                    updatedMap.put("max_qty", max);

                    int idx = rawList.indexOf(rawMap);
                    rawList.set(idx, updatedMap);
                    found = true;
                    player.sendMessage("§aUpdated §e" + resourceKey + " §aqty range to §b" + min + " - " + max);
                    break;
                }
            }

            if (found) {
                section.set("entries", rawList);
                try {
                    config.save(file);
                    plugin.getServiceRegistry().getRegistryManager().reloadAll();
                } catch (IOException e) {
                    player.sendMessage("§cFailed to save drop_tables.yml!");
                    e.printStackTrace();
                }
            }
        }
        player.openInventory(getInventory());
    }

    private void openEditorLater() {
        Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(BranzIdlePlugin.class), () -> {
            player.openInventory(getInventory());
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

        if (slot == 45) {
            player.openInventory(new AdminDropTableSelectorGUI(player).getInventory());
            return;
        }

        if (slot == 49) {
            player.closeInventory();
            return;
        }

        ClickType click = event.getClick();

        if (slot == 2) {
            int delta = 0;
            if (click == ClickType.SHIFT_LEFT) delta = 10;
            else if (click == ClickType.SHIFT_RIGHT) delta = -10;
            else if (click.isLeftClick()) delta = 1;
            else if (click.isRightClick()) delta = -1;

            if (delta != 0) {
                updateMinExplorationLevel(delta);
            }
            return;
        }

        // Drag & click item addition (slots 9 to 44)
        if (slot >= 9 && slot < 45) {
            ItemStack cursorItem = event.getCursor();
            String resKey = slotToResourceMap.get(slot);

            if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                event.setCancelled(true);
                String addedKey = getResourceKey(cursorItem);
                if (addedKey != null) {
                    addItemToDropTable(addedKey, cursorItem.getAmount());
                }
                return;
            }

            if (resKey != null) {
                if (click == ClickType.SHIFT_RIGHT) {
                    removeItemFromDropTable(resKey);
                } else if (click == ClickType.DROP) {
                    editQuantityRange(resKey);
                } else {
                    double delta = 0.0;

                    if (click == ClickType.SHIFT_LEFT) {
                        delta = 20.0;
                    } else if (click.isLeftClick()) {
                        delta = 5.0;
                    } else if (click.isRightClick()) {
                        delta = -5.0;
                    }

                    if (delta != 0.0) {
                        updateWeightInFile(resKey, delta);
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
