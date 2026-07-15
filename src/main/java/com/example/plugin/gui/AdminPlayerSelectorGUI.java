package com.example.plugin.gui;

import com.example.plugin.gui.item.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin player selector GUI (54 slots) listing all online players in the server.
 * Enables selecting a player to modify their account via AdminPlayerEditorGUI.
 */
public class AdminPlayerSelectorGUI implements InventoryProvider {

    private final Player player;
    private final Inventory inventory;
    private final Map<Integer, UUID> slotToPlayerMap = new HashMap<>();

    public AdminPlayerSelectorGUI(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, "§8Admin: §eSelect Online Player");
        populate();
    }

    private void populate() {
        inventory.clear();
        slotToPlayerMap.clear();

        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("§r").build();
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 45; i < 54; i++) inventory.setItem(i, border);

        inventory.setItem(4, new ItemBuilder(Material.NAME_TAG)
            .name("§e§lSelect Player")
            .lore("§7Click an online player below to manage their account.").build());

        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        int slot = 9;

        for (Player target : onlinePlayers) {
            if (slot >= 45) break;

            inventory.setItem(slot, new ItemBuilder(Material.PLAYER_HEAD)
                .name("§6" + target.getName())
                .lore(
                    "§7UUID: §8" + target.getUniqueId(),
                    "§7World: §f" + target.getWorld().getName(),
                    "",
                    "§eClick to edit profile!"
                ).build());

            slotToPlayerMap.put(slot, target.getUniqueId());
            slot++;
        }

        // Slot 45: Back to Admin Hub
        inventory.setItem(45, new ItemBuilder(Material.ARROW)
            .name("§c§lBack to Admin Hub").build());

        // Slot 49: Close
        inventory.setItem(49, new ItemBuilder(Material.BARRIER).name("§c§lClose Menu").build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!player.hasPermission("branzidle.admin")) {
            player.sendMessage("§cYou do not have permission to access this menu!");
            player.closeInventory();
            return;
        }

        int slot = event.getRawSlot();

        if (slot == 45) {
            // Back to Admin Hub
            player.openInventory(new AdminHubGUI(player).getInventory());
            return;
        }

        if (slot == 49) {
            player.closeInventory();
            return;
        }

        UUID targetId = slotToPlayerMap.get(slot);
        if (targetId != null) {
            Player target = Bukkit.getPlayer(targetId);
            if (target != null && target.isOnline()) {
                player.openInventory(new AdminPlayerEditorGUI(player, target).getInventory());
            } else {
                player.sendMessage("§cThat player is no longer online!");
                populate();
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
