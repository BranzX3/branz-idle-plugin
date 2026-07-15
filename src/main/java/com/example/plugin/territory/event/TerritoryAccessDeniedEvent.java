package com.example.plugin.territory.event;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a player attempts an unauthorized action inside a territory claim.
 * Allows plugins or GUI alerts to intercept access denial.
 */
public class TerritoryAccessDeniedEvent extends Event implements Cancellable {

    public enum ActionType {
        BLOCK_BREAK, BLOCK_PLACE, CONTAINER_ACCESS, REDSTONE_INTERACT, ENTITY_DAMAGE
    }

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final Location location;
    private final ActionType actionType;
    private boolean cancelled = false;

    public TerritoryAccessDeniedEvent(Player player, Location location, ActionType actionType) {
        this.player = player;
        this.location = location;
        this.actionType = actionType;
    }

    public Player getPlayer() {
        return player;
    }

    public Location getLocation() {
        return location;
    }

    public ActionType getActionType() {
        return actionType;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
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
