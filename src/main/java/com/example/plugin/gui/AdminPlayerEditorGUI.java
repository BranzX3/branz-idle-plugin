package com.example.plugin.gui;

import com.example.plugin.bootstrap.BranzIdlePlugin;
import com.example.plugin.bootstrap.ServiceRegistry;
import com.example.plugin.economy.service.EconomyService;
import com.example.plugin.gui.item.ItemBuilder;
import com.example.plugin.node.service.NodeService;
import com.example.plugin.onboarding.service.OnboardingService;
import com.example.plugin.worker.model.WorkerInstance;
import com.example.plugin.worker.service.WorkerService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Admin interface (54 slots) to edit a specific player's profile in real-time.
 * Restricted to staffs with 'branzidle.admin' permission.
 */
public class AdminPlayerEditorGUI implements InventoryProvider {

    private final Player player;
    private final Player target;
    private final Inventory inventory;

    private final EconomyService economyService;
    private final NodeService nodeService;
    private final WorkerService workerService;
    private final OnboardingService onboardingService;

    public AdminPlayerEditorGUI(Player player, Player target) {
        this.player = player;
        this.target = target;
        this.inventory = Bukkit.createInventory(this, 54, "§8Admin: §eEdit " + target.getName());

        ServiceRegistry registry = JavaPlugin.getPlugin(BranzIdlePlugin.class).getServiceRegistry();
        this.economyService = registry.getRequiredService(EconomyService.class);
        this.nodeService = registry.getRequiredService(NodeService.class);
        this.workerService = registry.getRequiredService(WorkerService.class);
        this.onboardingService = registry.getRequiredService(OnboardingService.class);

        populate();
    }

    private void populate() {
        inventory.clear();

        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("§r").build();
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, border);
        }

        // Target information card
        double coinsVal = economyService.getProfile(target.getUniqueId())
            .map(com.example.plugin.economy.model.PlayerProfile::getCoins).orElse(0.0);
        long coins = (long) coinsVal;
        long diamonds = economyService.getProfile(target.getUniqueId())
            .map(com.example.plugin.economy.model.PlayerProfile::getDiamonds).orElse(0L);
        int nodeCount = nodeService.getPlayerNodes(target.getUniqueId()).size();
        int workerCount = workerService.getPlayerWorkers(target.getUniqueId()).size();

        inventory.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
            .name("§6§lTarget: §e§l" + target.getName())
            .lore(
                "§7UUID: §8" + target.getUniqueId(),
                "§7Coins: §6" + coins,
                "§7Diamonds: §b" + diamonds,
                "§7Nodes Owned: §a" + nodeCount,
                "§7Workers Hired: §d" + workerCount
            ).build());

        // Slot 11: Add 10,000 Coins
        inventory.setItem(11, new ItemBuilder(Material.GOLD_INGOT)
            .name("§a§lAdd 10,000 Coins")
            .lore("§7Grant 10,000 Coins directly to", "§7this player's account.", "", "§eClick to grant!").build());

        // Slot 13: Add 1,000 Diamonds
        inventory.setItem(13, new ItemBuilder(Material.DIAMOND)
            .name("§a§lAdd 1,000 Diamonds")
            .lore("§7Grant 1,000 Diamonds directly to", "§7this player's account.", "", "§eClick to grant!").build());

        // Slot 15: Spawn Random Worker
        inventory.setItem(15, new ItemBuilder(Material.IRON_AXE)
            .name("§d§lSpawn Random Worker")
            .lore(
                "§7Spawn and grant a random worker from Gacha",
                "§7templates to this player's collection.",
                "",
                "§eClick to spawn!"
            ).build());

        // Slot 22: Manage Player's Nodes (Exploration Level control)
        inventory.setItem(22, new ItemBuilder(Material.COMPASS)
            .name("§e§lManage Production Nodes")
            .lore(
                "§7View all production nodes owned by this player,",
                "§7and adjust their node exploration mastery levels.",
                "",
                "§eClick to manage nodes!"
            ).build());

        // Slot 31: Reset Base (Destructive - Confirmation)
        inventory.setItem(31, new ItemBuilder(Material.TNT)
            .name("§c§lForce Base Reset")
            .lore(
                "§7Forcefully reset all claims, nodes, and worker",
                "§7assignments for this player.",
                "§7Currency and worker templates are kept.",
                "",
                "§c§lWARNING: This action is irreversible!",
                "§eClick to force reset base."
            ).build());

        // Slot 45: Back to Player List
        inventory.setItem(45, new ItemBuilder(Material.ARROW)
            .name("§c§lBack to Player List").build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!player.hasPermission("branzidle.admin")) {
            player.sendMessage("§cYou do not have permission to access this menu!");
            player.closeInventory();
            return;
        }

        if (!target.isOnline()) {
            player.sendMessage("§cThis player is no longer online!");
            player.openInventory(new AdminPlayerSelectorGUI(player).getInventory());
            return;
        }

        int slot = event.getRawSlot();

        switch (slot) {
            case 11 -> {
                economyService.addCoins(target.getUniqueId(), 10000.0);
                player.sendMessage("§aGranted 10,000 Coins to " + target.getName() + "!");
                populate();
            }
            case 13 -> {
                economyService.addDiamonds(target.getUniqueId(), 1000L);
                player.sendMessage("§aGranted 1,000 Diamonds to " + target.getName() + "!");
                populate();
            }
            case 15 -> {
                // Spawn a random worker
                String[] templates = {"miner_common_1", "lumberjack_common_1", "fisher_common_1", "miner_epic_1", "fisher_legendary_1"};
                String rolled = templates[ThreadLocalRandom.current().nextInt(templates.length)];
                WorkerInstance worker = workerService.spawnWorker(target.getUniqueId(), rolled);
                player.sendMessage("§aSuccessfully spawned worker " + rolled + " for " + target.getName() + "!");
                populate();
            }
            case 22 -> {
                // Open Player's Nodes list
                player.openInventory(new AdminPlayerNodesGUI(player, target).getInventory());
            }
            case 31 -> {
                // Force base reset
                player.openInventory(new ConfirmationGUI(
                    player,
                    "Force Reset Base: " + target.getName(),
                    List.of(
                        "§cThis will permanently delete:",
                        "§7• All " + target.getName() + "'s territory claims",
                        "§7• All " + target.getName() + "'s production nodes",
                        "§7• All worker assignments",
                        "",
                        "§aTarget's workers and currency are kept."
                    ),
                    () -> {
                        onboardingService.resetBase(target);
                        player.sendMessage("§aSuccessfully reset base for " + target.getName() + "!");
                        player.openInventory(new AdminPlayerEditorGUI(player, target).getInventory());
                    },
                    () -> player.openInventory(new AdminPlayerEditorGUI(player, target).getInventory())
                ).getInventory());
            }
            case 45 -> {
                // Back to Player List
                player.openInventory(new AdminPlayerSelectorGUI(player).getInventory());
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
