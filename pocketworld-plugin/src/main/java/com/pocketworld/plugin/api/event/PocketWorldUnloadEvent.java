package com.pocketworld.plugin.api.event;

import com.pocketworld.plugin.world.PocketWorld;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Fired just before a pocket world's Bukkit world is unloaded. */
public class PocketWorldUnloadEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final PocketWorld pocketWorld;

    public PocketWorldUnloadEvent(PocketWorld pocketWorld) {
        this.pocketWorld = pocketWorld;
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
