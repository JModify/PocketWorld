package me.modify.pocketworld.event;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * PlayerDisconnectionEvent is the event of which a player actually disconnects from the server.
 * This could be either a forcible kick or when the player leaves themselves.
 * May be called multiple times due to the nature of PlayerQuitEvent and PlayerKickEvents.
 */
public class PlayerDisconnectEvent extends Event {

    @Getter private Player player;
    public PlayerDisconnectEvent(Player player) {
        this.player = player;
    }

    private static final HandlerList HANDLERS = new HandlerList();

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @NotNull @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
