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

    private final PocketWorldPlugin plugin;

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
            plugin.getDebugger().info("[ProtectedItemListener] Blocked drop of tagged item ("
                    + event.getItemDrop().getItemStack().getType() + ") for " + event.getPlayer().getName());
        }
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
