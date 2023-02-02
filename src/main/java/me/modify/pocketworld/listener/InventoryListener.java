package me.modify.pocketworld.listener;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.menu.PocketMenu;
import me.modify.pocketworld.theme.creation.menus.SelectIconMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class InventoryListener implements Listener {

    private final PocketWorldPlugin plugin;
    public InventoryListener(PocketWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player p = (Player) event.getWhoClicked();

        if (event.getClickedInventory() == null)  {
            return;
        }

        // Cancels shift clicking into pocket menu's
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            InventoryHolder topInventory = event.getView().getTopInventory().getHolder();
            if (topInventory instanceof PocketMenu) {
                event.setCancelled(true);
            }
        }

        InventoryHolder holder = event.getClickedInventory().getHolder();

        if(holder instanceof PocketMenu menu){
            menu.handleMenuClick(event);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory inventory = event.getInventory();

        // Cancels dragging items into pocket menu's
        if (inventory.getHolder() instanceof PocketMenu menu) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();

        if (inventory.getHolder() instanceof SelectIconMenu menu) {
            menu.handleMenuClose(event);
        }
    }




}
