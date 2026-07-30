package com.pocketworld.plugin.api.event;

import com.pocketworld.plugin.world.PocketWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Fired when a player leaves a pocket world (by teleport or world change) for anywhere else. */
public class PlayerLeavePocketWorldEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final PocketWorld pocketWorld;

    public PlayerLeavePocketWorldEvent(Player player, PocketWorld pocketWorld) {
        this.player = player;
        this.pocketWorld = pocketWorld;
    }

    public Player getPlayer() {
        return player;
    }

    public PocketWorld getPocketWorld() {
        return pocketWorld;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
