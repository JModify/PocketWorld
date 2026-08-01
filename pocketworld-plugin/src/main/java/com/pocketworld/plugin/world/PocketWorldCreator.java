package com.pocketworld.plugin.world;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.api.event.PocketWorldCreateEvent;
import com.pocketworld.plugin.theme.PocketTheme;
import org.bukkit.Bukkit;
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
                new HashMap<>(), theme.getBiome(), PocketWorld.DEFAULT_WORLD_SIZE, spawn, true, true, true, false);
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

        int position = plugin.getCreationQueue().enqueue(onComplete ->
                createNow(plugin, world, theme, creatorId, worldId, themeId, onComplete));

        if (position > 0) {
            Player creator = Bukkit.getPlayer(creatorId);
            if (creator != null) {
                plugin.getMessageReader().send("world-creation-queued", creator, "{POSITION}:" + position);
            }
        }
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
                int dataVersion = plugin.getRuntime().prepareLoad(worldId);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        World bWorld = plugin.getRuntime().activate(worldId, dataVersion, world.toWorldProperties(plugin));
                        long time = System.currentTimeMillis() - start;

                        world.setWorldBorder();
                        bWorld.setSpawnLocation(world.getWorldSpawn().getBukkitLocation(bWorld));
                        world.setLoaded(true);
                        Bukkit.getPluginManager().callEvent(new PocketWorldCreateEvent(world, bWorld, creatorId));

                        Player creator = Bukkit.getPlayer(creatorId);
                        if (creator != null) {
                            plugin.getMessageReader().send("world-creation-complete", creator, "{TIME}:" + time);

                            // Bukkit.createWorld() (called by activate() above) synchronously
                            // prepares the spawn area before returning, so the world is already
                            // ready to receive players by this point - no artificial delay needed.
                            world.teleport(creator);
                        }

                        plugin.getLogger().info("Successfully created pocket world " + world.getId() + " in " + time + "ms!");
                    } catch (IOException e) {
                        plugin.getLogger().severe("Failed to activate newly created pocket world " + worldId + ": " + e);
                    } finally {
                        onComplete.run();
                    }
                });
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create pocket world " + worldId + ": " + e);
                onComplete.run();
            }
        });
    }
}
