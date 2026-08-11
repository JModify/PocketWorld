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
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.Set;

public class ThemeCreationListener implements Listener {

    /** Every label (and alias) PocketWorld's three commands are registered under in plugin.yml -
     *  see {@link #onCommandPreprocess}. */
    private static final Set<String> POCKETWORLD_COMMAND_LABELS =
            Set.of("pocketworld", "pw", "pocketworldadmin", "pwa", "theme");

    private final PocketWorldPlugin plugin;

    public ThemeCreationListener(PocketWorldPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Blocks every PocketWorld command (any label/alias) for a player mid-theme-creation, at the
     * single point every one of them passes through regardless of which command class ends up
     * handling it - simpler and more robust than each of the three CommandExecutors independently
     * checking {@link ThemeCreationRegistry#containsUser}, and automatically covers aliases (/pw,
     * /pwa) without needing to enumerate them per-executor.
     */
    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!ThemeCreationRegistry.getInstance().containsUser(player.getUniqueId())) {
            return;
        }

        String label = event.getMessage().substring(1).split(" ", 2)[0].toLowerCase(Locale.ROOT);
        if (!POCKETWORLD_COMMAND_LABELS.contains(label)) {
            return;
        }

        event.setCancelled(true);
        plugin.getMessageReader().send("theme-creation-blocks-commands", player);
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
     * RIGHT_CLICK_BLOCK} are handled. {@code LEFT_CLICK_AIR}/{@code LEFT_CLICK_BLOCK} (punching while
     * holding a tagged item) and {@code PHYSICAL} (stepping on a pressure plate) deliberately do
     * nothing - the arm still swings as normal, but no theme action fires. This also happens to be
     * what makes {@link PlayerDropItemEvent}-blocking and this handler independent of each other:
     * pressing the drop key on a tagged item produces an arm-throw animation that surfaces as its own
     * {@code PlayerInteractEvent} with a {@code LEFT_CLICK_*} action, so as long as only right-clicks
     * are handled here, {@link ProtectedItemListener} blocking the drop and this handler reacting to
     * clicks never have to coordinate - no timing/state needed to tell one from the other.
     * <p>
     * Every branch below must also call {@code event.setCancelled(true)} - three of them (theme
     * complete, spawn point, cancel) previously didn't. Since their items are all real, placeable/
     * throwable vanilla items (LIME_WOOL, ENDER_EYE, BARRIER), an uncancelled right-click let the
     * normal vanilla action proceed alongside the plugin's own logic - e.g. right-clicking the cancel
     * item against a block correctly cancelled theme creation AND placed the barrier as a real block,
     * since nothing stopped the placement half of that same interaction.
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
            event.setCancelled(true);
            ThemeCreationRegistry.getInstance().getController(player.getUniqueId()).nextState();
        } else if (PocketItem.hasTag(plugin, itemInHand, "is-spawn-point")) {
            event.setCancelled(true);
            Location location = player.getLocation();
            String formattedLocation = String.format("%f:%f:%f:%f:%f", location.getX(),
                    location.getY(), location.getZ(), location.getYaw(), location.getPitch());
            ThemeCreationController controller = ThemeCreationRegistry.getInstance().getController(player.getUniqueId());
            controller.setSpawnPoint(formattedLocation);

            plugin.getMessageReader().send("theme-spawn-set", player);
            controller.nextState();
        } else if (PocketItem.hasTag(plugin, itemInHand, "is-cancel-theme")) {
            event.setCancelled(true);
            plugin.getDebugger().info("[ThemeCreationListener] Cancelling theme creation for "
                    + player.getName() + " - clicked cancel item (action: " + event.getAction() + ").");
            ThemeCreationRegistry.getInstance().getController(player.getUniqueId()).cancelCreation();
        }
    }

    /**
     * If a player mid-creation is teleported out of the editor world by anything other than the
     * plugin's own cancel/complete flow (another plugin, a command, an ender pearl, ...), theme
     * creation is cancelled to match - there's nothing useful left to track otherwise. Deliberately
     * uses {@link ThemeCreationController#isEnding()} to skip the case where this event fires
     * because {@code cancelCreation()}/{@code completeCreation()} itself is the one doing the
     * teleporting (both happen to leave the editor world as part of normal, successful cleanup) -
     * without that check this would call {@code cancelCreation()} reentrantly on every cancel or
     * completion.
     */
    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        ThemeCreationRegistry registry = ThemeCreationRegistry.getInstance();

        if (!registry.containsUser(player.getUniqueId())) {
            return;
        }

        ThemeCreationController controller = registry.getController(player.getUniqueId());
        if (controller.isEnding() || !controller.isPhysicallyInEditorWorld()) {
            return;
        }

        plugin.getDebugger().info("[ThemeCreationListener] Cancelling theme creation for "
                + player.getName() + " - left the editor world.");
        controller.cancelCreation();
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
