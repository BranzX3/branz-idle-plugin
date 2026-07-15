package com.example.plugin.onboarding.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Event fired after a player successfully finishes the onboarding territory and starter pack flow.
 */
public class PlayerOnboardingCompletedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final int starterChunksGranted;
    private final List<String> starterWorkerTemplates;

    public PlayerOnboardingCompletedEvent(Player player, int starterChunksGranted, List<String> starterWorkerTemplates) {
        this.player = player;
        this.starterChunksGranted = starterChunksGranted;
        this.starterWorkerTemplates = starterWorkerTemplates;
    }

    public Player getPlayer() {
        return player;
    }

    public int getStarterChunksGranted() {
        return starterChunksGranted;
    }

    public List<String> getStarterWorkerTemplates() {
        return starterWorkerTemplates;
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
