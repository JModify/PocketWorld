package com.pocketworld.plugin.api.event;

import com.pocketworld.plugin.world.PocketWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Fired when a player is teleported into a pocket world. */
public class PlayerEnterPocketWorldEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final PocketWorld pocketWorld;

    public PlayerEnterPocketWorldEvent(Player player, PocketWorld pocketWorld) {
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
