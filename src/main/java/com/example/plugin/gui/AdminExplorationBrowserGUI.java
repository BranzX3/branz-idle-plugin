package com.example.plugin.gui;

import com.example.plugin.bootstrap.BranzIdlePlugin;
import com.example.plugin.config.DropTableRegistry;
import com.example.plugin.exploration.service.ExplorationService;
import com.example.plugin.gui.item.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Read-only admin browser (54 slots) showing exploration progression metrics,
 * required XP scales, and unlocking levels for drop pools.
 */
public class AdminExplorationBrowserGUI implements InventoryProvider {

    private final Player player;
    private final Inventory inventory;

    public AdminExplorationBrowserGUI(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, "§8Admin: §3Exploration Config");
        populate();
    }

    private void populate() {
        inventory.clear();

        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("§r").build();
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 45; i < 54; i++) inventory.setItem(i, border);

        BranzIdlePlugin plugin = JavaPlugin.getPlugin(BranzIdlePlugin.class);
        ExplorationService explorationService = plugin.getServiceRegistry().getRequiredService(ExplorationService.class);

        int maxLevel = explorationService.getMaxLevel();
        int baseXP = plugin.getConfig().getInt("exploration.base_xp", 100);
        double exponent = plugin.getConfig().getDouble("exploration.xp_exponent", 1.5);

        // ── Slot 4: Progression Summary Card ──
        inventory.setItem(4, new ItemBuilder(Material.COMPASS)
            .name("§3§lExploration Progression Config")
            .lore(
                "§7Main configuration settings from §econfig.yml§7.",
                "",
                "§7Max Exploration Level: §bLv. " + maxLevel,
                "§7Base Level-Up XP: §e" + baseXP,
                "§7XP Exponent: §a" + String.format("%.2f", exponent),
                "",
                "§dFormula: §7Base XP * (Level ^ Exponent)"
            ).build());

        // ── Slots 10-16: XP Scale Sample Points ──
        int[] sampleLevels = { 1, 10, 50, 100, 300, 600, 900 };
        int sampleSlot = 10;
        for (int lvl : sampleLevels) {
            if (lvl > maxLevel) continue;
            long xpNeeded = explorationService.getRequiredExperience(lvl);
            inventory.setItem(sampleSlot, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .name("§a§lRequired XP at Lv. " + lvl)
                .lore(
                    "§7To progress to Lv. " + (lvl + 1) + ":",
                    "§e" + String.format("%,d XP", xpNeeded)
                ).build());
            sampleSlot++;
        }

        // ── Slots 27-44: Drop Table Level Unlocks ──
        List<DropTableRegistry.DropTableDefinition> tables = new ArrayList<>(
            plugin.getServiceRegistry().getRegistryManager().getDropTableRegistry().getAllDropTables().values()
        );
        tables.sort(Comparator.comparingInt(DropTableRegistry.DropTableDefinition::minExplorationLevel));

        int tableSlot = 18;
        for (DropTableRegistry.DropTableDefinition table : tables) {
            if (tableSlot >= 45) break;

            Material icon = switch (table.tableKey().split("_")[0]) {
                case "mining" -> Material.IRON_ORE;
                case "lumber" -> Material.OAK_LOG;
                case "fishing" -> Material.COD;
                default -> Material.PAPER;
            };

            inventory.setItem(tableSlot, new ItemBuilder(icon)
                .name("§b§l" + table.tableKey().replace("_", " ").toUpperCase())
                .lore(
                    "§7Table Key: §8" + table.tableKey(),
                    "§7Min Level Required: §eLv. " + table.minExplorationLevel(),
                    "§7Contains Drop Items: §a" + table.entries().size() + " types"
                ).build());
            tableSlot++;
        }

        // ── Row 6: Navigation ──
        inventory.setItem(45, new ItemBuilder(Material.ARROW)
            .name("§c§lBack to Admin Hub").build());

        inventory.setItem(49, new ItemBuilder(Material.PAPER)
            .name("§f§lInfo")
            .lore("§7Displays the configured Level-Up XP curves", "§7and drop table unlocks.").build());

        inventory.setItem(53, new ItemBuilder(Material.BARRIER).name("§c§lClose Menu").build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 45) {
            player.openInventory(new AdminHubGUI(player).getInventory());
        } else if (slot == 53) {
            player.closeInventory();
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
