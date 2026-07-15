package com.example.plugin.gui.item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fluent helper class for constructing custom ItemStacks with display names and lore.
 */
public class ItemBuilder {

    private final ItemStack itemStack;
    private final ItemMeta itemMeta;

    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
        this.itemMeta = itemStack.getItemMeta();
    }

    public ItemBuilder(Material material, int amount) {
        this.itemStack = new ItemStack(material, amount);
        this.itemMeta = itemStack.getItemMeta();
    }

    public ItemBuilder name(String name) {
        if (itemMeta != null) {
            itemMeta.setDisplayName(name);
        }
        return this;
    }

    /**
     * Sets the lore, replacing any existing lore lines entirely.
     */
    public ItemBuilder lore(String... lines) {
        if (itemMeta != null) {
            itemMeta.setLore(Arrays.asList(lines));
        }
        return this;
    }

    /**
     * Sets the lore, replacing any existing lore lines entirely.
     */
    public ItemBuilder lore(List<String> lines) {
        if (itemMeta != null) {
            itemMeta.setLore(lines);
        }
        return this;
    }

    /**
     * Appends additional lore lines after existing lore without overwriting.
     * Use this when building lore incrementally (e.g., base info + status line).
     */
    public ItemBuilder addLore(String... lines) {
        if (itemMeta != null) {
            List<String> existing = itemMeta.getLore();
            if (existing == null) {
                existing = new ArrayList<>();
            } else {
                existing = new ArrayList<>(existing);
            }
            existing.addAll(Arrays.asList(lines));
            itemMeta.setLore(existing);
        }
        return this;
    }

    public ItemStack build() {
        if (itemMeta != null) {
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }
}
