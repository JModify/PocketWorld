package com.pocketworld.plugin.listener;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.ui.PocketItem;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Any item PocketWorld hands a player directly (theme-creation stage icons, and anything similar
 * in the future) is tagged via {@link PocketItem}'s PDC mechanism and must never leave their
 * inventory except through the plugin's own flow - losing one (dropped, deleted via creative
 * mode's drag-out-of-window gesture, or placed into a container) otherwise strands the player
 * mid-stage with no way to continue or cleanly cancel.
 * <p>
 * Deliberately blanket: any inventory interaction involving a tagged item is cancelled, everywhere,
 * not just during a tracked "stage." Every {@code PocketMenu} implementation already cancels its
 * own clicks as the first line of {@code handleMenuClick} - this is a safety net on top of that,
 * not a conflict with it, and there's no current or planned case where a tagged item is meant to
 * actually move slots rather than just trigger an action.
 */
public class ProtectedItemListener implements Listener {

    /** How long after a blocked drop an interact event for the same player is still treated as
     *  that drop's echo rather than a deliberate click - see {@link #wasDropJustBlocked}. */
    private static final long DROP_ECHO_WINDOW_MILLIS = 250;

    private final PocketWorldPlugin plugin;
    private final Map<UUID, Long> recentlyBlockedDrops = new HashMap<>();

    public ProtectedItemListener(PocketWorldPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * {@code HIGHEST} (rather than the implicit {@code NORMAL}) so nothing else can run after this
     * and undo the cancellation. Deliberately relies on {@code setCancelled(true)} alone - an
     * earlier version of this method also called {@code event.getItemDrop().remove()} as extra
     * "belt-and-suspenders" hardening, but that made things actively worse: manually removing the
     * entity apparently fights with Paper's own cancel-drop restoration, and was observed (live,
     * in-game) to both fail to keep the item AND tear the player out of theme creation entirely.
     * {@code setCancelled(true)} alone is the standard, well-tested way to block a drop - no need to
     * second-guess it further.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDrop(PlayerDropItemEvent event) {
        if (isTagged(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            recentlyBlockedDrops.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
            plugin.getDebugger().info("[ProtectedItemListener] Blocked drop of tagged item ("
                    + event.getItemDrop().getItemStack().getType() + ") for " + event.getPlayer().getName());
        }
    }

    /**
     * Whether {@code playerId}'s attempt to drop a tagged item was blocked within the last
     * {@link #DROP_ECHO_WINDOW_MILLIS}ms - confirmed live: pressing the drop key on a tagged item
     * also produces an arm-throw animation that surfaces as its own {@code PlayerInteractEvent},
     * indistinguishable by action type alone from a deliberate click on the same item. Callers that
     * react to clicking a tagged item (like theme-creation's cancel button) need to ignore that echo
     * rather than treat it as the player having clicked to confirm something.
     */
    public boolean wasDropJustBlocked(UUID playerId) {
        Long blockedAt = recentlyBlockedDrops.get(playerId);
        return blockedAt != null && System.currentTimeMillis() - blockedAt <= DROP_ECHO_WINDOW_MILLIS;
    }

    /** Covers creative mode's "drag out of the window to delete" gesture (no PlayerDropItemEvent
     *  fires for that - it's an InventoryClickEvent with a null clicked inventory) and placing a
     *  tagged item into any container. */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (isTagged(event.getCursor()) || isTagged(event.getCurrentItem())) {
            event.setCancelled(true);
        }
    }

    /** Belt-and-suspenders for stage items that happen to be real, placeable blocks (BARRIER,
     *  LIME_WOOL, ...): {@code ThemeCreationListener} is responsible for cancelling the
     *  {@code PlayerInteractEvent} that would otherwise lead to this, but this catches it directly
     *  too in case any future tagged item reaches placement through a path that listener doesn't
     *  cover (dispensers, other menus, etc.). */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isTagged(event.getItemInHand())) {
            event.setCancelled(true);
        }
    }

    private boolean isTagged(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || item.getItemMeta() == null) {
            return false;
        }
        return PocketItem.getTag(plugin, item) != null;
    }
}
