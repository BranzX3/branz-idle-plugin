package com.example.plugin.gui;

import com.example.plugin.config.RegistryManager;
import com.example.plugin.economy.service.EconomyService;
import com.example.plugin.gui.item.ItemBuilder;
import com.example.plugin.node.service.NodeService;
import com.example.plugin.onboarding.service.OnboardingService;
import com.example.plugin.storage.service.StorageService;
import com.example.plugin.territory.service.TerritoryService;
import com.example.plugin.worker.service.WorkerService;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Central navigation hub GUI (54 slots) for Branz.Idle MMORPG.
 * Serves as the main entry point from /idle or /idle menu.
 * Provides quick access to all game systems: Map, Nodes, Workers, Gacha, Marketplace.
 * Integrates onboarding (claim starter base) and base reset functionality.
 */
public class MainHubGUI implements InventoryProvider {

    private final Player player;
    private final TerritoryService territoryService;
    private final NodeService nodeService;
    private final WorkerService workerService;
    private final StorageService storageService;
    private final EconomyService economyService;
    private final OnboardingService onboardingService;
    private final RegistryManager registryManager;
    private final Inventory inventory;

    public MainHubGUI(
        Player player,
        TerritoryService territoryService,
        NodeService nodeService,
        WorkerService workerService,
        StorageService storageService,
        EconomyService economyService,
        OnboardingService onboardingService,
        RegistryManager registryManager
    ) {
        this.player = player;
        this.territoryService = territoryService;
        this.nodeService = nodeService;
        this.workerService = workerService;
        this.storageService = storageService;
        this.economyService = economyService;
        this.onboardingService = onboardingService;
        this.registryManager = registryManager;
        this.inventory = Bukkit.createInventory(this, 54, "§8Branz.Idle §6§lMain Hub");
        populate();
    }

    private void populate() {
        inventory.clear();

        // Fill all slots with glass border
        ItemStack glass = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name("§r").build();
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, glass);
        }

        // Row 1: Title + Player profile
        inventory.setItem(4, new ItemBuilder(Material.NETHER_STAR)
            .name("§6§lBranz.Idle MMORPG")
            .lore(
                "§7Welcome to Branz.Idle!",
                "§7Build your empire, manage workers,",
                "§7and produce resources even while offline!"
            ).build());

        // Player profile info (coins & diamonds)
        double coinsVal = economyService.getProfile(player.getUniqueId())
            .map(com.example.plugin.economy.model.PlayerProfile::getCoins).orElse(0.0);
        long coins = (long) coinsVal;
        long diamonds = economyService.getProfile(player.getUniqueId())
            .map(com.example.plugin.economy.model.PlayerProfile::getDiamonds).orElse(0L);
        int nodeCount = nodeService.getPlayerNodes(player.getUniqueId()).size();
        int workerCount = workerService.getPlayerWorkers(player.getUniqueId()).size();

        String coinsName = registryManager.getPlugin().getConfig().getString("economy.currency_name", "Coins");
        String diamondsName = registryManager.getPlugin().getConfig().getString("economy.diamonds_name", "Diamonds");

        inventory.setItem(8, new ItemBuilder(Material.PLAYER_HEAD)
            .name("§e§l" + player.getName() + "'s Profile")
            .lore(
                "§7" + coinsName + ": §6" + coins,
                "§7" + diamondsName + ": §b" + diamonds,
                "§7Nodes: §a" + nodeCount,
                "§7Workers: §d" + workerCount
            ).build());

        // Row 2: Main navigation buttons
        inventory.setItem(11, new ItemBuilder(Material.FILLED_MAP)
            .name("§a§lTerritory Map")
            .lore(
                "§7View and manage your claimed territory.",
                "§7Claim new chunks or unclaim existing ones.",
                "",
                "§eClick to open the Territory Map!"
            ).build());

        inventory.setItem(13, new ItemBuilder(Material.BEACON)
            .name("§6§lMy Nodes")
            .lore(
                "§7View all your production nodes.",
                "§7Check tier, workers, and collected yields.",
                "",
                "§eClick to browse your nodes!"
            ).build());

        inventory.setItem(15, new ItemBuilder(Material.IRON_AXE)
            .name("§d§lWorker Management")
            .lore(
                "§7Assign and manage workers across nodes.",
                "§7Select a node first, then manage its workers.",
                "",
                "§eClick to manage workers!"
            ).build());

        // Row 3: Secondary features
        inventory.setItem(20, new ItemBuilder(Material.EMERALD)
            .name("§5§lGacha Banner")
            .lore(
                "§7Recruit powerful new workers!",
                "§7Spend Gems to pull from the banner.",
                "",
                "§eClick to open Gacha!"
            ).build());

        inventory.setItem(22, new ItemBuilder(Material.GOLD_INGOT)
            .name("§e§lMarketplace")
            .lore(
                "§7Sell your stored resources for coins.",
                "§7Check resource prices and sell in bulk.",
                "",
                "§eClick to open the Marketplace!"
            ).build());

        inventory.setItem(24, new ItemBuilder(Material.CHEST)
            .name("§6§lVirtual Warehouse")
            .lore(
                "§7View bulk items stored in your virtual warehouse.",
                "§7Withdraw items as physical blocks and ores.",
                "",
                "§eClick to open the Warehouse!"
            ).build());

        // Row 4: Onboarding / Base section
        boolean isOnboarded = onboardingService.isPlayerOnboarded(player);
        if (isOnboarded) {
            inventory.setItem(31, new ItemBuilder(Material.TNT)
                .name("§c§lReset Base")
                .lore(
                    "§7Remove all your territory, nodes,",
                    "§7and worker assignments.",
                    "§7Workers and currency are preserved.",
                    "",
                    "§c§lWARNING: This action is irreversible!",
                    "§eClick to reset your base."
                ).build());
        } else {
            inventory.setItem(31, new ItemBuilder(Material.CHEST_MINECART)
                .name("§e§lClaim Starter Base")
                .lore(
                    "§7Receive: §61000 Coins §7+ §a3 Starter Workers",
                    "§7Claims your current 2x2 chunk block (4 chunks)",
                    "§7and establishes your first Production Nodes!",
                    "",
                    "§eClick to claim and establish base here!"
                ).build());
        }
        
        // Row 4: Unlock Node Slot (Slot 29)
        Optional<PlayerProfile> profOpt = economyService.getProfile(player.getUniqueId());
        if (profOpt.isPresent()) {
            PlayerProfile profile = profOpt.get();
            int currentLimit = profile.getUnlockedSlots();
            int maxLimit = getMaxSlotLimit(player);
            
            ItemBuilder slotBtn = new ItemBuilder(Material.GOLD_BLOCK)
                .name("§e§lUnlock Node Slot");
            
            if (currentLimit >= 40) {
                slotBtn.lore(
                    "§7Current Capacity: §a" + currentLimit + " / 40",
                    "",
                    "§aMaximum capacity reached!"
                );
            } else {
                int nextSlot = currentLimit + 1;
                long costCoins = getNodeSlotCost(nextSlot);
                long costDiamonds = costCoins / 100;
                
                slotBtn.lore(
                    "§7Current Capacity: §a" + currentLimit + " / 40",
                    "§7Your Rank Max Limit: §e" + maxLimit,
                    "",
                    "§aLeft-Click to Unlock next slot:",
                    "§7Cost: §6" + costCoins + " Coins",
                    "",
                    "§bRight-Click to Unlock next slot:",
                    "§7Cost: §b" + costDiamonds + " Diamonds"
                );
            }
            inventory.setItem(29, slotBtn.build());
        }

        // Row 6: Close
        inventory.setItem(49, new ItemBuilder(Material.BARRIER).name("§c§lClose Menu").build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        switch (slot) {
            case 11 -> {
                // Territory Map
                if (!territoryService.isDedicatedWorld(player.getWorld())) {
                    player.sendMessage("§cYou can only view the Territory Map in the dedicated Idle world!");
                    return;
                }
                player.openInventory(new TerritoryMapGUI(player, territoryService, nodeService, workerService, storageService, economyService, onboardingService, registryManager).getInventory());
            }
            case 13 -> {
                // Node Selection (Overview mode)
                player.openInventory(new NodeSelectionGUI(player, NodeSelectionGUI.Mode.OVERVIEW, territoryService, nodeService, workerService, storageService, economyService, onboardingService, registryManager).getInventory());
            }
            case 15 -> {
                // Node Selection (Worker mode)
                player.openInventory(new NodeSelectionGUI(player, NodeSelectionGUI.Mode.WORKER, territoryService, nodeService, workerService, storageService, economyService, onboardingService, registryManager).getInventory());
            }
            case 20 -> {
                // Gacha
                player.openInventory(new GachaPullGUI(player, workerService, economyService, registryManager, territoryService, nodeService, storageService, onboardingService).getInventory());
            }
            case 22 -> {
                // Marketplace
                player.openInventory(new MarketplaceGUI(player, storageService, economyService, territoryService, nodeService, workerService, onboardingService, registryManager).getInventory());
            }
            case 24 -> {
                // Virtual Warehouse
                player.openInventory(new WarehouseGUI(player, storageService, economyService, territoryService, nodeService, workerService, onboardingService, registryManager).getInventory());
            }
            case 31 -> {
                boolean isOnboarded = onboardingService.isPlayerOnboarded(player);
                if (isOnboarded) {
                    // Reset Base — show confirmation
                    player.openInventory(new ConfirmationGUI(
                        player,
                        "Confirm Reset Base",
                        List.of(
                            "§cThis will permanently remove:",
                            "§7• All your territory chunks",
                            "§7• All your production nodes",
                            "§7• All worker assignments",
                            "",
                            "§aYour workers and currency will be kept."
                        ),
                        () -> {
                            onboardingService.resetBase(player);
                            player.openInventory(new MainHubGUI(player, territoryService, nodeService, workerService, storageService, economyService, onboardingService, registryManager).getInventory());
                        },
                        () -> player.openInventory(new MainHubGUI(player, territoryService, nodeService, workerService, storageService, economyService, onboardingService, registryManager).getInventory())
                    ).getInventory());
                } else {
                    // Claim Starter Base
                    Chunk chunk = player.getLocation().getChunk();
                    boolean success = onboardingService.startOnboarding(player, chunk.getX(), chunk.getZ());
                    if (success) {
                        // Refresh hub to show updated state
                        player.openInventory(new MainHubGUI(player, territoryService, nodeService, workerService, storageService, economyService, onboardingService, registryManager).getInventory());
                    }
                }
            }
            case 29 -> {
                Optional<PlayerProfile> profOpt = economyService.getProfile(player.getUniqueId());
                if (profOpt.isEmpty()) return;
                PlayerProfile profile = profOpt.get();
                int currentLimit = profile.getUnlockedSlots();
                int maxLimit = getMaxSlotLimit(player);

                if (currentLimit >= 40) {
                    player.sendMessage("§cYou have reached the maximum capacity of 40 node slots!");
                    return;
                }

                if (currentLimit >= maxLimit) {
                    player.sendMessage("§cYou have reached your Rank's Max Limit (" + currentLimit + "/" + maxLimit + ")!");
                    player.sendMessage("§ePurchase a higher premium rank to unlock up to 40 slots!");
                    return;
                }

                int nextSlot = currentLimit + 1;
                long costCoins = getNodeSlotCost(nextSlot);
                long costDiamonds = costCoins / 100;

                boolean isRightClick = event.isRightClick();
                if (isRightClick) {
                    if (economyService.removeDiamonds(player.getUniqueId(), costDiamonds)) {
                        profile.setUnlockedSlots(nextSlot);
                        economyService.saveProfile(profile);
                        player.sendMessage("§a§l[Expansion] §7Unlocked Node Slot #" + nextSlot + " with Diamonds!");
                        populate();
                    } else {
                        player.sendMessage("§cYou do not have enough Diamonds!");
                    }
                } else {
                    if (economyService.removeCoins(player.getUniqueId(), costCoins)) {
                        profile.setUnlockedSlots(nextSlot);
                        economyService.saveProfile(profile);
                        player.sendMessage("§a§l[Expansion] §7Unlocked Node Slot #" + nextSlot + " with Coins!");
                        populate();
                    } else {
                        player.sendMessage("§cYou do not have enough Coins!");
                    }
                }
            }
            case 49 -> player.closeInventory();
            default -> {
                // Ignore clicks on glass/border slots
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public static int getMaxSlotLimit(Player player) {
        if (player.hasPermission("branzidle.rank.elite") || player.hasPermission("branzidle.rank.legend")) {
            return 40;
        }
        if (player.hasPermission("branzidle.rank.vip")) {
            return 30;
        }
        return 20;
    }

    public static long getNodeSlotCost(int nextSlotNumber) {
        if (nextSlotNumber <= 4) return 0;
        if (nextSlotNumber == 5) return 10000;
        if (nextSlotNumber == 6) return 25000;
        if (nextSlotNumber == 7) return 50000;
        if (nextSlotNumber == 8) return 100000;
        long diff = nextSlotNumber - 8;
        return 100000L + (diff * 50000L);
    }
}
