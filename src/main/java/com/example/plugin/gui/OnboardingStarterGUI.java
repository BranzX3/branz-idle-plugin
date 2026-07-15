package com.example.plugin.gui;

import com.example.plugin.gui.item.ItemBuilder;
import com.example.plugin.onboarding.service.OnboardingService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Interactive onboarding GUI welcoming new players and guiding them through base claiming.
 */
public class OnboardingStarterGUI implements InventoryProvider {

    private final Player player;
    private final OnboardingService onboardingService;
    private final Inventory inventory;

    public OnboardingStarterGUI(Player player, OnboardingService onboardingService) {
        this.player = player;
        this.onboardingService = onboardingService;
        this.inventory = Bukkit.createInventory(this, 27, "§8Welcome to Branz.Idle MMORPG");
        populate();
    }

    private void populate() {
        inventory.clear();

        ItemStack glass = new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).name("§r").build();
        for (int i = 0; i < 27; i++) inventory.setItem(i, glass);

        inventory.setItem(4, new ItemBuilder(Material.BOOK)
            .name("§b§lWelcome to Branz.Idle!")
            .lore(
                "§7This server runs an autonomous Idle MMORPG.",
                "§7Claim your territory, establish nodes, recruit workers,",
                "§7and build an empire even while you sleep!"
            ).build());

        boolean isOnboarded = onboardingService.isPlayerOnboarded(player);
        if (isOnboarded) {
            inventory.setItem(13, new ItemBuilder(Material.MINECART)
                .name("§a§lStarter Base Secured!")
                .lore(
                    "§7You have completed your onboarding flow.",
                    "§7Your 2x2 base and starter workers are active!",
                    "§7Use §e/idle map §7or §e/idle worker §7to proceed."
                ).build());
        } else {
            inventory.setItem(13, new ItemBuilder(Material.CHEST_MINECART)
                .name("§e§lSecure Starter Base & Claim Pack")
                .lore(
                    "§7Receive: §61000 Coins §7+ §a3 Starter Workers",
                    "§7Claims your current 2x2 chunk block (4 chunks)",
                    "§7and establishes your very first Production Nodes!",
                    "§eClick to claim and establish base here!"
                ).build());
        }

        inventory.setItem(22, new ItemBuilder(Material.BARRIER).name("§c§lClose Guide").build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 22) {
            player.closeInventory();
            return;
        }

        if (slot == 13) {
            if (onboardingService.isPlayerOnboarded(player)) {
                player.sendMessage("§cYou are already onboarded!");
                return;
            }
            org.bukkit.Chunk chunk = player.getLocation().getChunk();
            boolean success = onboardingService.startOnboarding(player, chunk.getX(), chunk.getZ());
            if (success) {
                player.closeInventory();
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
