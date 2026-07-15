package com.example.plugin.gui;

import com.example.plugin.config.RegistryManager;
import com.example.plugin.economy.service.EconomyService;
import com.example.plugin.gui.item.ItemBuilder;
import com.example.plugin.node.service.NodeService;
import com.example.plugin.onboarding.service.OnboardingService;
import com.example.plugin.storage.model.PlayerResourceWallet;
import com.example.plugin.storage.service.StorageService;
import com.example.plugin.territory.service.TerritoryService;
import com.example.plugin.worker.service.WorkerService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interactive GUI marketplace enabling players to sell virtual warehouse resources for coins.
 * Sell actions show ConfirmationGUI before executing. Back button navigates to MainHubGUI.
 */
public class MarketplaceGUI implements InventoryProvider {

    private final Player player;
    private final StorageService storageService;
    private final EconomyService economyService;
    private final TerritoryService territoryService;
    private final NodeService nodeService;
    private final WorkerService workerService;
    private final OnboardingService onboardingService;
    private final RegistryManager registryManager;
    private final Inventory inventory;
    private final Map<Integer, String> slotToResourceMap = new HashMap<>();

    public MarketplaceGUI(
        Player player,
        StorageService storageService,
        EconomyService economyService,
        TerritoryService territoryService,
        NodeService nodeService,
        WorkerService workerService,
        OnboardingService onboardingService,
        RegistryManager registryManager
    ) {
        this.player = player;
        this.storageService = storageService;
        this.economyService = economyService;
        this.territoryService = territoryService;
        this.nodeService = nodeService;
        this.workerService = workerService;
        this.onboardingService = onboardingService;
        this.registryManager = registryManager;
        this.inventory = Bukkit.createInventory(this, 54, "§8Virtual Resource Marketplace");
        populate();
    }

    private void populate() {
        inventory.clear();
        slotToResourceMap.clear();

        ItemStack glass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("§r").build();
        for (int i = 0; i < 9; i++) inventory.setItem(i, glass);
        for (int i = 45; i < 54; i++) inventory.setItem(i, glass);

        double coinsVal = economyService.getProfile(player.getUniqueId()).map(com.example.plugin.economy.model.PlayerProfile::getCoins).orElse(0.0);
        long coins = (long) coinsVal;
        inventory.setItem(4, new ItemBuilder(Material.GOLD_INGOT)
            .name("§6§lResource Marketplace")
            .lore(
                "§7Your Coins: §6" + coins,
                "§7Click any stored resource below to sell all for coins."
            ).build());

        PlayerResourceWallet wallet = storageService.getPlayerWallet(player.getUniqueId());
        int slot = 9;

        for (Map.Entry<String, Long> entry : wallet.getResourceQuantities().entrySet()) {
            if (slot >= 45) break;
            long amount = entry.getValue();
            if (amount <= 0) continue;

            String resKey = entry.getKey();
            long pricePerItem = getUnitPrice(resKey);
            long totalValue = amount * pricePerItem;

            inventory.setItem(slot, new ItemBuilder(Material.CHEST_MINECART)
                .name("§e§lResource: §a" + resKey)
                .lore(
                    "§7Stored Quantity: §b" + amount,
                    "§7Unit Price: §6" + pricePerItem + " Coins",
                    "§7Total Value: §a" + totalValue + " Coins",
                    "§eClick to sell all!"
                ).build());

            slotToResourceMap.put(slot, resKey);
            slot++;
        }

        // Slot 45: Back to Hub
        inventory.setItem(45, new ItemBuilder(Material.ARROW)
            .name("§c§lBack to Hub").build());

        inventory.setItem(49, new ItemBuilder(Material.BARRIER).name("§c§lClose Marketplace").build());
    }

    private long getUnitPrice(String resourceKey) {
        return switch (resourceKey.toLowerCase()) {
            case "iron_ore", "coal" -> 5L;
            case "gold_ore", "lapis_lazuli" -> 15L;
            case "diamond", "emerald" -> 50L;
            case "netherite_scrap" -> 200L;
            default -> 10L;
        };
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot == 45) {
            // Back to Hub
            player.openInventory(new MainHubGUI(player, territoryService, nodeService, workerService, storageService, economyService, onboardingService, registryManager).getInventory());
            return;
        }

        if (slot == 49) {
            player.closeInventory();
            return;
        }

        String resKey = slotToResourceMap.get(slot);
        if (resKey != null) {
            PlayerResourceWallet wallet = storageService.getPlayerWallet(player.getUniqueId());
            long amount = wallet.getQuantity(resKey);
            if (amount <= 0) return;

            long totalCoins = amount * getUnitPrice(resKey);

            // Show confirmation before selling
            player.openInventory(new ConfirmationGUI(
                player,
                "Sell All " + resKey,
                List.of(
                    "§7Resource: §e" + resKey,
                    "§7Quantity: §b" + amount,
                    "§7Total Value: §6" + totalCoins + " Coins",
                    "",
                    "§7All stored units will be sold."
                ),
                () -> {
                    // Re-check amount in case it changed
                    PlayerResourceWallet freshWallet = storageService.getPlayerWallet(player.getUniqueId());
                    long freshAmount = freshWallet.getQuantity(resKey);
                    if (freshAmount > 0 && storageService.spendResource(player.getUniqueId(), resKey, freshAmount)) {
                        long freshCoins = freshAmount * getUnitPrice(resKey);
                        economyService.addCoins(player.getUniqueId(), freshCoins);
                        player.sendMessage("§aSold " + freshAmount + "x " + resKey + " for §6" + freshCoins + " coins!");
                    }
                    player.openInventory(new MarketplaceGUI(player, storageService, economyService, territoryService, nodeService, workerService, onboardingService, registryManager).getInventory());
                },
                () -> player.openInventory(new MarketplaceGUI(player, storageService, economyService, territoryService, nodeService, workerService, onboardingService, registryManager).getInventory())
            ).getInventory());
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
