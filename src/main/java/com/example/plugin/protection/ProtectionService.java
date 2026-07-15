package com.example.plugin.protection;

import com.example.plugin.territory.event.TerritoryAccessDeniedEvent;
import com.example.plugin.territory.model.ChunkClaim;
import com.example.plugin.territory.service.TerritoryService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

/**
 * Core security service evaluating player permissions and territory claims for world interactions.
 */
public class ProtectionService {

    private final JavaPlugin plugin;
    private final TerritoryService territoryService;
    private final boolean allowWildernessBuilding;

    public ProtectionService(JavaPlugin plugin, TerritoryService territoryService) {
        this.plugin = plugin;
        this.territoryService = territoryService;
        this.allowWildernessBuilding = plugin.getConfig().getBoolean("territory.allow_wilderness_building", false);
    }

    /**
     * Evaluates whether a player has permission to perform an action at the specified location.
     *
     * @param player Player attempting action
     * @param location Target world location
     * @param actionType Type of action being attempted
     * @return true if allowed, false if denied
     */
    public boolean hasAccess(Player player, Location location, TerritoryAccessDeniedEvent.ActionType actionType) {
        if (player.hasPermission("branzidle.admin")) {
            return true;
        }

        World world = location.getWorld();
        if (world == null) {
            return false;
        }

        boolean inDedicatedWorld = territoryService.isDedicatedWorld(world);
        int chunkX = location.getChunk().getX();
        int chunkZ = location.getChunk().getZ();

        Optional<ChunkClaim> claimOpt = territoryService.getClaim(chunkX, chunkZ);
        if (claimOpt.isPresent()) {
            ChunkClaim claim = claimOpt.get();
            if (claim.getOwnerId().equals(player.getUniqueId())) {
                return true;
            } else {
                notifyAccessDenied(player, location, actionType);
                return false;
            }
        } else {
            // Unclaimed chunk in dedicated world
            if (inDedicatedWorld && !allowWildernessBuilding) {
                notifyAccessDenied(player, location, actionType);
                return false;
            }
            return true;
        }
    }

    private void notifyAccessDenied(Player player, Location location, TerritoryAccessDeniedEvent.ActionType actionType) {
        TerritoryAccessDeniedEvent event = new TerritoryAccessDeniedEvent(player, location, actionType);
        Bukkit.getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            player.sendMessage("§cYou cannot modify blocks or interact with territory that does not belong to you!");
        }
    }

    public TerritoryService getTerritoryService() {
        return territoryService;
    }
}
