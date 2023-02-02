package me.modify.pocketworld.menu;

import org.bukkit.entity.Player;

public abstract class PocketPaginatedMenu extends PocketMenu {

    protected int page;
    protected final int maxItemsPerPage = 28;
    protected int index = 0;

    public PocketPaginatedMenu(Player player) {
        super(player);
    }

    @Override
    public int getMenuSlots() {
        return 54;
    }

    /**
     * Add border for this paginated inventory.
     * This border surrounds the square of items in the menu.
     */
    public abstract void addMenuBorder();
}
