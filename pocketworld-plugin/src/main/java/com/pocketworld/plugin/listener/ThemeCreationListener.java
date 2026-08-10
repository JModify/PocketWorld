package com.pocketworld.plugin.listener;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.theme.creation.ThemeCreationController;
import com.pocketworld.plugin.theme.creation.ThemeCreationRegistry;
import com.pocketworld.plugin.ui.PocketItem;
import com.pocketworld.plugin.ui.theme_menus.SelectBiomeMenu;
import com.pocketworld.plugin.ui.theme_menus.SelectIconMenu;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
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
            plugin.getDebugger().info("[ThemeCreationListener] Cancelling theme creation for "
                    + player.getName() + " - disconnected.");
            ThemeCreationController controller = registry.getController(player.getUniqueId());
            controller.cancelCreation();
            registry.removeByUser(player.getUniqueId());
        }
    }

    /**
     * Every stage item here is meant to be activated with a deliberate right-click, matching their
     * lore ("Right click to select theme biome", etc.) - so only {@code RIGHT_CLICK_AIR}/{@code
     * RIGHT_CLICK_BLOCK} are handled. Previously this ran for every {@link PlayerInteractEvent}
     * regardless of action, including {@code LEFT_CLICK_AIR}/{@code LEFT_CLICK_BLOCK} - live testing
     * showed pressing Q to attempt (and correctly have blocked) a drop of the cancel item also fires
     * an incidental interact packet, which this unfiltered handler was treating as "clicked cancel",
     * tearing the player out of theme creation entirely as a side effect of a blocked drop.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

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
            plugin.getDebugger().info("[ThemeCreationListener] Cancelling theme creation for "
                    + player.getName() + " - clicked cancel item (action: " + event.getAction() + ").");
            ThemeCreationRegistry.getInstance().getController(player.getUniqueId()).cancelCreation();
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        ThemeCreationRegistry registry = ThemeCreationRegistry.getInstance();

        if (!registry.containsUser(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        registry.getController(player.getUniqueId()).handleChatInput(message);
    }
}
