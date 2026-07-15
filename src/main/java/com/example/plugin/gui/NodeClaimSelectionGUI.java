package com.example.plugin.gui;

import com.example.plugin.config.RegistryManager;
import com.example.plugin.economy.service.EconomyService;
import com.example.plugin.gui.item.ItemBuilder;
import com.example.plugin.node.model.NodeType;
import com.example.plugin.node.service.NodeService;
import com.example.plugin.onboarding.service.OnboardingService;
import com.example.plugin.storage.service.StorageService;
import com.example.plugin.territory.service.TerritoryService;
import com.example.plugin.worker.service.WorkerService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * GUI for selecting a NodeType to create upon claiming a new chunk.
 * Cancel returns to TerritoryMapGUI.
 */
public class NodeClaimSelectionGUI implements InventoryProvider {

    private final Player player;
    private final int chunkX;
    private final int chunkZ;
    private final TerritoryService territoryService;
    private final NodeService nodeService;
    private final WorkerService workerService;
    private final StorageService storageService;
    private final EconomyService economyService;
    private final OnboardingService onboardingService;
    private final RegistryManager registryManager;
    private final Inventory inventory;

    public NodeClaimSelectionGUI(
        Player player,
        int chunkX,
        int chunkZ,
        TerritoryService territoryService,
        NodeService nodeService,
        WorkerService workerService,
        StorageService storageService,
        EconomyService economyService,
        OnboardingService onboardingService,
        RegistryManager registryManager
    ) {
        this.player = player;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.territoryService = territoryService;
        this.nodeService = nodeService;
        this.workerService = workerService;
        this.storageService = storageService;
        this.economyService = economyService;
        this.onboardingService = onboardingService;
        this.registryManager = registryManager;
        this.inventory = Bukkit.createInventory(this, 27, "§8Select Node Type to Build");
        populate();
    }

    private void populate() {
        ItemStack border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name("§r").build();
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, border);
        }

        inventory.setItem(11, new ItemBuilder(Material.IRON_PICKAXE)
            .name("§6§lMINING NODE")
            .lore("§7Claim this chunk and build", "§7a Mining node here.", "", "§aClick to build!").build());

        inventory.setItem(13, new ItemBuilder(Material.IRON_AXE)
            .name("§a§lLUMBER NODE")
            .lore("§7Claim this chunk and build", "§7a Lumber node here.", "", "§aClick to build!").build());

        inventory.setItem(15, new ItemBuilder(Material.FISHING_ROD)
            .name("§b§lFISHING NODE")
            .lore("§7Claim this chunk and build", "§7a Fishing node here.", "", "§aClick to build!").build());

        inventory.setItem(22, new ItemBuilder(Material.BARRIER).name("§c§lCancel").build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 22) {
            // Cancel → back to Territory Map
            player.openInventory(new TerritoryMapGUI(player, territoryService, nodeService, workerService, storageService, economyService, onboardingService, registryManager).getInventory());
            return;
        }

        NodeType type = null;
        if (slot == 11) {
            type = NodeType.MINING;
        } else if (slot == 13) {
            type = NodeType.LUMBER;
        } else if (slot == 15) {
            type = NodeType.FISHING;
        }

        if (type != null) {
            boolean success = territoryService.claimChunk(player, chunkX, chunkZ, com.example.plugin.territory.model.ChunkType.PRODUCTION);
            if (success) {
                nodeService.createNode(player.getUniqueId(), type, chunkX, chunkZ);
                player.sendMessage("§aClaimed chunk at (" + chunkX + ", " + chunkZ + ") as a " + type.name() + " node!");
            }
            player.openInventory(new TerritoryMapGUI(player, territoryService, nodeService, workerService, storageService, economyService, onboardingService, registryManager).getInventory());
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
