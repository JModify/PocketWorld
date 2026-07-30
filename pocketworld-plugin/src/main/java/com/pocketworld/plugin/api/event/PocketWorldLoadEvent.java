package com.pocketworld.plugin.api.event;

import com.pocketworld.plugin.world.PocketWorld;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Fired once a pocket world finishes loading and its Bukkit world is live. */
public class PocketWorldLoadEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final PocketWorld pocketWorld;
    private final World bukkitWorld;

    public PocketWorldLoadEvent(PocketWorld pocketWorld, World bukkitWorld) {
        this.pocketWorld = pocketWorld;
        this.bukkitWorld = bukkitWorld;
    }

    public PocketWorld getPocketWorld() {
        return pocketWorld;
    }

    public World getBukkitWorld() {
        return bukkitWorld;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
