package com.example.plugin.gui;

import com.example.plugin.gui.item.ItemBuilder;
import com.example.plugin.territory.model.ChunkCoord;
import com.example.plugin.node.service.NodeService;
import com.example.plugin.worker.service.WorkerService;
import com.example.plugin.territory.service.TerritoryService;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

/**
 * Interactive GUI mapping surrounding chunk territory ownership in a grid.
 */
public class TerritoryMapGUI implements InventoryProvider {

    private final Player player;
    private final TerritoryService territoryService;
    private final NodeService nodeService;
    private final WorkerService workerService;
    private final Inventory inventory;
    private final Chunk centerChunk;

    public TerritoryMapGUI(Player player, TerritoryService territoryService, NodeService nodeService, WorkerService workerService) {
        this.player = player;
        this.territoryService = territoryService;
        this.nodeService = nodeService;
        this.workerService = workerService;
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

        // Bottom row legend & close
        ItemStack border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name("§r").build();
        for (int i = 45; i < 54; i++) inventory.setItem(i, border);

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
                if (claim.getOwnerId().equals(player.getUniqueId())) {
                    if (claim.getChunkType() == com.example.plugin.territory.model.ChunkType.RESIDENTIAL) {
                        player.sendMessage("§cYou cannot unclaim your Residential Base Hub!");
                    } else {
                        // Unclaim and release node & workers
                        UUID nodeId = claim.getNodeId();
                        if (nodeId != null) {
                            // Unassign workers
                            for (com.example.plugin.worker.model.WorkerInstance w : workerService.getNodeWorkers(nodeId)) {
                                w.setAssignedNodeId(null);
                                workerService.updateWorker(w);
                            }
                            // Delete node
                            nodeService.deleteNode(nodeId);
                        }
                        territoryService.unclaimChunk(player, targetX, targetZ);
                        populate();
                    }
                } else {
                    player.sendMessage("§cYou do not own this territory chunk!");
                }
            } else {
                // Wilderness: Open NodeType selection
                player.openInventory(new NodeClaimSelectionGUI(player, targetX, targetZ, territoryService, nodeService, workerService).getInventory());
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
