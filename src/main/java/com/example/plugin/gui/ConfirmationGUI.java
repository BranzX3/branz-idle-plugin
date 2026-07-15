package com.example.plugin.gui;

import com.example.plugin.gui.item.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Reusable confirmation dialog GUI (27 slots) for destructive actions.
 * Displays a description of the action and Confirm/Cancel buttons.
 * Executes the provided callback only when the player explicitly confirms.
 */
public class ConfirmationGUI implements InventoryProvider {

    private final Player player;
    private final Inventory inventory;
    private final Runnable onConfirm;
    private final Runnable onCancel;

    /**
     * Creates a new confirmation dialog.
     *
     * @param player          the player viewing the dialog
     * @param title           inventory title (e.g., "Confirm Reset Base")
     * @param descriptionLore explanation lines shown on the info item
     * @param onConfirm       callback executed when the player clicks Confirm
     * @param onCancel        callback executed when the player clicks Cancel
     */
    public ConfirmationGUI(Player player, String title, List<String> descriptionLore, Runnable onConfirm, Runnable onCancel) {
        this.player = player;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
        this.inventory = Bukkit.createInventory(this, 27, "§8" + title);
        populate(title, descriptionLore);
    }

    private void populate(String title, List<String> descriptionLore) {
        ItemStack glass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("§r").build();
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, glass);
        }

        // Slot 11: Confirm button
        inventory.setItem(11, new ItemBuilder(Material.LIME_WOOL)
            .name("§a§lConfirm")
            .lore("§7Click to proceed with this action.").build());

        // Slot 13: Info / description
        inventory.setItem(13, new ItemBuilder(Material.PAPER)
            .name("§e§l" + title)
            .lore(descriptionLore).build());

        // Slot 15: Cancel button
        inventory.setItem(15, new ItemBuilder(Material.RED_WOOL)
            .name("§c§lCancel")
            .lore("§7Click to go back without changes.").build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot == 11) {
            // Confirmed — execute the callback
            onConfirm.run();
        } else if (slot == 15) {
            // Cancelled — return to previous GUI
            onCancel.run();
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
