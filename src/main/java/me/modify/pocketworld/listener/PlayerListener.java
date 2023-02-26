package me.modify.pocketworld.listener;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.data.DAO;
import me.modify.pocketworld.user.PocketUserInventory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerListener implements Listener {

    private final PocketWorldPlugin plugin;
    public PlayerListener(PocketWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID userId = event.getUniqueId();
        String userName = event.getName();
        DAO dao = plugin.getDataSource().getConnection().getDAO();

        // Attempts to register new pocket user in the database. Return value indicates whether user already existed.
        boolean userExists = dao.registerPocketUser(userId, userName);

/*        // If the user already exists in the database, just update their name on login.
        if (userExists) {
            dao.updatePocketUser(userId, Updates.set("name", userName));
        }*/
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PocketUserInventory.restoreUserInventory(plugin, player);
        plugin.getUserCache().handleConnection(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getUserCache().handleDisconnection(event.getPlayer());
    }


}
