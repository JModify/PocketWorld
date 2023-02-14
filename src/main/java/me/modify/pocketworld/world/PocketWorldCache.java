package me.modify.pocketworld.world;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.data.DAO;

import java.util.*;

/**
 * PocketWorldCache represents the set of pocket world's which are cached in memory.
 * Whether a pocket world is loaded or not is now a separate component to whether it is cached and is
 * handled internally within the PocketWorld object.
 */
public class PocketWorldCache {

    private PocketWorldPlugin plugin;
    private Map<UUID, PocketWorld> worlds;

    public PocketWorldCache(PocketWorldPlugin plugin) {
        this.plugin = plugin;
        this.worlds = new HashMap<>();
    }

    /**
     * Determines if this cache contains a world under the provided id.
     * @param worldId id to check if cache contains this world.
     * @return true if cache contains, else false.
     */
    public boolean contains(UUID worldId) {
        return worlds.containsKey(worldId);
    }

    /**
     * Retrieves the pocket world under the given ID.
     * If the world exists in the cache, it is simply returned.
     * <p>
     * If the world does NOT exist in the cache:
     *   1. It is retrieved from the data source
     *   2. It is stored in the cache for later use.
     *   3. It is returned to the user.
     * @param worldId id of world to get
     * @return the pocket world under the given id
     */
    public PocketWorld get(UUID worldId) {
        if (contains(worldId)) {
            return worlds.get(worldId);
        }

        return pull(worldId);
    }

    /**
     * Retrieves a pocket world under the given ID directly from the data source.
     * @param worldId id of world to retrieve
     * @return world under the given id OR null if such a world does not exist in data source.
     */
    private PocketWorld pull(UUID worldId) {
        DAO dao = plugin.getDataSource().getConnection().getDAO();
        PocketWorld world = dao.getPocketWorld(worldId);

        if (world == null) {
            return null;
        }

        worlds.put(world.getId(), world);
        return world;
    }

    /**
     * Pushes a provided pocket world to the data source (updates it).
     * If the world does not exist in this cache, nothing will be done.
     * It should be assumed that a world is retrieved using "get" method before using push() on it.
     * @param worldId id of world to push
     */
    public void push(UUID worldId) {
        PocketWorld world = worlds.get(worldId);
        if (world == null) return;

        DAO dao = plugin.getDataSource().getConnection().getDAO();
        dao.updatePocketWorld(world);
        remove(worldId);
    }

    public void pushAll() {

    }

    /**
     * Adds a world into this cache.
     * Does NOT add a world to the data source, that should be done directly via DAO.
     * @param world
     */
    public void add(PocketWorld world) {
        worlds.put(world.getId(), world);
    }

    /**
     * Removes a world from this cache if it contains it.
     * Does NOT delete a world from data source, that should be done directly via DAO.
     * @param worldId id of world to delete from cache.
     */
    public void remove(UUID worldId) {
        worlds.remove(worldId);
    }

    /**
     * Shutdown method for this cache.
     * Updated all worlds contained in the cache to data source.
     */
    public void shutdown() {
        DAO dao = plugin.getDataSource().getConnection().getDAO();

        for (PocketWorld world : worlds.values()) {
            if (world.isLoaded()) {
                world.unload(plugin, true);
            }
        }

        worlds.values().forEach(dao::updatePocketWorld);
        worlds.clear();
    }
}
