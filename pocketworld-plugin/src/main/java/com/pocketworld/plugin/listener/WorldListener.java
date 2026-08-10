package com.pocketworld.plugin.listener;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.api.event.PlayerEnterPocketWorldEvent;
import com.pocketworld.plugin.api.event.PlayerLeavePocketWorldEvent;
import com.pocketworld.plugin.util.PocketUtils;
import com.pocketworld.plugin.world.PocketWorld;
import com.pocketworld.plugin.world.WorldAction;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;

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

    /** Enforces {@link WorldAction#BREAK} for whoever's effective {@link com.pocketworld.plugin.world.PermissionRank}
     *  in this pocket world doesn't allow it - owners and anyone the matrix grants it always pass. */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        PocketWorld pocketWorld = asPocketWorld(event.getBlock().getWorld().getName());
        if (pocketWorld == null) {
            return;
        }
        denyUnless(event, pocketWorld, event.getPlayer(), WorldAction.BREAK);
    }

    /** Enforces {@link WorldAction#BUILD}, same rules as {@link #onBlockBreak}. */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        PocketWorld pocketWorld = asPocketWorld(event.getBlock().getWorld().getName());
        if (pocketWorld == null) {
            return;
        }
        denyUnless(event, pocketWorld, event.getPlayer(), WorldAction.BUILD);
    }

    /** Enforces {@link WorldAction#INTERACT} for right-clicking blocks with their own click
     *  behavior (doors, chests, buttons, levers, ...) and physical triggers (pressure plates,
     *  tripwire) - not item use in the air, and deliberately not plain terrain. Right-clicking a
     *  non-interactable block (dirt, stone, ...) while holding a placeable item fires this event
     *  before {@link BlockPlaceEvent} does, purely to place the item - gating that under INTERACT
     *  too would entangle it with {@link WorldAction#BUILD}, requiring both permissions just to
     *  place a block against plain ground. {@link Material#isInteractable()} is Bukkit's own flag
     *  for exactly this distinction. */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.PHYSICAL) {
            return;
        }
        if (event.getClickedBlock() == null || !event.getClickedBlock().getType().isInteractable()) {
            return;
        }

        PocketWorld pocketWorld = asPocketWorld(event.getClickedBlock().getWorld().getName());
        if (pocketWorld == null) {
            return;
        }
        denyUnless(event, pocketWorld, event.getPlayer(), WorldAction.INTERACT);
    }

    private void denyUnless(org.bukkit.event.Cancellable event, PocketWorld pocketWorld, Player player, WorldAction action) {
        if (plugin.isBypassing(player.getUniqueId()) || pocketWorld.hasPermission(player.getUniqueId(), action)) {
            return;
        }
        event.setCancelled(true);
        plugin.getMessageReader().sendActionBar("world-permission-denied", player);
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
