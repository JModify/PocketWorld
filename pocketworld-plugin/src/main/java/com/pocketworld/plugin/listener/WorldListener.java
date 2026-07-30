package com.pocketworld.plugin.listener;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.api.event.PlayerEnterPocketWorldEvent;
import com.pocketworld.plugin.api.event.PlayerLeavePocketWorldEvent;
import com.pocketworld.plugin.util.PocketUtils;
import com.pocketworld.plugin.world.PocketWorld;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import java.util.UUID;

public class WorldListener implements Listener {

    private final PocketWorldPlugin plugin;

    public WorldListener(PocketWorldPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Cancels animal/monster spawns per the owning PocketWorld's settings. Uses a cache-only lookup
     * ({@code getIfCached}, never {@code readThrough}) deliberately: this fires on every single
     * mob-spawn attempt in every pocket world, so any DAO fallback reachable from here would mean an
     * occasional synchronous database call on the main thread from one of the hottest event paths in
     * the game. A loaded world is always cached first (see {@link PocketWorld#load}), so a cache miss
     * here can only mean the world isn't actually a live pocket world - safe to just do nothing.
     */
    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        World world = event.getEntity().getWorld();
        String name = world.getName();

        if (!PocketUtils.isUUID(name)) {
            return;
        }

        UUID worldId = UUID.fromString(name);
        PocketWorld pocketWorld = plugin.getWorldCache().getIfCached(worldId);
        if (pocketWorld == null || !pocketWorld.isLoaded()) {
            return;
        }

        Entity entity = event.getEntity();
        if (entity instanceof Animals && !pocketWorld.isAllowAnimals()) {
            event.setCancelled(true);
        } else if (entity instanceof Monster && !pocketWorld.isAllowMonsters()) {
            event.setCancelled(true);
        }
    }

    /** Fires the public enter/leave API events by diffing the player's previous and new world. */
    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        PocketWorld previous = asPocketWorld(event.getFrom().getName());
        if (previous != null) {
            Bukkit.getPluginManager().callEvent(new PlayerLeavePocketWorldEvent(player, previous));
        }

        PocketWorld current = asPocketWorld(player.getWorld().getName());
        if (current != null) {
            Bukkit.getPluginManager().callEvent(new PlayerEnterPocketWorldEvent(player, current));
        }
    }

    private PocketWorld asPocketWorld(String worldName) {
        if (!PocketUtils.isUUID(worldName)) {
            return null;
        }
        return plugin.getWorldCache().getIfCached(UUID.fromString(worldName));
    }
}
