package com.example.plugin.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Interface implemented by custom GUIs acting as inventory holders to process player clicks cleanly.
 */
public interface InventoryProvider extends InventoryHolder {

    /**
     * Handles inventory click interactions dispatched by the central GUIController.
     */
    void onClick(InventoryClickEvent event);
}
