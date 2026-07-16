package com.example.plugin.gui;

import com.example.plugin.bootstrap.BranzIdlePlugin;
import com.example.plugin.config.ResourceRegistry;
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
 * Admin GUI (54 slots) enabling real-time adjustments of global economy parameters
 * and individual resource sell/buy prices inside resource pack configs.
 */
public class AdminEconomyEditorGUI implements InventoryProvider {

    private final Player player;
    private final Inventory inventory;
    private boolean resourceSelectionMode = false;
    private int page = 0;
    private final Map<Integer, String> slotToResourceMap = new HashMap<>();
    private static final int ITEMS_PER_PAGE = 28;

    public AdminEconomyEditorGUI(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, "§8Admin: Economy Editor");
        populate();
    }

    private void populate() {
        inventory.clear();
        slotToResourceMap.clear();

        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("§r").build();
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 45; i < 54; i++) inventory.setItem(i, border);

        BranzIdlePlugin plugin = JavaPlugin.getPlugin(BranzIdlePlugin.class);

        // ── Mode 1: Global Parameters ──
        if (!resourceSelectionMode) {
            double npcPriceModifier = plugin.getConfig().getDouble("economy.npc_price_modifier", 1.0);
            double sellTax = plugin.getConfig().getDouble("economy.global_sell_tax", 0.0);

            inventory.setItem(4, new ItemBuilder(Material.GOLD_INGOT)
                .name("§6§lGlobal Economy Settings")
                .lore("§7Tune tax rates and price modifiers instantly.").build());

            // Slot 11: NPC Price Modifier
            inventory.setItem(11, new ItemBuilder(Material.EMERALD)
                .name("§e§lNPC Price Modifier")
                .lore(
                    "§7Multiplier applied to all NPC sell prices.",
                    "§7Current: §a" + String.format("%.2f", npcPriceModifier) + "x",
                    "",
                    "§aLeft-Click: §f+0.1         §bRight-Click: §f-0.1",
                    "§2Shift-Left: §f+0.5         §dShift-Right: §f-0.5"
                ).build());

            // Slot 13: Global Sell Tax
            inventory.setItem(13, new ItemBuilder(Material.REDSTONE)
                .name("§c§lGlobal Sell Tax")
                .lore(
                    "§7Tax deducted from items sold in Marketplace.",
                    "§7Current: §e" + String.format("%.1f%%", sellTax * 100),
                    "",
                    "§aLeft-Click: §f+1%          §bRight-Click: §f-1%",
                    "§2Shift-Left: §f+5%          §dShift-Right: §f-5%"
                ).build());

            // Slot 15: Edit specific resource prices selector
            inventory.setItem(15, new ItemBuilder(Material.BOOKSHELF)
                .name("§b§lEdit Resource Prices")
                .lore(
                    "§7Browse all loaded resources and tune",
                    "§7their individual sell/buy prices.",
                    "",
                    "§eClick to browse resources!"
                ).build());

            inventory.setItem(45, new ItemBuilder(Material.ARROW)
                .name("§c§lBack to Admin Hub").build());
            inventory.setItem(49, new ItemBuilder(Material.BARRIER).name("§c§lClose Menu").build());
            return;
        }

        // ── Mode 2: Resource Price Tuning Selector ──
        inventory.setItem(4, new ItemBuilder(Material.BOOKSHELF)
            .name("§b§lSelect Resource to Edit Price")
            .lore("§7Choose a resource below to edit its base sell/buy prices.").build());

        ResourceRegistry registry = plugin.getServiceRegistry().getRegistryManager().getResourceRegistry();
        List<ResourceRegistry.ResourceDefinition> resources = new ArrayList<>(registry.getAllResources().values());

        // Sort alphabetically
        resources.sort((a, b) -> a.key().compareTo(b.key()));

        int totalPages = Math.max(1, (int) Math.ceil((double) resources.size() / ITEMS_PER_PAGE));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        int startIdx = page * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, resources.size());

        int slot = 9;
        for (int i = startIdx; i < endIdx; i++) {
            if (slot >= 45) break;
            ResourceRegistry.ResourceDefinition def = resources.get(i);

            inventory.setItem(slot, new ItemBuilder(def.material())
                .name("§a§l" + org.bukkit.ChatColor.translateAlternateColorCodes('&', def.displayName()))
                .lore(
                    "§7Key: §8" + def.key(),
                    "§7Sell Price: §6" + def.sellPrice() + " Coins",
                    "§7Buy Price: §e" + def.buyPrice() + " Coins",
                    "§7Category: §7" + def.category(),
                    "",
                    "§eClick to edit prices!"
                ).build());

            slotToResourceMap.put(slot, def.key());
            slot++;
        }

        inventory.setItem(45, new ItemBuilder(Material.ARROW)
            .name("§c§lBack to Global Config").build());

        if (page > 0) {
            inventory.setItem(47, new ItemBuilder(Material.ARROW)
                .name("§e§l◄ Previous Page")
                .lore("§7Page " + page + " / " + totalPages).build());
        }

        inventory.setItem(49, new ItemBuilder(Material.PAPER)
            .name("§f§lPage " + (page + 1) + " / " + totalPages).build());

        if (page < totalPages - 1) {
            inventory.setItem(51, new ItemBuilder(Material.ARROW)
                .name("§e§lNext Page ►")
                .lore("§7Page " + (page + 2) + " / " + totalPages).build());
        }

        inventory.setItem(53, new ItemBuilder(Material.BARRIER).name("§c§lClose Menu").build());
    }

    private void updateGlobalConfig(String path, double val) {
        BranzIdlePlugin plugin = JavaPlugin.getPlugin(BranzIdlePlugin.class);
        plugin.getConfig().set(path, val);
        plugin.saveConfig();
        // Reload configurations to apply config changes
        plugin.getServiceRegistry().getRegistryManager().reloadAll();
        populate();
    }

    private void editResourcePrice(String resourceKey) {
        player.closeInventory();
        player.sendMessage("§d§l[RESOURCE PRICE] §eType the new Sell and Buy price for §b" + resourceKey + " §e(e.g., `5.0 10.0`), or type `cancel`.");

        org.bukkit.event.Listener listener = new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onChat(org.bukkit.event.player.AsyncPlayerChatEvent e) {
                if (!e.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
                e.setCancelled(true);

                String msg = e.getMessage().trim().toLowerCase();
                if (msg.equals("cancel")) {
                    player.sendMessage("§cPrice edit cancelled.");
                    org.bukkit.event.HandlerList.unregisterAll(this);
                    openEditorLater();
                    return;
                }

                String[] parts = msg.split("[- ]+");
                if (parts.length != 2) {
                    player.sendMessage("§cInvalid format! Use format: `<sell_price> <buy_price>` (e.g. `5.5 11.0`)");
                    return;
                }

                try {
                    double sell = Double.parseDouble(parts[0]);
                    double buy = Double.parseDouble(parts[1]);

                    if (sell < 0 || buy < 0) {
                        player.sendMessage("§cPrices cannot be negative!");
                        return;
                    }

                    org.bukkit.event.HandlerList.unregisterAll(this);
                    Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(BranzIdlePlugin.class), () -> {
                        performResourcePriceUpdate(resourceKey, sell, buy);
                    });
                } catch (NumberFormatException ex) {
                    player.sendMessage("§cInvalid numbers! Format: `<sell_price> <buy_price>`");
                }
            }
        };

        Bukkit.getPluginManager().registerEvents(listener, JavaPlugin.getPlugin(BranzIdlePlugin.class));
    }

    private void performResourcePriceUpdate(String resourceKey, double sell, double buy) {
        BranzIdlePlugin plugin = JavaPlugin.getPlugin(BranzIdlePlugin.class);
        File resourcesDir = new File(plugin.getDataFolder(), "resources");
        if (!resourcesDir.exists()) return;

        File[] files = resourcesDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        boolean updated = false;
        for (File f : files) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(f);
            ConfigurationSection sec = config.getConfigurationSection(resourceKey);
            if (sec != null) {
                sec.set("sell_price", sell);
                sec.set("buy_price", buy);
                try {
                    config.save(f);
                    updated = true;
                    player.sendMessage("§aSuccessfully updated price in §b" + f.getName() + "§a!");
                } catch (IOException e) {
                    player.sendMessage("§cFailed to save " + f.getName());
                    e.printStackTrace();
                }
                break;
            }
        }

        if (updated) {
            plugin.getServiceRegistry().getRegistryManager().reloadAll();
        } else {
            player.sendMessage("§cCould not locate resource key '" + resourceKey + "' inside resources folder config files!");
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
        ClickType click = event.getClick();
        BranzIdlePlugin plugin = JavaPlugin.getPlugin(BranzIdlePlugin.class);

        if (!resourceSelectionMode) {
            // Global Settings Mode Clicks
            if (slot == 45) {
                player.openInventory(new AdminHubGUI(player).getInventory());
                return;
            }
            if (slot == 49) {
                player.closeInventory();
                return;
            }

            if (slot == 11) {
                double current = plugin.getConfig().getDouble("economy.npc_price_modifier", 1.0);
                double delta = 0.0;
                if (click == ClickType.SHIFT_LEFT) delta = 0.5;
                else if (click == ClickType.SHIFT_RIGHT) delta = -0.5;
                else if (click.isLeftClick()) delta = 0.1;
                else if (click.isRightClick()) delta = -0.1;

                if (delta != 0.0) {
                    updateGlobalConfig("economy.npc_price_modifier", Math.max(0.0, current + delta));
                }
                return;
            }

            if (slot == 13) {
                double current = plugin.getConfig().getDouble("economy.global_sell_tax", 0.0);
                double delta = 0.0;
                if (click == ClickType.SHIFT_LEFT) delta = 0.05;
                else if (click == ClickType.SHIFT_RIGHT) delta = -0.05;
                else if (click.isLeftClick()) delta = 0.01;
                else if (click.isRightClick()) delta = -0.01;

                if (delta != 0.0) {
                    updateGlobalConfig("economy.global_sell_tax", Math.max(0.0, Math.min(1.0, current + delta)));
                }
                return;
            }

            if (slot == 15) {
                resourceSelectionMode = true;
                page = 0;
                populate();
                return;
            }
            return;
        }

        // Resource selection mode clicks
        if (slot == 45) {
            resourceSelectionMode = false;
            populate();
            return;
        }
        if (slot == 47) {
            if (page > 0) { page--; populate(); }
            return;
        }
        if (slot == 51) {
            page++;
            populate();
            return;
        }
        if (slot == 53) {
            player.closeInventory();
            return;
        }

        String resourceKey = slotToResourceMap.get(slot);
        if (resourceKey != null) {
            editResourcePrice(resourceKey);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
