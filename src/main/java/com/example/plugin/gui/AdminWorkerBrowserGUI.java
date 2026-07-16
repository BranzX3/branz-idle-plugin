package com.example.plugin.gui;

import com.example.plugin.bootstrap.BranzIdlePlugin;
import com.example.plugin.config.ResourceRegistry;
import com.example.plugin.config.WorkerRegistry;
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
 * Read-only admin browser (54 slots) for browsing all worker templates.
 * Supports rarity filtering and pagination.
 */
public class AdminWorkerBrowserGUI implements InventoryProvider {

    private final Player player;
    private final Inventory inventory;
    private ResourceRegistry.Rarity filterRarity; // null = ALL
    private int page = 0;
    private static final int ITEMS_PER_PAGE = 28;

    public AdminWorkerBrowserGUI(Player player) {
        this.player = player;
        this.filterRarity = null;
        this.inventory = Bukkit.createInventory(this, 54, "§8Admin: §eWorker Browser");
        populate();
    }

    private void populate() {
        inventory.clear();

        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("§r").build();
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 45; i < 54; i++) inventory.setItem(i, border);

        // ── Row 1: Rarity Filter Buttons ──
        inventory.setItem(1, buildFilterButton("ALL", null));
        inventory.setItem(2, buildFilterButton("COMMON", ResourceRegistry.Rarity.COMMON));
        inventory.setItem(3, buildFilterButton("UNCOMMON", ResourceRegistry.Rarity.UNCOMMON));
        inventory.setItem(4, buildFilterButton("RARE", ResourceRegistry.Rarity.RARE));
        inventory.setItem(5, buildFilterButton("EPIC", ResourceRegistry.Rarity.EPIC));
        inventory.setItem(6, buildFilterButton("LEGENDARY", ResourceRegistry.Rarity.LEGENDARY));
        inventory.setItem(7, buildFilterButton("MYTHIC", ResourceRegistry.Rarity.MYTHIC));

        // Gather and filter templates
        WorkerRegistry registry = JavaPlugin.getPlugin(BranzIdlePlugin.class)
            .getServiceRegistry().getRegistryManager().getWorkerRegistry();

        List<WorkerRegistry.WorkerTemplate> templates = new ArrayList<>(registry.getAllTemplates().values());

        if (filterRarity != null) {
            templates.removeIf(t -> t.rarity() != filterRarity);
        }

        templates.sort(Comparator
            .comparing(WorkerRegistry.WorkerTemplate::rarity)
            .thenComparing(WorkerRegistry.WorkerTemplate::profession));

        // Pagination
        int totalPages = Math.max(1, (int) Math.ceil((double) templates.size() / ITEMS_PER_PAGE));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        int startIdx = page * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, templates.size());

        // ── Rows 2-4: Worker Items ──
        int slot = 9;
        for (int i = startIdx; i < endIdx; i++) {
            if (slot >= 45) break;
            WorkerRegistry.WorkerTemplate tmpl = templates.get(i);
            inventory.setItem(slot, buildWorkerItem(tmpl));
            slot++;
        }

        // ── Row 6: Navigation ──
        inventory.setItem(45, new ItemBuilder(Material.ARROW)
            .name("§c§lBack to Admin Hub").build());

        if (page > 0) {
            inventory.setItem(47, new ItemBuilder(Material.ARROW)
                .name("§e§l◄ Previous Page")
                .lore("§7Page " + page + " / " + totalPages).build());
        }

        inventory.setItem(49, new ItemBuilder(Material.PAPER)
            .name("§f§lPage " + (page + 1) + " / " + totalPages)
            .lore("§7Total Templates: §b" + templates.size(),
                   "§7Filter: §d" + (filterRarity != null ? filterRarity.name() : "ALL")).build());

        if (page < totalPages - 1) {
            inventory.setItem(51, new ItemBuilder(Material.ARROW)
                .name("§e§lNext Page ►")
                .lore("§7Page " + (page + 2) + " / " + totalPages).build());
        }

        inventory.setItem(53, new ItemBuilder(Material.BARRIER).name("§c§lClose Menu").build());
    }

    private ItemStack buildFilterButton(String label, ResourceRegistry.Rarity rarity) {
        boolean active = (filterRarity == null && rarity == null) || (filterRarity == rarity);

        Material icon = switch (label) {
            case "ALL" -> Material.NETHER_STAR;
            case "COMMON" -> Material.IRON_INGOT;
            case "UNCOMMON" -> Material.COPPER_INGOT;
            case "RARE" -> Material.GOLD_INGOT;
            case "EPIC" -> Material.DIAMOND;
            case "LEGENDARY" -> Material.EMERALD;
            case "MYTHIC" -> Material.NETHER_STAR;
            default -> Material.PAPER;
        };

        return new ItemBuilder(icon)
            .name((active ? "§a§l" : "§7") + label)
            .lore(active ? "§a§l[Selected]" : "§eClick to filter").build();
    }

    private ItemStack buildWorkerItem(WorkerRegistry.WorkerTemplate tmpl) {
        String rarityColor = switch (tmpl.rarity()) {
            case COMMON -> "§f";
            case UNCOMMON -> "§a";
            case RARE -> "§9";
            case EPIC -> "§5";
            case LEGENDARY -> "§6";
            case MYTHIC -> "§c";
        };

        Material icon = switch (tmpl.profession()) {
            case "MINING" -> Material.IRON_PICKAXE;
            case "LUMBER" -> Material.IRON_AXE;
            case "FISHING" -> Material.FISHING_ROD;
            default -> Material.PLAYER_HEAD;
        };

        return new ItemBuilder(icon)
            .name(rarityColor + "§l" + org.bukkit.ChatColor.translateAlternateColorCodes('&', tmpl.displayName()))
            .lore(
                "§7Template: §8" + tmpl.templateId(),
                "§7Profession: §e" + tmpl.profession(),
                "§7Rarity: " + rarityColor + tmpl.rarity().name(),
                "§7Skin: §f" + tmpl.citizensSkin(),
                "",
                "§b§lBase Stats",
                "§7Speed Bonus: §a+" + String.format("%.1f%%", tmpl.speedBonus() * 100),
                "§7Yield Bonus: §a+" + String.format("%.1f%%", tmpl.yieldBonus() * 100),
                "§7Rare Drop Bonus: §d+" + String.format("%.1f%%", tmpl.rareDropBonus() * 100),
                "§7Growth/Level: §e+" + String.format("%.2f", tmpl.growthPerLevel())
            ).build();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        switch (slot) {
            case 1 -> { filterRarity = null; page = 0; populate(); }
            case 2 -> { filterRarity = ResourceRegistry.Rarity.COMMON; page = 0; populate(); }
            case 3 -> { filterRarity = ResourceRegistry.Rarity.UNCOMMON; page = 0; populate(); }
            case 4 -> { filterRarity = ResourceRegistry.Rarity.RARE; page = 0; populate(); }
            case 5 -> { filterRarity = ResourceRegistry.Rarity.EPIC; page = 0; populate(); }
            case 6 -> { filterRarity = ResourceRegistry.Rarity.LEGENDARY; page = 0; populate(); }
            case 7 -> { filterRarity = ResourceRegistry.Rarity.MYTHIC; page = 0; populate(); }
            case 45 -> player.openInventory(new AdminHubGUI(player).getInventory());
            case 47 -> { if (page > 0) { page--; populate(); } }
            case 51 -> { page++; populate(); }
            case 53 -> player.closeInventory();
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
