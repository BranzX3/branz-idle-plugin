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

        inventory.setItem(4, new ItemBuilder(Material.BEACON)
            .name("§6§lTuning: §e" + tableKey)
            .lore(
                "§7Click resources below to tune their weights:",
                "§aLeft-Click: §f+5 Weight   §bRight-Click: §f-5 Weight",
                "§2Shift-Left: §f+20 Weight  §dShift-Right: §f-20 Weight"
            ).build());

        DropTableRegistry.DropTableDefinition def = JavaPlugin.getPlugin(BranzIdlePlugin.class)
            .getServiceRegistry()
            .getRegistryManager()
            .getDropTableRegistry()
            .getDropTable(tableKey)
            .orElse(null);

        if (def != null) {
            int slot = 9;
            for (DropTableRegistry.DropEntry entry : def.entries()) {
                if (slot >= 45) break;

                Material mat = switch (entry.resourceKey().toLowerCase()) {
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
                    default -> Material.PAPER;
                };

                inventory.setItem(slot, new ItemBuilder(mat)
                    .name("§e§l" + entry.resourceKey())
                    .lore(
                        "§7Current Weight: §b" + entry.weight(),
                        "§7Qty Range: §f" + entry.minQty() + " - " + entry.maxQty(),
                        "",
                        "§7§oClick to adjust drop probability weight."
                    ).build());

                slotToResourceMap.put(slot, entry.resourceKey());
                slot++;
            }
        }

        // Slot 45: Back to Table List
        inventory.setItem(45, new ItemBuilder(Material.ARROW)
            .name("§c§lBack to Drop Tables").build());
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
                    // Force registry to reload atomically
                    plugin.getServiceRegistry().getRegistryManager().reloadAll();
                    populate();
                } catch (IOException e) {
                    player.sendMessage("§cFailed to save configuration changes to disk!");
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
            player.openInventory(new AdminDropTableSelectorGUI(player).getInventory());
            return;
        }

        String resKey = slotToResourceMap.get(slot);
        if (resKey != null) {
            ClickType click = event.getClick();
            double delta = 0.0;

            if (click == ClickType.SHIFT_LEFT) {
                delta = 20.0;
            } else if (click == ClickType.SHIFT_RIGHT) {
                delta = -20.0;
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

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
