package com.pocketworld.plugin.api.event;

import com.pocketworld.plugin.world.PocketWorld;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/** Fired once a new pocket world has finished generating and is live. */
public class PocketWorldCreateEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final PocketWorld pocketWorld;
    private final World bukkitWorld;
    private final UUID creatorId;

    public PocketWorldCreateEvent(PocketWorld pocketWorld, World bukkitWorld, UUID creatorId) {
        this.pocketWorld = pocketWorld;
        this.bukkitWorld = bukkitWorld;
        this.creatorId = creatorId;
    }

    public PocketWorld getPocketWorld() {
        return pocketWorld;
    }

    public World getBukkitWorld() {
        return bukkitWorld;
    }

    public UUID getCreatorId() {
        return creatorId;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
