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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Interactive Warehouse GUI (54 slots) enabling players to view their virtual resources
 * and safely withdraw them as physical Minecraft ItemStacks.
 */
public class WarehouseGUI implements InventoryProvider {

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

    public WarehouseGUI(
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
        this.inventory = Bukkit.createInventory(this, 54, "§8Virtual Warehouse");
        populate();
    }

    private void populate() {
        inventory.clear();
        slotToResourceMap.clear();

        // Border glass
        ItemStack glass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("§r").build();
        for (int i = 0; i < 9; i++) inventory.setItem(i, glass);
        for (int i = 45; i < 54; i++) inventory.setItem(i, glass);

        inventory.setItem(4, new ItemBuilder(Material.CHEST)
            .name("§6§lVirtual Warehouse")
            .lore(
                "§7Store bulk items produced by nodes here.",
                "§aLeft-Click: §7Withdraw 1 Stack (64)",
                "§bRight-Click: §7Withdraw All (fits in inventory)"
            ).build());

        PlayerResourceWallet wallet = storageService.getPlayerWallet(player.getUniqueId());
        int slot = 9;

        for (Map.Entry<String, Long> entry : wallet.getResourceQuantities().entrySet()) {
            if (slot >= 45) break;
            long amount = entry.getValue();
            if (amount <= 0) continue;

            String resKey = entry.getKey();
            ItemStack previewItem = createItemStack(resKey, 1);
            String displayName = previewItem.getItemMeta().hasDisplayName()
                ? previewItem.getItemMeta().getDisplayName()
                : "§f" + resKey;

            inventory.setItem(slot, new ItemBuilder(previewItem.getType())
                .name(displayName)
                .lore(
                    "§7Stored Amount: §b" + amount,
                    "",
                    "§aLeft-Click to withdraw 1 Stack (64)",
                    "§bRight-Click to withdraw all that fits"
                ).build());

            slotToResourceMap.put(slot, resKey);
            slot++;
        }

        // Slot 45: Back to Hub
        inventory.setItem(45, new ItemBuilder(Material.ARROW)
            .name("§c§lBack to Hub").build());

        // Slot 49: Close
        inventory.setItem(49, new ItemBuilder(Material.BARRIER).name("§c§lClose Menu").build());
    }

    private ItemStack createItemStack(String resourceKey, int amount) {
        Optional<com.example.plugin.config.ResourceRegistry.ResourceDefinition> defOpt = registryManager.getResourceRegistry().getResource(resourceKey);
        if (defOpt.isPresent()) {
            com.example.plugin.config.ResourceRegistry.ResourceDefinition def = defOpt.get();
            ItemBuilder builder = new ItemBuilder(def.material(), amount)
                .name(org.bukkit.ChatColor.translateAlternateColorCodes('&', def.displayName()))
                .lore("§7Branz.Idle production yield.");
            if (def.customModelData() > 0) {
                builder.customModelData(def.customModelData());
            }
            return builder.build();
        }

        // Fallback for unregistered items / standard minecraft materials
        Material material = Material.matchMaterial(resourceKey.toUpperCase());
        if (material == null) {
            material = Material.PAPER;
        }
        return new ItemBuilder(material, amount)
            .name("§e§l" + resourceKey)
            .lore("§7Branz.Idle production yield.").build();
    }

    private void handleWithdraw(String resourceKey, boolean all) {
        UUID playerId = player.getUniqueId();
        long walletQty = storageService.getPlayerWallet(playerId).getQuantity(resourceKey);
        if (walletQty <= 0) {
            player.sendMessage("§cYou do not have any " + resourceKey + " in your warehouse.");
            return;
        }

        // Maximum standard capacity limit we want to process at once is a full inventory (2304 items)
        int requestedAmount = all ? (int) Math.min(walletQty, 2304) : (int) Math.min(walletQty, 64);
        if (requestedAmount <= 0) return;

        // 1. Spend the resource first from the wallet cache & database to prevent race conditions
        boolean spendSuccess = storageService.spendResource(playerId, resourceKey, requestedAmount);
        if (!spendSuccess) {
            player.sendMessage("§cError processing withdraw transaction: Insufficient funds.");
            return;
        }

        // 2. Add item stacks to player's inventory
        ItemStack stackToWithdraw = createItemStack(resourceKey, requestedAmount);
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(stackToWithdraw);

        // 3. If the inventory was full or partially full, calculate leftover and refund it to wallet
        int leftoverQty = leftover.values().stream().mapToInt(ItemStack::getAmount).sum();
        int successfullyAdded = requestedAmount - leftoverQty;

        if (leftoverQty > 0) {
            storageService.addResourceToWallet(playerId, resourceKey, leftoverQty);
        }

        if (successfullyAdded > 0) {
            String name = stackToWithdraw.getItemMeta().hasDisplayName()
                ? stackToWithdraw.getItemMeta().getDisplayName()
                : resourceKey;
            player.sendMessage("§aWithdrew " + successfullyAdded + "x " + name + " into your inventory!");
            populate();
        } else {
            player.sendMessage("§cYour inventory is full! Cannot withdraw items.");
        }
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
            ClickType click = event.getClick();
            if (click.isLeftClick()) {
                handleWithdraw(resKey, false);
            } else if (click.isRightClick()) {
                handleWithdraw(resKey, true);
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
