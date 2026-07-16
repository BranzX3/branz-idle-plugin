package com.example.plugin.gui;

import com.example.plugin.bootstrap.BranzIdlePlugin;
import com.example.plugin.config.GachaRegistry;
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
 * Admin GUI (54 slots) for listing and editing Gacha pools.
 * Allows live editing of pull costs, currencies, and reward weights.
 */
public class AdminGachaEditorGUI implements InventoryProvider {

    private final Player player;
    private final Inventory inventory;
    private String selectedPoolId;
    private final Map<Integer, String> slotToPoolMap = new HashMap<>();
    private final Map<Integer, String> slotToTemplateMap = new HashMap<>();

    public AdminGachaEditorGUI(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, "§8Admin: Gacha Editor");
        populate();
    }

    private void populate() {
        inventory.clear();
        slotToPoolMap.clear();
        slotToTemplateMap.clear();

        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("§r").build();
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 45; i < 54; i++) inventory.setItem(i, border);

        GachaRegistry registry = JavaPlugin.getPlugin(BranzIdlePlugin.class)
            .getServiceRegistry()
            .getRegistryManager()
            .getGachaRegistry();

        // ── If no pool is selected, show pool selector ──
        if (selectedPoolId == null) {
            inventory.setItem(4, new ItemBuilder(Material.ENDER_CHEST)
                .name("§5§lSelect Gacha Pool")
                .lore("§7Choose a gacha pool below to edit its rates and costs.").build());

            int slot = 19;
            for (GachaRegistry.GachaPoolDefinition pool : registry.getAllPools().values()) {
                if (slot >= 45) break;

                Material icon = pool.currencyType().equals("DIAMONDS") ? Material.DIAMOND : Material.GOLD_INGOT;
                inventory.setItem(slot, new ItemBuilder(icon)
                    .name("§d§l" + pool.poolId())
                    .lore(
                        "§7Currency: §f" + pool.currencyType(),
                        "§7Cost per Pull: §e" + String.format("%,.1f", pool.costPerPull()),
                        "§7Rewards: §b" + pool.rewards().size() + " templates",
                        "",
                        "§eClick to edit pool settings!"
                    ).build());

                slotToPoolMap.put(slot, pool.poolId());
                slot++;
            }

            inventory.setItem(45, new ItemBuilder(Material.ARROW)
                .name("§c§lBack to Admin Hub").build());
            inventory.setItem(49, new ItemBuilder(Material.BARRIER).name("§c§lClose Menu").build());
            return;
        }

        // ── A specific pool is selected ──
        GachaRegistry.GachaPoolDefinition pool = registry.getPool(selectedPoolId).orElse(null);
        if (pool == null) {
            selectedPoolId = null;
            populate();
            return;
        }

        inventory.setItem(4, new ItemBuilder(Material.ENDER_CHEST)
            .name("§5§lPool: §d" + pool.poolId())
            .lore("§7Editing Gacha Pool details. All changes are hot-reloaded.").build());

        // Slot 11: Currency Type Toggle
        Material currIcon = pool.currencyType().equals("DIAMONDS") ? Material.DIAMOND : Material.GOLD_INGOT;
        inventory.setItem(11, new ItemBuilder(currIcon)
            .name("§b§lCurrency Type")
            .lore(
                "§7Current: §e" + pool.currencyType(),
                "",
                "§aClick to toggle (COINS / DIAMONDS)"
            ).build());

        // Slot 13: Cost per Pull
        inventory.setItem(13, new ItemBuilder(Material.GOLD_NUGGET)
            .name("§e§lCost per Pull")
            .lore(
                "§7Current Cost: §a" + String.format("%,.1f", pool.costPerPull()),
                "",
                "§aLeft-Click: §f+100         §bRight-Click: §f-100",
                "§2Shift-Left: §f+1,000       §dShift-Right: §f-1,000"
            ).build());

        // Slot 15: Add Worker Template to Rewards
        inventory.setItem(15, new ItemBuilder(Material.WRITABLE_BOOK)
            .name("§a§lAdd Reward Entry")
            .lore("§7Initiates chat prompt to add a new", "§7worker template reward option.").build());

        // Rewards list (slots 18-44)
        double totalWeight = 0.0;
        for (GachaRegistry.GachaRewardEntry reward : pool.rewards()) {
            totalWeight += reward.weight();
        }

        int slot = 18;
        for (GachaRegistry.GachaRewardEntry reward : pool.rewards()) {
            if (slot >= 45) break;

            double prob = totalWeight > 0 ? (reward.weight() / totalWeight) * 100.0 : 0.0;

            inventory.setItem(slot, new ItemBuilder(Material.PLAYER_HEAD)
                .name("§e§l" + reward.workerTemplate())
                .lore(
                    "§7Template ID: §8" + reward.workerTemplate(),
                    "§7Weight: §b" + reward.weight() + " §8(" + String.format("%.2f", prob) + "%)",
                    "",
                    "§eLeft-Click: §7+5 Weight",
                    "§bRight-Click: §7-5 Weight",
                    "§cShift-Right: §cRemove from pool"
                ).build());

            slotToTemplateMap.put(slot, reward.workerTemplate());
            slot++;
        }

        // Navigation
        inventory.setItem(45, new ItemBuilder(Material.ARROW)
            .name("§c§lBack to Pool List").build());
        inventory.setItem(49, new ItemBuilder(Material.BARRIER).name("§c§lClose Menu").build());
    }

    private void updatePoolParam(String param, Object val) {
        BranzIdlePlugin plugin = JavaPlugin.getPlugin(BranzIdlePlugin.class);
        File file = new File(plugin.getDataFolder(), "gacha.yml");
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set(selectedPoolId + "." + param, val);
        try {
            config.save(file);
            plugin.getServiceRegistry().getRegistryManager().reloadAll();
            populate();
        } catch (IOException e) {
            player.sendMessage("§cFailed to save gacha.yml!");
            e.printStackTrace();
        }
    }

    private void updateRewardWeight(String workerTemplate, double delta) {
        BranzIdlePlugin plugin = JavaPlugin.getPlugin(BranzIdlePlugin.class);
        File file = new File(plugin.getDataFolder(), "gacha.yml");
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = config.getConfigurationSection(selectedPoolId);
        if (sec != null) {
            List<Map<?, ?>> rawRewards = sec.getMapList("rewards");
            boolean found = false;

            for (Map<?, ?> rawMap : rawRewards) {
                if (workerTemplate.equals(rawMap.get("worker_template"))) {
                    double curWeight = 10.0;
                    Object wVal = rawMap.get("weight");
                    if (wVal instanceof Number) curWeight = ((Number) wVal).doubleValue();
                    double newWeight = Math.max(1.0, curWeight + delta);

                    Map<Object, Object> updatedMap = new HashMap<>(rawMap);
                    updatedMap.put("weight", newWeight);

                    int idx = rawRewards.indexOf(rawMap);
                    rawRewards.set(idx, updatedMap);
                    found = true;
                    player.sendMessage("§aUpdated §e" + workerTemplate + " §aweight to §b" + newWeight);
                    break;
                }
            }

            if (found) {
                sec.set("rewards", rawRewards);
                try {
                    config.save(file);
                    plugin.getServiceRegistry().getRegistryManager().reloadAll();
                    populate();
                } catch (IOException e) {
                    player.sendMessage("§cFailed to save gacha.yml!");
                    e.printStackTrace();
                }
            }
        }
    }

    private void removeReward(String workerTemplate) {
        BranzIdlePlugin plugin = JavaPlugin.getPlugin(BranzIdlePlugin.class);
        File file = new File(plugin.getDataFolder(), "gacha.yml");
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = config.getConfigurationSection(selectedPoolId);
        if (sec != null) {
            List<Map<?, ?>> rawRewards = sec.getMapList("rewards");
            boolean found = false;

            for (Map<?, ?> rawMap : rawRewards) {
                if (workerTemplate.equals(rawMap.get("worker_template"))) {
                    rawRewards.remove(rawMap);
                    found = true;
                    player.sendMessage("§cRemoved §e" + workerTemplate + " §cfrom reward pool.");
                    break;
                }
            }

            if (found) {
                sec.set("rewards", rawRewards);
                try {
                    config.save(file);
                    plugin.getServiceRegistry().getRegistryManager().reloadAll();
                    populate();
                } catch (IOException e) {
                    player.sendMessage("§cFailed to save gacha.yml!");
                    e.printStackTrace();
                }
            }
        }
    }

    private void promptAddReward() {
        player.closeInventory();
        player.sendMessage("§d§l[GACHA ADD REWARD] §eType the Worker Template ID (e.g. `miner_common_1`) to add. Or type `cancel`.");

        org.bukkit.event.Listener listener = new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onChat(org.bukkit.event.player.AsyncPlayerChatEvent e) {
                if (!e.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
                e.setCancelled(true);

                String msg = e.getMessage().trim().toLowerCase();
                if (msg.equals("cancel")) {
                    player.sendMessage("§cCancelled reward entry addition.");
                    org.bukkit.event.HandlerList.unregisterAll(this);
                    openEditorLater();
                    return;
                }

                org.bukkit.event.HandlerList.unregisterAll(this);
                Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(BranzIdlePlugin.class), () -> {
                    addRewardToConfig(msg);
                });
            }
        };

        Bukkit.getPluginManager().registerEvents(listener, JavaPlugin.getPlugin(BranzIdlePlugin.class));
    }

    private void addRewardToConfig(String templateId) {
        BranzIdlePlugin plugin = JavaPlugin.getPlugin(BranzIdlePlugin.class);
        File file = new File(plugin.getDataFolder(), "gacha.yml");
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = config.getConfigurationSection(selectedPoolId);
        if (sec != null) {
            List<Map<?, ?>> rawRewards = sec.getMapList("rewards");
            for (Map<?, ?> rawMap : rawRewards) {
                if (templateId.equals(rawMap.get("worker_template"))) {
                    player.sendMessage("§cThis template is already in the reward pool!");
                    player.openInventory(getInventory());
                    return;
                }
            }

            Map<Object, Object> entry = new HashMap<>();
            entry.put("worker_template", templateId);
            entry.put("weight", 10.0);

            rawRewards.add(entry);
            sec.set("rewards", rawRewards);

            try {
                config.save(file);
                plugin.getServiceRegistry().getRegistryManager().reloadAll();
                player.sendMessage("§aAdded reward option: §e" + templateId);
            } catch (IOException e) {
                player.sendMessage("§cFailed to save gacha.yml!");
                e.printStackTrace();
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

        if (selectedPoolId == null) {
            if (slot == 45) {
                player.openInventory(new AdminHubGUI(player).getInventory());
                return;
            }
            if (slot == 49) {
                player.closeInventory();
                return;
            }

            String poolId = slotToPoolMap.get(slot);
            if (poolId != null) {
                selectedPoolId = poolId;
                populate();
            }
            return;
        }

        // Sub-menu for selected pool
        if (slot == 45) {
            selectedPoolId = null;
            populate();
            return;
        }
        if (slot == 49) {
            player.closeInventory();
            return;
        }

        GachaRegistry registry = JavaPlugin.getPlugin(BranzIdlePlugin.class)
            .getServiceRegistry()
            .getRegistryManager()
            .getGachaRegistry();

        GachaRegistry.GachaPoolDefinition pool = registry.getPool(selectedPoolId).orElse(null);
        if (pool == null) return;

        ClickType click = event.getClick();

        if (slot == 11) {
            String newCurr = pool.currencyType().equals("DIAMONDS") ? "COINS" : "DIAMONDS";
            updatePoolParam("currency_type", newCurr);
            return;
        }

        if (slot == 13) {
            double delta = 0.0;
            if (click == ClickType.SHIFT_LEFT) delta = 1000.0;
            else if (click == ClickType.SHIFT_RIGHT) delta = -1000.0;
            else if (click.isLeftClick()) delta = 100.0;
            else if (click.isRightClick()) delta = -100.0;

            if (delta != 0.0) {
                double newCost = Math.max(0.0, pool.costPerPull() + delta);
                updatePoolParam("cost_per_pull", newCost);
            }
            return;
        }

        if (slot == 15) {
            promptAddReward();
            return;
        }

        if (slot >= 18 && slot < 45) {
            String templateId = slotToTemplateMap.get(slot);
            if (templateId != null) {
                if (click == ClickType.SHIFT_RIGHT) {
                    removeReward(templateId);
                } else {
                    double delta = 0.0;
                    if (click.isLeftClick()) delta = 5.0;
                    else if (click.isRightClick()) delta = -5.0;

                    if (delta != 0.0) {
                        updateRewardWeight(templateId, delta);
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
