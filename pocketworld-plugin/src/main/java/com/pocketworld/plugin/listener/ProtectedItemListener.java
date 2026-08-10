package com.pocketworld.plugin.listener;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.ui.PocketItem;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent event) {
        if (isTagged(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
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

    private boolean isTagged(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || item.getItemMeta() == null) {
            return false;
        }
        return PocketItem.getTag(plugin, item) != null;
    }
}
