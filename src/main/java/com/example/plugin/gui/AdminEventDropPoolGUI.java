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

/**
 * Admin GUI (54 slots) enabling real-time drag-and-click addition, deletion,
 * and tuning of Event Drop Pools. Edits are saved directly to event_drops.yml.
 */
public class AdminEventDropPoolGUI implements InventoryProvider {

    private final Player player;
    private final Inventory inventory;
    private String selectedCategory = "mining"; // mining, lumber, fishing
    private final Map<Integer, String> slotToResourceMap = new HashMap<>();

    public AdminEventDropPoolGUI(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, "§8Admin: Event Drop Pools");
        populate();
    }

    private void populate() {
        inventory.clear();
        slotToResourceMap.clear();

        // Gray pane borders
        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("§r").build();
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 45; i < 54; i++) inventory.setItem(i, border);

        // Category selectors
        inventory.setItem(2, new ItemBuilder(selectedCategory.equals("mining") ? Material.GOLDEN_PICKAXE : Material.IRON_PICKAXE)
            .name("§e§lMining Events")
            .lore("§7Click to view/edit Mining event drops.", selectedCategory.equals("mining") ? "§a§l[Selected]" : "§eClick to select").build());

        inventory.setItem(4, new ItemBuilder(selectedCategory.equals("lumber") ? Material.GOLDEN_AXE : Material.IRON_AXE)
            .name("§e§lLumber Events")
            .lore("§7Click to view/edit Lumber event drops.", selectedCategory.equals("lumber") ? "§a§l[Selected]" : "§eClick to select").build());

        inventory.setItem(6, new ItemBuilder(selectedCategory.equals("fishing") ? Material.FISHING_ROD : Material.FISHING_ROD)
            .name("§e§lFishing Events")
            .lore("§7Click to view/edit Fishing event drops.", selectedCategory.equals("fishing") ? "§a§l[Selected]" : "§eClick to select").build());

        // Help instruction card in Slot 8
        inventory.setItem(8, new ItemBuilder(Material.BOOK)
            .name("§d§lDrag & Drop Instructions")
            .lore(
                "§7• §bDrag any item§7 from cursor and §bclick an empty slot§7 inside",
                "§7  the drop grid below (slots 9-44) to add it to the pool.",
                "§7• §aLeft-Click§7: +5 Weight    §7• §aShift-Left§7: +20 Weight",
                "§7• §bRight-Click§7: -5 Weight   §7• §cShift-Right§7: §c§lDelete Item"
            ).build());

        // Fill slots with drop entries
        EventDropRegistry registry = JavaPlugin.getPlugin(BranzIdlePlugin.class)
            .getServiceRegistry()
            .getRegistryManager()
            .getEventDropRegistry();

        List<EventDropRegistry.EventDropEntry> entries = registry.getEntries(selectedCategory);
        int slot = 9;

        for (EventDropRegistry.EventDropEntry entry : entries) {
            if (slot >= 45) break;

            Material mat = getMaterial(entry.resourceKey());
            String displayName = getDisplayName(entry.resourceKey());

            inventory.setItem(slot, new ItemBuilder(mat)
                .name(displayName)
                .lore(
                    "§7Resource Key: §8" + entry.resourceKey(),
                    "§7Current Weight: §b" + entry.weight(),
                    "§7Qty Range: §f" + entry.minQty() + " - " + entry.maxQty(),
                    "",
                    "§7Click to adjust weight. Shift-Right to delete."
                ).build());

            slotToResourceMap.put(slot, entry.resourceKey());
            slot++;
        }

        // Slot 45: Back to Admin Hub
        inventory.setItem(45, new ItemBuilder(Material.ARROW)
            .name("§c§lBack to Admin Hub").build());

        // Slot 49: Close
        inventory.setItem(49, new ItemBuilder(Material.BARRIER).name("§c§lClose Menu").build());
    }

    private Material getMaterial(String resKey) {
        return switch (resKey.toLowerCase()) {
            case "iron_ore" -> Material.IRON_ORE;
            case "coal" -> Material.COAL;
            case "oak_log" -> Material.OAK_LOG;
            case "mithril_crystal" -> Material.PRISMARINE_CRYSTALS;
            case "adamantite_core" -> Material.NETHERITE_SCRAP;
            case "hardwood" -> Material.OAK_WOOD;
            case "ancient_bark" -> Material.OAK_SAPLING;
            case "golden_fish" -> Material.COD;
            case "pearl" -> Material.ENDER_PEARL;
            case "sunken_relic" -> Material.HEART_OF_THE_SEA;
            default -> {
                try {
                    yield Material.valueOf(resKey.toUpperCase());
                } catch (IllegalArgumentException e) {
                    yield Material.PAPER;
                }
            }
        };
    }

    private String getDisplayName(String resKey) {
        return switch (resKey.toLowerCase()) {
            case "iron_ore" -> "§fIron Ore";
            case "coal" -> "§8Coal";
            case "oak_log" -> "§6Oak Log";
            case "mithril_crystal" -> "§b§lMithril Crystal";
            case "adamantite_core" -> "§c§lAdamantite Core";
            case "hardwood" -> "§6Hardwood";
            case "ancient_bark" -> "§aAncient Bark";
            case "golden_fish" -> "§eGolden Fish";
            case "pearl" -> "§dPearl";
            case "sunken_relic" -> "§9Sunken Relic";
            default -> "§f" + resKey;
        };
    }

    private String getResourceKey(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;

        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String name = item.getItemMeta().getDisplayName();
            if (name.contains("Mithril Crystal")) return "mithril_crystal";
            if (name.contains("Adamantite Core")) return "adamantite_core";
            if (name.contains("Hardwood")) return "hardwood";
            if (name.contains("Ancient Bark")) return "ancient_bark";
            if (name.contains("Golden Fish")) return "golden_fish";
            if (name.contains("Pearl")) return "pearl";
            if (name.contains("Sunken Relic")) return "sunken_relic";
        }

        return item.getType().name().toLowerCase();
    }

    private void addEventDropToFile(String resourceKey, int amount) {
        BranzIdlePlugin plugin = JavaPlugin.getPlugin(BranzIdlePlugin.class);
        File file = new File(plugin.getDataFolder(), "event_drops.yml");
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection(selectedCategory);
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
                player.sendMessage("§aAdded §e" + resourceKey + " §ato §b" + selectedCategory + " §eevent drop pool!");
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
        ConfigurationSection section = config.getConfigurationSection(selectedCategory);
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
                    player.sendMessage("§cRemoved §e" + resourceKey + " §cfrom §b" + selectedCategory + " §cevent drop pool!");
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
        ConfigurationSection section = config.getConfigurationSection(selectedCategory);
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

        if (slot == 49) {
            player.closeInventory();
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
                    addEventDropToFile(addedKey, cursorItem.getAmount());
                }
                return;
            }

            if (resKey != null) {
                ClickType click = event.getClick();
                if (click == ClickType.SHIFT_RIGHT) {
                    removeEventDropFromFile(resKey);
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
