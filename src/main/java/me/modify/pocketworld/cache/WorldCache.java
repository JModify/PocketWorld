package me.modify.pocketworld.cache;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.data.DAO;
import me.modify.pocketworld.user.PocketUser;
import me.modify.pocketworld.world.PocketWorld;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

/**
 * PocketWorldCache represents the set of pocket world's which are cached in memory.
 * Whether a pocket world is loaded or not is now a separate component to whether it is cached and is
 * handled internally within the PocketWorld object.
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
        if (world == null) return;

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

    /*
     * Handles the world cache upon a player disconnecting from a server.
     * This algorithm will determine when worlds in the world cache should be unloaded/saved from world cache
     * to data source.
     * <p>
     * To put it simply, if the player disconnecting is the last player online from a
     * world in the world cache, that world should be pushed from the world cache to
     * the data source and unloaded from the server (if it is loaded).
     * @param player disconnecting from server.
     */
    //TODO: Unloading worlds when last player leaves is way too server intensive. Find a better way to push and unload.
    @Deprecated
    public void handleDisconnection(Player player) {
        PocketUser user = plugin.getUserCache().get(player.getUniqueId());

        // If the user is not associated to any pocket worlds, do not proceed further.
        Set<UUID> userWorldReferences = user.getWorlds();
        if (userWorldReferences.isEmpty()) {
            return;
        }

        for (UUID worldId : userWorldReferences) {
            // If this world is not cached, it is assumed that it is not loaded and no changes were made to it.
            if (!contains(worldId)) {
                continue;
            }

            // If the player has a reference to this world, but the world itself does not contain the player as
            // a member, then there has been some error so we should not proceed.
            PocketWorld world = cache.get(worldId);
            Set<UUID> users = world.getUsers().keySet();
            if (!users.contains(player.getUniqueId())) {
                continue;
            }

            // Count the number of online members for this world.
            int onlineMembersCount = (int) users.stream().filter(u -> Bukkit.getPlayer(u) != null).count();

            // If there are more than 1 (the user disconnecting) members online, nothing should be done.
            if (onlineMembersCount > 1) {
                continue;
            }

            // If the player disconnecting was the last member of this world online, push any changes made to the
            // world (via the world cache) to the data source.
            flush(worldId);

            // If the world was loaded, unload and save it when unloading.
            if (world.isLoaded()) {
                world.unload(plugin, true);
            }
        }
    }
}
