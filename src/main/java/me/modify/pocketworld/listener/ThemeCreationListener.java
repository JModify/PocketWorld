package me.modify.pocketworld.listener;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.ui.theme_menus.EnterThemeNameMenu;
import me.modify.pocketworld.ui.theme_menus.SelectBiomeMenu;
import me.modify.pocketworld.ui.theme_menus.SelectIconMenu;
import me.modify.pocketworld.theme.creation.ThemeCreationController;
import me.modify.pocketworld.theme.creation.ThemeCreationRegistry;
import me.modify.pocketworld.util.ColorFormat;
import me.modify.pocketworld.ui.PocketItem;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class ThemeCreationListener implements Listener {

    private final PocketWorldPlugin plugin;
    public ThemeCreationListener(PocketWorldPlugin plugin) {
        this.plugin = plugin;
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
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!ThemeCreationRegistry.getInstance().containsUser(player.getUniqueId())) {
            return;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getItemMeta() == null) {
            return;
        }

        if  (PocketItem.hasTag(plugin, itemInHand, "is-name-entry")) {
            EnterThemeNameMenu enterThemeNameMenu = new EnterThemeNameMenu(player, plugin);
            enterThemeNameMenu.open();
        } else if (PocketItem.hasTag(plugin, itemInHand, "is-biome-select")) {
            event.setCancelled(true);
            SelectBiomeMenu selectBiomeMenu = new SelectBiomeMenu(player, plugin);
            selectBiomeMenu.open();
        }  else if (PocketItem.hasTag(plugin, itemInHand, "is-icon-select")) {
            event.setCancelled(true);
            SelectIconMenu selectIconMenu = new SelectIconMenu(player, plugin);
            selectIconMenu.open();
        } else if (PocketItem.hasTag(plugin, itemInHand, "is-theme-complete")) {
            ThemeCreationRegistry registry = ThemeCreationRegistry.getInstance();
            ThemeCreationController controller = registry.getController(player.getUniqueId());
            controller.nextState();
        } else if (PocketItem.hasTag(plugin, itemInHand, "is-spawn-point")) {
            Location location = player.getLocation();
            String formattedLocation = String.format("%f:%f:%f:%f:%f", location.getX(),
                    location.getY(), location.getZ(), location.getYaw(), location.getPitch());
            ThemeCreationRegistry registry = ThemeCreationRegistry.getInstance();
            ThemeCreationController controller = registry.getController(player.getUniqueId());
            controller.setSpawnPoint(formattedLocation);

            player.sendMessage(ColorFormat.format("&2&lSUCCESS &r&aDefault spawn point for this theme has been set"));
            controller.nextState();
        } else if (PocketItem.hasTag(plugin, itemInHand, "is-cancel-theme")) {
            ThemeCreationRegistry registry = ThemeCreationRegistry.getInstance();
            ThemeCreationController controller = registry.getController(player.getUniqueId());
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

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        ThemeCreationRegistry registry = ThemeCreationRegistry.getInstance();

        if (!registry.containsUser(player.getUniqueId())) {
            return;
        }

        String message = event.getMessage();
        System.out.println(message);
        event.setCancelled(true);
        ThemeCreationController controller = registry.getController(player.getUniqueId());
        controller.handleChatInput(message);
    }

}
