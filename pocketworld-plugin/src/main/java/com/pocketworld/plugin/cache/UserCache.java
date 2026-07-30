package com.pocketworld.plugin.cache;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.data.DAO;
import com.pocketworld.plugin.user.PocketUser;
import org.bukkit.Bukkit;
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
        if (user == null) {
            return;
        }

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

    /** On join: reach-through loads (or creates) the user's data and refreshes their cached name. */
    public void handleConnection(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PocketUser user = readThrough(player.getUniqueId());
            user.setName(player.getName());
        });
    }

    /** On quit: pushes the user's data to the data source and evicts it from the cache. */
    public void handleDisconnection(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> flush(player.getUniqueId()));
    }
}
