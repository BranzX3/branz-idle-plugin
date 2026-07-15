package com.example.plugin.territory.event;

import com.example.plugin.territory.model.ChunkClaim;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired immediately after a player successfully claims a territory chunk.
 */
public class TerritoryClaimedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final ChunkClaim claim;

    public TerritoryClaimedEvent(Player player, ChunkClaim claim) {
        this.player = player;
        this.claim = claim;
    }

    public Player getPlayer() {
        return player;
    }

    public ChunkClaim getClaim() {
        return claim;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
