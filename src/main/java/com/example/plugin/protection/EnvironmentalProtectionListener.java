package com.example.plugin.protection;

import com.example.plugin.territory.service.TerritoryService;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Iterator;

/**
 * Listener preventing environmental damage (explosions, fire spread, endermen, farmland trampling) inside territory.
 */
public class EnvironmentalProtectionListener implements Listener {

    private final ProtectionService protectionService;
    private final TerritoryService territoryService;

    public EnvironmentalProtectionListener(ProtectionService protectionService) {
        this.protectionService = protectionService;
        this.territoryService = protectionService.getTerritoryService();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        World world = event.getLocation().getWorld();
        if (world == null) return;

        boolean inDedicatedWorld = territoryService.isDedicatedWorld(world);
        Iterator<Block> it = event.blockList().iterator();
        while (it.hasNext()) {
            Block b = it.next();
            int cx = b.getChunk().getX();
            int cz = b.getChunk().getZ();
            if (inDedicatedWorld || territoryService.getClaim(cx, cz).isPresent()) {
                it.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        World world = event.getBlock().getWorld();
        if (world == null) return;

        boolean inDedicatedWorld = territoryService.isDedicatedWorld(world);
        Iterator<Block> it = event.blockList().iterator();
        while (it.hasNext()) {
            Block b = it.next();
            int cx = b.getChunk().getX();
            int cz = b.getChunk().getZ();
            if (inDedicatedWorld || territoryService.getClaim(cx, cz).isPresent()) {
                it.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof Enderman || event.getEntityType() == EntityType.WITHER) {
            Block b = event.getBlock();
            int cx = b.getChunk().getX();
            int cz = b.getChunk().getZ();
            if (territoryService.isDedicatedWorld(b.getWorld()) || territoryService.getClaim(cx, cz).isPresent()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        Block b = event.getBlock();
        int cx = b.getChunk().getX();
        int cz = b.getChunk().getZ();
        if (territoryService.isDedicatedWorld(b.getWorld()) || territoryService.getClaim(cx, cz).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        Block b = event.getBlock();
        int cx = b.getChunk().getX();
        int cz = b.getChunk().getZ();
        if (territoryService.isDedicatedWorld(b.getWorld()) || territoryService.getClaim(cx, cz).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFarmlandTrample(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL && event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.FARMLAND) {
            Player player = event.getPlayer();
            Block b = event.getClickedBlock();
            int cx = b.getChunk().getX();
            int cz = b.getChunk().getZ();
            if (!protectionService.hasAccess(player, b.getLocation(), com.example.plugin.territory.event.TerritoryAccessDeniedEvent.ActionType.BLOCK_BREAK)) {
                event.setCancelled(true);
            }
        }
    }
}
