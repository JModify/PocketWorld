package com.pocketworld.plugin.listener;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.theme.creation.ThemeCreationController;
import com.pocketworld.plugin.theme.creation.ThemeCreationRegistry;
import com.pocketworld.plugin.ui.PocketItem;
import com.pocketworld.plugin.ui.theme_menus.SelectBiomeMenu;
import com.pocketworld.plugin.ui.theme_menus.SelectIconMenu;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
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

        if (PocketItem.hasTag(plugin, itemInHand, "is-biome-select")) {
            event.setCancelled(true);
            new SelectBiomeMenu(player, plugin).open();
        } else if (PocketItem.hasTag(plugin, itemInHand, "is-icon-select")) {
            event.setCancelled(true);
            new SelectIconMenu(player, plugin).open();
        } else if (PocketItem.hasTag(plugin, itemInHand, "is-theme-complete")) {
            ThemeCreationRegistry.getInstance().getController(player.getUniqueId()).nextState();
        } else if (PocketItem.hasTag(plugin, itemInHand, "is-spawn-point")) {
            Location location = player.getLocation();
            String formattedLocation = String.format("%f:%f:%f:%f:%f", location.getX(),
                    location.getY(), location.getZ(), location.getYaw(), location.getPitch());
            ThemeCreationController controller = ThemeCreationRegistry.getInstance().getController(player.getUniqueId());
            controller.setSpawnPoint(formattedLocation);

            plugin.getMessageReader().send("theme-spawn-set", player);
            controller.nextState();
        } else if (PocketItem.hasTag(plugin, itemInHand, "is-cancel-theme")) {
            ThemeCreationRegistry.getInstance().getController(player.getUniqueId()).cancelCreation();
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        ThemeCreationRegistry registry = ThemeCreationRegistry.getInstance();

        if (!registry.containsUser(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        registry.getController(player.getUniqueId()).handleChatInput(event.getMessage());
    }
}
