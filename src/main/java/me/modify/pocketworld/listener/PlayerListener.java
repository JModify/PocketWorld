package me.modify.pocketworld.listener;

import com.mongodb.client.model.Updates;
import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.data.DAO;
import me.modify.pocketworld.user.PocketUser;
import me.modify.pocketworld.user.PocketUserInventory;
import me.modify.pocketworld.world.PocketWorld;
import me.modify.pocketworld.world.PocketWorldCache;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

import java.util.Set;
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

        // If the user already exists in the database, just update their name on login.
        if (userExists) {
            dao.updatePocketUser(userId, Updates.set("name", userName));
        }
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        DAO dao = plugin.getDataSource().getConnection().getDAO();
        PocketUser user = dao.getPocketUser(player.getUniqueId());

        Set<UUID> worldIds = user.getWorlds();
        if (worldIds.isEmpty()) {
            plugin.getLogger().severe("World ids empty");
            return;
        }

        PocketWorldCache cache = plugin.getWorldCache();
        for (UUID id : worldIds) {
            if (!cache.contains(id)) {
                plugin.getLogger().severe("Cache does not contain this world." + id.toString());
                continue;
            }

            PocketWorld world = cache.get(id);
            Set<UUID> users = world.getUsers().keySet();
            if (!users.contains(player.getUniqueId())) {
                plugin.getLogger().severe("User not contained in world user list. " + id.toString());
                continue;
            }

            int count = (int) users.stream().filter(u -> Bukkit.getPlayer(u) != null).count();

            if (count > 1) {
                plugin.getLogger().severe("More than 1 online users, skipping " + id.toString());
                continue;
            }

            plugin.getLogger().severe("Successfully pushed " + id.toString());
            cache.push(id);

            if (world.isLoaded()) {
                plugin.getLogger().severe("Successfully unloaded " + id.toString());
                world.unload(plugin, true);
            }
        }

    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        PocketUserInventory.restoreUserInventory(plugin, player);

    }
}
