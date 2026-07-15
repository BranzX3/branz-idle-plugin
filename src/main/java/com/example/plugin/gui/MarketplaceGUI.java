package com.example.plugin.gui;

import com.example.plugin.economy.service.EconomyService;
import com.example.plugin.gui.item.ItemBuilder;
import com.example.plugin.storage.model.PlayerResourceWallet;
import com.example.plugin.storage.service.StorageService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Interactive GUI marketplace enabling players to sell virtual warehouse resources for coins.
 */
public class MarketplaceGUI implements InventoryProvider {

    private final Player player;
    private final StorageService storageService;
    private final EconomyService economyService;
    private final Inventory inventory;
    private final Map<Integer, String> slotToResourceMap = new HashMap<>();

    public MarketplaceGUI(Player player, StorageService storageService, EconomyService economyService) {
        this.player = player;
        this.storageService = storageService;
        this.economyService = economyService;
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
        if (slot == 49) {
            player.closeInventory();
            return;
        }

        String resKey = slotToResourceMap.get(slot);
        if (resKey != null) {
            PlayerResourceWallet wallet = storageService.getPlayerWallet(player.getUniqueId());
            long amount = wallet.getQuantity(resKey);
            if (amount > 0 && storageService.spendResource(player.getUniqueId(), resKey, amount)) {
                long totalCoins = amount * getUnitPrice(resKey);
                economyService.addCoins(player.getUniqueId(), totalCoins);
                player.sendMessage("§aSold " + amount + "x " + resKey + " for §6" + totalCoins + " coins!");
                populate();
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
