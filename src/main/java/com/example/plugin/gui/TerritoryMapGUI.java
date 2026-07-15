package com.example.plugin.gui;

import com.example.plugin.config.RegistryManager;
import com.example.plugin.economy.service.EconomyService;
import com.example.plugin.gui.item.ItemBuilder;
import com.example.plugin.node.service.NodeService;
import com.example.plugin.onboarding.service.OnboardingService;
import com.example.plugin.storage.service.StorageService;
import com.example.plugin.territory.service.TerritoryService;
import com.example.plugin.worker.service.WorkerService;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interactive GUI mapping surrounding chunk territory ownership in a grid.
 * Navigates back to MainHubGUI via Back to Hub button.
 */
public class TerritoryMapGUI implements InventoryProvider {

    private final Player player;
    private final TerritoryService territoryService;
    private final NodeService nodeService;
    private final WorkerService workerService;
    private final StorageService storageService;
    private final EconomyService economyService;
    private final OnboardingService onboardingService;
    private final RegistryManager registryManager;
    private final Inventory inventory;
    private final Chunk centerChunk;

    public TerritoryMapGUI(
        Player player,
        TerritoryService territoryService,
        NodeService nodeService,
        WorkerService workerService,
        StorageService storageService,
        EconomyService economyService,
        OnboardingService onboardingService,
        RegistryManager registryManager
    ) {
        this.player = player;
        this.territoryService = territoryService;
        this.nodeService = nodeService;
        this.workerService = workerService;
        this.storageService = storageService;
        this.economyService = economyService;
        this.onboardingService = onboardingService;
        this.registryManager = registryManager;
        this.centerChunk = player.getLocation().getChunk();
        this.inventory = Bukkit.createInventory(this, 54, "§8Surrounding Chunk Map");
        populate();
    }

    private void populate() {
        inventory.clear();

        int centerX = centerChunk.getX();
        int centerZ = centerChunk.getZ();

        // 9x5 Grid (Slots 0..44) representing X: -4..+4, Z: -2..+2
        for (int dz = -2; dz <= 2; dz++) {
            for (int dx = -4; dx <= 4; dx++) {
                int slot = (dz + 2) * 9 + (dx + 4);
                int targetX = centerX + dx;
                int targetZ = centerZ + dz;
                Optional<com.example.plugin.territory.model.ChunkClaim> claimOpt = territoryService.getClaim(targetX, targetZ);
                Optional<UUID> ownerOpt = claimOpt.map(com.example.plugin.territory.model.ChunkClaim::getOwnerId);
                boolean isResidential = claimOpt.map(c -> c.getChunkType() == com.example.plugin.territory.model.ChunkType.RESIDENTIAL).orElse(false);

                if (dx == 0 && dz == 0) {
                    if (ownerOpt.isPresent() && ownerOpt.get().equals(player.getUniqueId())) {
                        String typeStr = isResidential ? "RESIDENTIAL BASE" : "PRODUCTION PLOT";
                        String actionLore = isResidential ? "§8(Base Hub - Cannot Unclaim)" : "§cClick to Unclaim this chunk";
                        inventory.setItem(slot, new ItemBuilder(Material.GREEN_STAINED_GLASS)
                            .name("§a§l[YOUR CHUNK - CURRENT POS]")
                            .lore(
                                "§7Coord: X:" + targetX + " Z:" + targetZ,
                                "§7Type: " + typeStr,
                                "",
                                actionLore
                            ).build());
                    } else if (ownerOpt.isPresent()) {
                        inventory.setItem(slot, new ItemBuilder(Material.RED_STAINED_GLASS)
                            .name("§c§l[CLAIMED CHUNK - CURRENT POS]")
                            .lore("§7Coord: X:" + targetX + " Z:" + targetZ, "§cOwned by another player!").build());
                    } else {
                        inventory.setItem(slot, new ItemBuilder(Material.YELLOW_STAINED_GLASS)
                            .name("§e§l[WILD CHUNK - CURRENT POS]")
                            .lore("§7Coord: X:" + targetX + " Z:" + targetZ, "", "§eClick to claim this chunk!").build());
                    }
                } else if (ownerOpt.isPresent()) {
                    if (ownerOpt.get().equals(player.getUniqueId())) {
                        String typeStr = isResidential ? "RESIDENTIAL BASE" : "PRODUCTION PLOT";
                        String actionLore = isResidential ? "§8(Base Hub - Cannot Unclaim)" : "§cClick to Unclaim this chunk";
                        inventory.setItem(slot, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                            .name("§a[Your Territory]")
                            .lore(
                                "§7Coord: X:" + targetX + " Z:" + targetZ,
                                "§7Type: " + typeStr,
                                "",
                                actionLore
                            ).build());
                    } else {
                        inventory.setItem(slot, new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                            .name("§c[Claimed Territory]")
                            .lore("§7Coord: X:" + targetX + " Z:" + targetZ).build());
                    }
                } else {
                    inventory.setItem(slot, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                        .name("§7[Wilderness]")
                        .lore(
                            "§7Coord: X:" + targetX + " Z:" + targetZ,
                            "§8Unclaimed wilderness.",
                            "",
                            "§eClick to claim this chunk!"
                        ).build());
                }
            }
        }

        // Bottom row legend & navigation
        ItemStack border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name("§r").build();
        for (int i = 45; i < 54; i++) inventory.setItem(i, border);

        // Slot 45: Back to Hub
        inventory.setItem(45, new ItemBuilder(Material.ARROW)
            .name("§c§lBack to Hub").build());

        inventory.setItem(48, new ItemBuilder(Material.PAPER)
            .name("§e§lMap Legend")
            .lore(
                "§aLime/Green: §7Your claimed territory",
                "§cRed: §7Another player's territory",
                "§7Gray/Yellow: §7Unclaimed Wilderness"
            ).build());

        inventory.setItem(49, new ItemBuilder(Material.BARRIER).name("§c§lClose Map").build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
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

        if (slot >= 0 && slot < 45) {
            int centerX = centerChunk.getX();
            int centerZ = centerChunk.getZ();
            int row = slot / 9;
            int col = slot % 9;
            int dx = col - 4;
            int dz = row - 2;
            int targetX = centerX + dx;
            int targetZ = centerZ + dz;

            Optional<com.example.plugin.territory.model.ChunkClaim> claimOpt = territoryService.getClaim(targetX, targetZ);
            if (claimOpt.isPresent()) {
                com.example.plugin.territory.model.ChunkClaim claim = claimOpt.get();
                if (claim.getOwnerId().equals(player.getUniqueId()) || player.hasPermission("branzidle.admin")) {
                    // Unclaim — show confirmation
                    UUID nodeId = claim.getNodeId();
                    int workerCount = nodeId != null ? workerService.getNodeWorkers(nodeId).size() : 0;

                    List<String> loreLines = new java.util.ArrayList<>();
                    loreLines.add("§7Chunk: §e(" + targetX + ", " + targetZ + ")");
                    if (claim.getChunkType() == com.example.plugin.territory.model.ChunkType.RESIDENTIAL) {
                        loreLines.add("§cUnclaiming your Residential Base Hub!");
                        loreLines.add("§7You will need to onboard again to claim a new base.");
                    } else {
                        loreLines.add("§7This will remove the production node");
                        loreLines.add("§7and unassign §c" + workerCount + " worker(s)§7.");
                    }
                    loreLines.add("");
                    loreLines.add("§cThis action cannot be undone!");

                    player.openInventory(new ConfirmationGUI(
                        player,
                        "Unclaim Chunk",
                        loreLines,
                        () -> {
                            // Execute unclaim logic
                            if (nodeId != null) {
                                for (com.example.plugin.worker.model.WorkerInstance w : workerService.getNodeWorkers(nodeId)) {
                                    w.setAssignedNodeId(null);
                                    workerService.updateWorker(w);
                                }
                                nodeService.deleteNode(nodeId);
                            }
                            territoryService.unclaimChunk(player, targetX, targetZ);

                            // If they unclaimed their residential chunk, reset onboarding status
                            if (claim.getChunkType() == com.example.plugin.territory.model.ChunkType.RESIDENTIAL) {
                                economyService.setOnboardingCompleted(player.getUniqueId(), false);
                                player.sendMessage("§eYour Residential Base Hub has been unclaimed. Onboarding reset.");
                            }

                            player.openInventory(new TerritoryMapGUI(player, territoryService, nodeService, workerService, storageService, economyService, onboardingService, registryManager).getInventory());
                        },
                        () -> player.openInventory(new TerritoryMapGUI(player, territoryService, nodeService, workerService, storageService, economyService, onboardingService, registryManager).getInventory())
                    ).getInventory());
                } else {
                    player.sendMessage("§cYou do not own this territory chunk!");
                }
            } else {
                // Wilderness: Open NodeType selection
                player.openInventory(new NodeClaimSelectionGUI(player, targetX, targetZ, territoryService, nodeService, workerService, storageService, economyService, onboardingService, registryManager).getInventory());
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
