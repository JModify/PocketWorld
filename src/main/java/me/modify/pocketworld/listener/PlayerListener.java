package me.modify.pocketworld.listener;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.theme.creation.ThemeCreationController;
import me.modify.pocketworld.menu.thememenus.EnterThemeNameMenu;
import me.modify.pocketworld.theme.creation.ThemeCreationRegistry;
import me.modify.pocketworld.menu.thememenus.SelectBiomeMenu;
import me.modify.pocketworld.menu.thememenus.SelectIconMenu;
import me.modify.pocketworld.util.PocketItem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener {

    private final PocketWorldPlugin plugin;
    public PlayerListener(PocketWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        plugin.getDataSource().getConnection().getDAO().registerPocketUser(event.getUniqueId());
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ThemeCreationRegistry registry = ThemeCreationRegistry.getInstance();

        if (registry.containsUser(player.getUniqueId())) {
            ThemeCreationController controller = registry.getController(player.getUniqueId());
            controller.cancelCreation();
            registry.removeByUser(player.getUniqueId());
        }

        //TODO: If player is last of a pocket world to leave server, unload pocket world and remove from registry
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ItemStack[] inventory = plugin.getDataSource().getConnection().getDAO()
                .retrieveUserInventory(player.getUniqueId());
        if (inventory != null) {
            player.getInventory().setContents(inventory);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {

        if (!ThemeCreationRegistry.getInstance().containsUser(event.getPlayer().getUniqueId())) {
            return;
        }

        ItemStack itemInHand = event.getPlayer().getInventory().getItemInMainHand();
        if (itemInHand.getItemMeta() == null) {
            return;
        }

        if  (PocketItem.hasTag(plugin, itemInHand, "is-name-entry")) {
            EnterThemeNameMenu enterThemeNameMenu = new EnterThemeNameMenu(event.getPlayer(), plugin);
            enterThemeNameMenu.open();
        } else if (PocketItem.hasTag(plugin, itemInHand, "is-biome-select")) {
            event.setCancelled(true);
            SelectBiomeMenu selectBiomeMenu = new SelectBiomeMenu(event.getPlayer(), plugin);
            selectBiomeMenu.open();
        }  else if (PocketItem.hasTag(plugin, itemInHand, "is-icon-select")) {
            event.setCancelled(true);
            SelectIconMenu selectIconMenu = new SelectIconMenu(event.getPlayer(), plugin);
            selectIconMenu.open();
        } else if (PocketItem.hasTag(plugin, itemInHand, "is-theme-complete")) {
            ThemeCreationRegistry registry = ThemeCreationRegistry.getInstance();
            ThemeCreationController controller = registry.getController(event.getPlayer().getUniqueId());
            controller.nextState();
        } else if (PocketItem.hasTag(plugin, itemInHand, "is-cancel-theme")) {
            ThemeCreationRegistry registry = ThemeCreationRegistry.getInstance();
            ThemeCreationController controller = registry.getController(event.getPlayer().getUniqueId());
            controller.cancelCreation();
        }
    }

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();

        if (PocketItem.hasAnyTags(plugin, item, "is-name-entry","is-biome-select", "is-icon-select", "is-theme-complete", "is-cancel-theme")) {
            event.setCancelled(true);
        }
    }
}
