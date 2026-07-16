package com.example.plugin.gui;

import com.example.plugin.bootstrap.BranzIdlePlugin;
import com.example.plugin.config.ResourceRegistry;
import com.example.plugin.gui.item.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
 * Read-only admin browser (54 slots) for browsing all resource definitions.
 * Supports category filtering and pagination.
 */
public class AdminResourceBrowserGUI implements InventoryProvider {

    private final Player player;
    private final Inventory inventory;
    private String filterCategory; // null = ALL
    private int page = 0;
    private static final int ITEMS_PER_PAGE = 28; // slots 9-17, 18-26, 27-35 (rows 2-4, minus borders)

    public AdminResourceBrowserGUI(Player player) {
        this.player = player;
        this.filterCategory = null;
        this.inventory = Bukkit.createInventory(this, 54, "§8Admin: §aResource Browser");
        populate();
    }

    private void populate() {
        inventory.clear();

        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("§r").build();
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 45; i < 54; i++) inventory.setItem(i, border);

        // ── Row 1: Filter Buttons ──
        inventory.setItem(1, buildFilterButton("ALL", null, Material.NETHER_STAR));
        inventory.setItem(2, buildFilterButton("MINING", "MINING", Material.IRON_PICKAXE));
        inventory.setItem(3, buildFilterButton("LUMBER", "LUMBER", Material.IRON_AXE));
        inventory.setItem(4, buildFilterButton("FISHING", "FISHING", Material.FISHING_ROD));
        inventory.setItem(5, buildFilterButton("FARMING", "FARMING", Material.WHEAT));
        inventory.setItem(6, buildFilterButton("TREASURE", "TREASURE", Material.CHEST));

        // Gather and filter resources
        ResourceRegistry registry = JavaPlugin.getPlugin(BranzIdlePlugin.class)
            .getServiceRegistry().getRegistryManager().getResourceRegistry();

        List<ResourceRegistry.ResourceDefinition> allResources = new ArrayList<>(registry.getAllResources().values());

        if (filterCategory != null) {
            allResources.removeIf(def -> !def.category().name().equals(filterCategory));
        }

        // Sort by tier then unlock level
        allResources.sort(Comparator
            .comparing(ResourceRegistry.ResourceDefinition::tier)
            .thenComparingInt(ResourceRegistry.ResourceDefinition::unlockLevel));

        // Pagination
        int totalPages = Math.max(1, (int) Math.ceil((double) allResources.size() / ITEMS_PER_PAGE));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        int startIdx = page * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, allResources.size());

        // ── Rows 2-4: Resource Items ──
        int slot = 9;
        for (int i = startIdx; i < endIdx; i++) {
            if (slot >= 45) break;
            // Skip border columns (slot 17, 26, 35 are end-of-row)
            ResourceRegistry.ResourceDefinition def = allResources.get(i);
            inventory.setItem(slot, buildResourceItem(def));
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
            .lore("§7Total Resources: §b" + allResources.size(),
                   "§7Filter: §e" + (filterCategory != null ? filterCategory : "ALL")).build());

        if (page < totalPages - 1) {
            inventory.setItem(51, new ItemBuilder(Material.ARROW)
                .name("§e§lNext Page ►")
                .lore("§7Page " + (page + 2) + " / " + totalPages).build());
        }

        inventory.setItem(53, new ItemBuilder(Material.BARRIER).name("§c§lClose Menu").build());
    }

    private ItemStack buildFilterButton(String label, String categoryValue, Material icon) {
        boolean active = (filterCategory == null && categoryValue == null)
            || (filterCategory != null && filterCategory.equals(categoryValue));

        return new ItemBuilder(icon)
            .name((active ? "§a§l" : "§7") + label)
            .lore(active ? "§a§l[Selected]" : "§eClick to filter").build();
    }

    private ItemStack buildResourceItem(ResourceRegistry.ResourceDefinition def) {
        String displayName = ChatColor.translateAlternateColorCodes('&', def.displayName());

        List<String> lore = new ArrayList<>();
        lore.add("§7Key: §8" + def.key());
        lore.add("§7Material: §f" + def.material().name());
        lore.add("");
        lore.add("§7Rarity: §d" + def.rarity().name());
        lore.add("§7Quality: §b" + def.quality().name());
        lore.add("§7Tier: §e" + def.tier().name());
        lore.add("§7Category: §a" + def.category().name());
        lore.add("§7Family: §f" + def.family());
        lore.add("");
        lore.add("§7Unlock Lv: §b" + def.unlockLevel());
        lore.add("§7Sell: §6" + String.format("%.1f", def.sellPrice()) + " §7Buy: §6" + String.format("%.1f", def.buyPrice()));
        lore.add("§7EXP: §a" + def.exp());
        lore.add("§7Drop Weight: §e" + def.dropWeight());
        lore.add("§7Max Stack: §f" + def.maxStack());
        lore.add("");
        lore.add("§7Type: §f" + def.resourceType().name());
        lore.add("§7Stack: §f" + def.stackType().name());
        lore.add("§7NPC Sell: " + (def.npcSell() ? "§a✔" : "§c✖") + " §7Trade: " + (def.playerTrade() ? "§a✔" : "§c✖"));

        if (!def.allowedNodes().isEmpty()) {
            lore.add("§7Nodes: §f" + String.join(", ", def.allowedNodes()));
        }
        if (!def.zones().isEmpty()) {
            lore.add("§7Zones: §f" + String.join(", ", def.zones()));
        }
        if (!def.tags().isEmpty()) {
            lore.add("§7Tags: §f" + String.join(", ", def.tags()));
        }

        lore.add("");
        lore.add("§8" + def.translationKey() + " | v" + def.introducedIn());

        return new ItemBuilder(def.material())
            .name(displayName)
            .lore(lore.toArray(new String[0])).build();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        switch (slot) {
            case 1 -> { filterCategory = null; page = 0; populate(); }
            case 2 -> { filterCategory = "MINING"; page = 0; populate(); }
            case 3 -> { filterCategory = "LUMBER"; page = 0; populate(); }
            case 4 -> { filterCategory = "FISHING"; page = 0; populate(); }
            case 5 -> { filterCategory = "FARMING"; page = 0; populate(); }
            case 6 -> { filterCategory = "TREASURE"; page = 0; populate(); }
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
