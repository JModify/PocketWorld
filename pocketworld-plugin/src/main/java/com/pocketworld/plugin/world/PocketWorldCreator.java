package com.pocketworld.plugin.world;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.api.event.PocketWorldCreateEvent;
import com.pocketworld.plugin.theme.PocketTheme;
import com.pocketworld.plugin.util.ChunkPrewarmer;
import com.pocketworld.slime.format.SlimeFormatException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

/**
 * Creates new {@link PocketWorld}s from a {@link PocketTheme}. The theme's stored world is cloned,
 * at the storage layer, into the world runtime's own storage under a fresh world id - no decode/
 * re-encode round trip needed, since both runtimes speak the same Slime binary format.
 */
public class PocketWorldCreator {

    private final PocketWorldPlugin plugin;

    public PocketWorldCreator(PocketWorldPlugin plugin) {
        this.plugin = plugin;
    }

    public PocketWorld create(UUID creatorId, String worldName, UUID themeId) {
        PocketTheme theme = plugin.getThemeRegistry().getThemeByID(themeId);
        WorldSpawn spawn = WorldSpawn.fromString(theme.getSpawnPoint());

        PocketWorld world = new PocketWorld(UUID.randomUUID(), worldName, theme.getIcon(), new HashMap<>(),
                new HashMap<>(), theme.getBiome(), PocketWorld.DEFAULT_WORLD_SIZE, spawn, true, true, true, false,
                PocketWorld.defaultPermissions());
        world.getUsers().put(creatorId, WorldRank.OWNER);
        return world;
    }

    /**
     * Clones the theme's stored world into a brand-new PocketWorld and brings it live. Safe to call
     * from any thread: the clone and the runtime's I/O-safe preparation happen off the main thread,
     * and only the final activation step is scheduled back onto it.
     * <p>
     * Goes through the plugin's global {@link com.pocketworld.plugin.runtime.PocketWorldCreationQueue}
     * rather than starting immediately: {@code Bukkit.createWorld()} is unavoidably main-thread-
     * blocking (~100ms per world, measured), so if many players' creations happened to finish their
     * async prepare work around the same moment, all their activate() calls landing on the main
     * thread in the same tick would freeze the whole server for the sum of all of them. Serializing
     * creation server-wide - only one in flight at a time - caps that worst case to one world's cost,
     * at the price of later requests waiting for the current one to finish first.
     */
    public void generateWorldFromTheme(PocketWorldPlugin plugin, PocketWorld world, PocketTheme theme, UUID creatorId) {
        plugin.getWorldCache().add(world.getId(), world);
        plugin.getUserCache().readThrough(creatorId).addWorld(world.getId());

        String worldId = world.getId().toString();
        String themeId = theme.getId().toString();

        if (!plugin.isCreationQueueEnabled()) {
            createNow(plugin, world, theme, creatorId, worldId, themeId, () -> {});
            return;
        }

        int position = plugin.getCreationQueue().enqueue(
                onComplete -> createNow(plugin, world, theme, creatorId, worldId, themeId, delayedComplete(plugin, onComplete)),
                newPosition -> notifyQueuePosition(plugin, creatorId, newPosition));

        if (position > 0) {
            Player creator = Bukkit.getPlayer(creatorId);
            if (creator != null) {
                plugin.getMessageReader().send("world-creation-queued", creator, "{POSITION}:" + position);
            }
        }
    }

    private static void notifyQueuePosition(PocketWorldPlugin plugin, UUID playerId, int position) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            plugin.getMessageReader().sendActionBar("world-queue-position", player, "{POSITION}:" + position);
        }
    }

    /** Wraps the queue's completion callback with an extra configured pause, if any, before the
     *  next queued job is allowed to start - see {@link PocketWorldPlugin#getCreationQueueDelayTicks()}. */
    private static Runnable delayedComplete(PocketWorldPlugin plugin, Runnable onComplete) {
        long delayTicks = plugin.getCreationQueueDelayTicks();
        if (delayTicks <= 0) {
            return onComplete;
        }
        return () -> Bukkit.getScheduler().runTaskLater(plugin, onComplete, delayTicks);
    }

    private void createNow(PocketWorldPlugin plugin, PocketWorld world, PocketTheme theme, UUID creatorId,
                            String worldId, String themeId, Runnable onComplete) {
        long start = System.currentTimeMillis();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (plugin.getRuntime().exists(worldId)) {
                    plugin.getLogger().severe("Attempted world creation for " + worldId + " but this world already exists!");
                    onComplete.run();
                    return;
                }

                plugin.getThemeRuntime().cloneInto(themeId, plugin.getRuntime(), worldId);
                long afterClone = System.currentTimeMillis();
                int dataVersion = plugin.getRuntime().prepareLoad(worldId);
                long afterPrepare = System.currentTimeMillis();
                plugin.getDebugger().info("[PocketWorldCreator] " + worldId + " clone=" + (afterClone - start)
                        + "ms, prepareLoad=" + (afterPrepare - afterClone) + "ms");

                Bukkit.getScheduler().runTask(plugin, () -> {
                    World bWorld;
                    long beforeActivate = System.currentTimeMillis();
                    try {
                        bWorld = plugin.getRuntime().activate(worldId, dataVersion, world.toWorldProperties(plugin));
                    } catch (IOException e) {
                        plugin.getLogger().severe("Failed to activate newly created pocket world " + worldId + ": " + e);
                        onComplete.run();
                        return;
                    }
                    long afterActivate = System.currentTimeMillis();
                    plugin.getDebugger().info("[PocketWorldCreator] " + worldId + " activate=" + (afterActivate - beforeActivate) + "ms");

                    world.setWorldBorder();
                    Location spawnLocation = world.getWorldSpawn().getBukkitLocation(bWorld);
                    bWorld.setSpawnLocation(spawnLocation);

                    // Pre-warm the spawn chunk before world.teleport() below would otherwise force it
                    // to load synchronously - see ChunkPrewarmer's own doc for the Paper/Spigot split.
                    ChunkPrewarmer.prewarm(plugin, bWorld, spawnLocation.getBlockX() >> 4, spawnLocation.getBlockZ() >> 4, () -> {
                        long time = System.currentTimeMillis() - start;
                        plugin.getDebugger().info("[PocketWorldCreator] " + worldId + " chunkTouch="
                                + (System.currentTimeMillis() - afterActivate) + "ms, total=" + time + "ms");
                        world.setLoaded(true);
                        Bukkit.getPluginManager().callEvent(new PocketWorldCreateEvent(world, bWorld, creatorId));

                        Player creator = Bukkit.getPlayer(creatorId);
                        if (creator != null) {
                            plugin.getMessageReader().send("world-creation-complete", creator, "{TIME}:" + time);
                            world.teleport(creator);
                        }

                        plugin.getLogger().info("Successfully created pocket world " + world.getId() + " in " + time + "ms!");
                        onComplete.run();
                    });
                });
            } catch (SlimeFormatException e) {
                // Distinguished from a generic IOException because this specifically means theme
                // "themeId"'s own stored data is corrupted - every future creation from this theme
                // will fail the same way until an admin restores it from a backup (confirmable with
                // /pocketworldadmin validate theme <id>), not just a transient failure for this player.
                plugin.getLogger().severe("Failed to create pocket world " + worldId + ": theme " + themeId
                        + "'s stored data is corrupted (" + e.getMessage() + "). This needs manual recovery, e.g. from a backup.");
                Player creator = Bukkit.getPlayer(creatorId);
                if (creator != null) {
                    plugin.getMessageReader().send("world-creation-corrupted", creator);
                }
                onComplete.run();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create pocket world " + worldId + ": " + e);
                onComplete.run();
            }
        });
    }
}
