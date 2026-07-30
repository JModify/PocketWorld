package com.pocketworld.plugin.listener;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.data.DAO;
import com.pocketworld.plugin.user.PocketUserInventory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerListener implements Listener {

    private final PocketWorldPlugin plugin;
    private final WorldAutoUnloadTracker autoUnloadTracker;

    public PlayerListener(PocketWorldPlugin plugin, WorldAutoUnloadTracker autoUnloadTracker) {
        this.plugin = plugin;
        this.autoUnloadTracker = autoUnloadTracker;
    }

    /** Runs off the main thread already (per AsyncPlayerPreLoginEvent's own contract) - safe to block here. */
    @EventHandler
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID userId = event.getUniqueId();
        String userName = event.getName();
        DAO dao = plugin.getDataSource().getConnection().getDAO();
        dao.registerPocketUser(userId, userName);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PocketUserInventory.restoreUserInventory(plugin, player);
        plugin.getUserCache().handleConnection(player);
        autoUnloadTracker.onPlayerJoin(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getUserCache().handleDisconnection(event.getPlayer());
        autoUnloadTracker.onPlayerQuit();
    }
}
