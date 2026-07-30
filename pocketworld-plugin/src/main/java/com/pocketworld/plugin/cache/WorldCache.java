package com.pocketworld.plugin.cache;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.data.DAO;
import com.pocketworld.plugin.world.PocketWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The set of PocketWorlds cached in memory. Whether a world is loaded is a separate concern from
 * whether it's cached, and is tracked internally on the {@link PocketWorld} object itself.
 * <p>
 * Unlike the original implementation, this class does not attempt to decide WHEN a world should be
 * unloaded on player disconnect - that policy (with a grace period, so a quick reconnect doesn't
 * cause a needless unload/reload) lives in the listener package, which is the only place with
 * enough context (all online players, not just the one cache) to make that call safely.
 */
public class WorldCache extends PocketCache<PocketWorld> {

    public WorldCache(PocketWorldPlugin plugin) {
        super(plugin);
    }

    @Override
    protected PocketWorld get(UUID worldId) {
        DAO dao = plugin.getDataSource().getConnection().getDAO();
        PocketWorld world = dao.getPocketWorld(worldId);

        if (world == null) {
            return null;
        }

        add(world.getId(), world);
        return world;
    }

    @Override
    public void flush(UUID worldId) {
        PocketWorld world = cache.get(worldId);
        if (world == null) {
            return;
        }

        DAO dao = plugin.getDataSource().getConnection().getDAO();
        dao.updatePocketWorld(world);
        remove(worldId);
    }

    @Override
    public void flush() {
        DAO dao = plugin.getDataSource().getConnection().getDAO();

        for (PocketWorld world : cache.values()) {
            if (world.isLoaded()) {
                world.unload(plugin, true);
            }
            dao.updatePocketWorld(world);
        }
        cache.clear();
    }

    /**
     * Every cached-and-currently-loaded world. Since a loaded world is always added to this cache
     * first (see {@link PocketWorld#load} / {@link com.pocketworld.plugin.world.PocketWorldCreator}),
     * this is a complete list of every live pocket world on the server - used by the auto-unload
     * tracker so it doesn't need to consult any particular player's world membership to find worlds
     * that may have just gone empty.
     */
    public List<PocketWorld> getLoadedWorlds() {
        List<PocketWorld> loaded = new ArrayList<>();
        for (PocketWorld world : cache.values()) {
            if (world.isLoaded()) {
                loaded.add(world);
            }
        }
        return loaded;
    }
}
