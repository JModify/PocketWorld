package me.modify.pocketworld.cache;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.data.DAO;
import me.modify.pocketworld.user.PocketUser;
import me.modify.pocketworld.world.PocketWorld;
import org.bukkit.entity.Player;

import java.util.UUID;

public class UserCache extends PocketCache<PocketUser> {

    public UserCache(PocketWorldPlugin plugin) {
        super(plugin);
    }

    @Override
    protected PocketUser get(UUID userId) {
        DAO dao = plugin.getDataSource().getConnection().getDAO();
        PocketUser user = dao.getPocketUser(userId);

        if (user == null) {
            return null;
        }

        add(userId, user);
        return user;
    }

    @Override
    public void flush(UUID userId) {
        PocketUser user = cache.get(userId);
        if (user == null) return;

        DAO dao = plugin.getDataSource().getConnection().getDAO();
        dao.updatePocketUser(user);
        remove(userId);
    }

    @Override
    public void flush() {
        DAO dao = plugin.getDataSource().getConnection().getDAO();
        for (PocketUser user : cache.values()) {
            dao.updatePocketUser(user);
        }
        cache.clear();
    }

    /**
     * Handles a player connection.
     * Upon player connection the cache will retrieve the user's data from data source and cache it in this class.
     * If the is already cached, this method will do nothing.
     * @param player player joining the server.
     */
    public void handleConnection(Player player) {
        PocketUser user = readThrough(player.getUniqueId());
        user.setName(player.getName());
    }

    /**
     * Handles a player disconnection.
     * Upon player disconnection, the cache will push the user's data to the data source and delete it from the cache.
     * @param player player disconnecting from server.
     */
    public void handleDisconnection(Player player) {
        flush(player.getUniqueId());
    }
}
