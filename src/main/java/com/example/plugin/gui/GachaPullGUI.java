package com.example.plugin.gui;

import com.example.plugin.config.RegistryManager;
import com.example.plugin.economy.service.EconomyService;
import com.example.plugin.gui.item.ItemBuilder;
import com.example.plugin.node.service.NodeService;
import com.example.plugin.onboarding.service.OnboardingService;
import com.example.plugin.storage.service.StorageService;
import com.example.plugin.territory.service.TerritoryService;
import com.example.plugin.worker.model.WorkerInstance;
import com.example.plugin.worker.service.WorkerService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Interactive GUI providing Gacha worker recruitment pulls (1x and 10x).
 * Back button navigates to MainHubGUI.
 */
public class GachaPullGUI implements InventoryProvider {

    private final Player player;
    private final WorkerService workerService;
    private final EconomyService economyService;
    private final RegistryManager registryManager;
    private final TerritoryService territoryService;
    private final NodeService nodeService;
    private final StorageService storageService;
    private final OnboardingService onboardingService;
    private final Inventory inventory;

    public GachaPullGUI(
        Player player,
        WorkerService workerService,
        EconomyService economyService,
        RegistryManager registryManager,
        TerritoryService territoryService,
        NodeService nodeService,
        StorageService storageService,
        OnboardingService onboardingService
    ) {
        this.player = player;
        this.workerService = workerService;
        this.economyService = economyService;
        this.registryManager = registryManager;
        this.territoryService = territoryService;
        this.nodeService = nodeService;
        this.storageService = storageService;
        this.onboardingService = onboardingService;
        this.inventory = Bukkit.createInventory(this, 27, "§8Worker Recruitment Banner");
        populate();
    }

    private void populate() {
        inventory.clear();

        ItemStack glass = new ItemBuilder(Material.PURPLE_STAINED_GLASS_PANE).name("§r").build();
        for (int i = 0; i < 27; i++) inventory.setItem(i, glass);

        String gemsName = registryManager.getPlugin().getConfig().getString("economy.diamonds_name", "Gems");
        long gems = economyService.getProfile(player.getUniqueId()).map(com.example.plugin.economy.model.PlayerProfile::getDiamonds).orElse(0L);
        inventory.setItem(4, new ItemBuilder(Material.EMERALD)
            .name("§d§lStandard Worker Banner")
            .lore(
                "§7Your " + gemsName + ": §a" + gems,
                "§7Pity Counter: §e0/90",
                "§dRecruit powerful workers to boost production!"
            ).build());

        inventory.setItem(11, new ItemBuilder(Material.GOLD_INGOT)
            .name("§e§l1x Worker Pull")
            .lore(
                "§7Cost: §b100 " + gemsName,
                "§eClick to perform a single pull!"
            ).build());

        inventory.setItem(15, new ItemBuilder(Material.GOLD_BLOCK)
            .name("§6§l10x Worker Pull")
            .lore(
                "§7Cost: §b1000 " + gemsName,
                "§6Guaranteed 1x Rare or higher!",
                "§eClick to perform a 10x multi-pull!"
            ).build());

        // Slot 18: Back to Hub
        inventory.setItem(18, new ItemBuilder(Material.ARROW)
            .name("§c§lBack to Hub").build());

        inventory.setItem(22, new ItemBuilder(Material.BARRIER).name("§c§lClose Menu").build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot == 18) {
            // Back to Hub
            player.openInventory(new MainHubGUI(player, territoryService, nodeService, workerService, storageService, economyService, onboardingService, registryManager).getInventory());
            return;
        }

        if (slot == 22) {
            player.closeInventory();
            return;
        }

        if (slot == 11) {
            Optional<WorkerInstance> wOpt = pullWorker(player, "standard_banner");
            if (wOpt.isPresent()) {
                populate();
            }
        } else if (slot == 15) {
            String gemsName = registryManager.getPlugin().getConfig().getString("economy.diamonds_name", "Gems");
            long gems = economyService.getProfile(player.getUniqueId()).map(com.example.plugin.economy.model.PlayerProfile::getDiamonds).orElse(0L);
            if (gems < 1000) {
                player.sendMessage("§cYou need 1000 " + gemsName.toLowerCase() + " for a 10x pull! You have " + gems + ".");
                return;
            }
            int pulledCount = 0;
            for (int i = 0; i < 10; i++) {
                Optional<WorkerInstance> wOpt = pullWorker(player, "standard_banner");
                if (wOpt.isPresent()) pulledCount++;
            }
            if (pulledCount > 0) {
                populate();
            }
        }
    }

    private Optional<WorkerInstance> pullWorker(Player player, String bannerId) {
        if (registryManager == null) return Optional.empty();

        Optional<com.example.plugin.config.GachaRegistry.GachaPoolDefinition> poolOpt = registryManager.getGachaRegistry().getPool(bannerId);
        if (poolOpt.isEmpty()) {
            player.sendMessage("§cBanner pool " + bannerId + " not found!");
            return Optional.empty();
        }

        com.example.plugin.config.GachaRegistry.GachaPoolDefinition pool = poolOpt.get();
        double cost = pool.costPerPull();

        String gemsName = registryManager.getPlugin().getConfig().getString("economy.diamonds_name", "Gems");
        if (pool.currencyType().equalsIgnoreCase("DIAMONDS")) {
            long currentGems = economyService.getProfile(player.getUniqueId()).map(com.example.plugin.economy.model.PlayerProfile::getDiamonds).orElse(0L);
            if (currentGems < (long) cost) {
                player.sendMessage("§cInsufficient " + gemsName + "! Need " + (long) cost + ".");
                return Optional.empty();
            }
            economyService.removeDiamonds(player.getUniqueId(), (long) cost);
        } else {
            double currentCoins = economyService.getProfile(player.getUniqueId()).map(com.example.plugin.economy.model.PlayerProfile::getCoins).orElse(0.0);
            if (currentCoins < cost) {
                player.sendMessage("§cInsufficient Coins! Need " + (long) cost + ".");
                return Optional.empty();
            }
            economyService.removeCoins(player.getUniqueId(), cost);
        }

        double totalWeight = pool.rewards().stream().mapToDouble(com.example.plugin.config.GachaRegistry.GachaRewardEntry::weight).sum();
        if (totalWeight <= 0.0) {
            player.sendMessage("§cConfig error: Gacha pool has no weight!");
            return Optional.empty();
        }

        double rand = java.util.concurrent.ThreadLocalRandom.current().nextDouble() * totalWeight;
        double accum = 0.0;
        String rolledTemplate = null;
        for (com.example.plugin.config.GachaRegistry.GachaRewardEntry entry : pool.rewards()) {
            accum += entry.weight();
            if (rand <= accum) {
                rolledTemplate = entry.workerTemplate();
                break;
            }
        }

        if (rolledTemplate == null && !pool.rewards().isEmpty()) {
            rolledTemplate = pool.rewards().get(0).workerTemplate();
        }

        if (rolledTemplate == null) {
            player.sendMessage("§cError rolling gacha.");
            return Optional.empty();
        }

        WorkerInstance worker = workerService.spawnWorker(player.getUniqueId(), rolledTemplate);
        try {
            com.example.plugin.bootstrap.BranzIdlePlugin.getPlugin(com.example.plugin.bootstrap.BranzIdlePlugin.class)
                .getServiceRegistry().getService(com.example.plugin.analytics.service.AnalyticsService.class)
                .ifPresent(com.example.plugin.analytics.service.AnalyticsService::trackGachaPull);
        } catch (Exception ignored) {}
        player.sendMessage("§a§lRecruited worker: §e" + rolledTemplate + "§a!");
        return Optional.of(worker);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
