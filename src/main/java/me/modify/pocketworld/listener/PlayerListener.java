package me.modify.pocketworld.listener;

import com.mongodb.client.model.Updates;
import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.data.DAO;
import me.modify.pocketworld.theme.creation.ThemeCreationController;
import me.modify.pocketworld.menu.thememenus.EnterThemeNameMenu;
import me.modify.pocketworld.theme.creation.ThemeCreationRegistry;
import me.modify.pocketworld.menu.thememenus.SelectBiomeMenu;
import me.modify.pocketworld.menu.thememenus.SelectIconMenu;
import me.modify.pocketworld.user.UserInventory;
import me.modify.pocketworld.util.PocketItem;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

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
        if (!userExists) {
            dao.updatePocketUser(userId, Updates.set("name", userName));
        }
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent event) {
        //TODO: If player is last of a pocket world to leave server, unload pocket world and remove from registry
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        UserInventory.restoreUserInventory(plugin, player);

    }
}
