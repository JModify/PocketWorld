package me.modify.pocketworld.world.menu.management;

import me.modify.pocketworld.menu.PocketMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class WorldManagementMenu extends PocketMenu {

    public WorldManagementMenu(Player player) {
        super(player);
    }

    @Override
    public String getMenuName() {
        return null;
    }

    @Override
    public int getMenuSlots() {
        return 0;
    }

    @Override
    public void setMenuItems() {

    }

    @Override
    public void handleMenuClick(InventoryClickEvent e) {

    }
}
