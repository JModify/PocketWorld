package com.pocketworld.plugin.ui;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.util.ColorFormat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public abstract class PocketMenu implements InventoryHolder {

    protected final Player player;
    protected final PocketWorldPlugin plugin;

    private Inventory inventory;

    public PocketMenu(Player player, PocketWorldPlugin plugin) {
        this.player = player;
        this.plugin = plugin;
    }

    /** Creates the inventory, populates it, and opens it for the player. */
    public void open() {
        inventory = Bukkit.createInventory(this, getMenuSlots(), ColorFormat.format(getMenuName()));
        setMenuItems();
        player.openInventory(inventory);
    }

    public abstract String getMenuName();

    public abstract int getMenuSlots();

    public abstract void setMenuItems();

    /** Handles a click inside this menu - dispatched here by {@code InventoryListener}. */
    public abstract void handleMenuClick(InventoryClickEvent e);

    public void addFillers(ItemStack fillerItem) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack is = inventory.getItem(i);
            if (is == null || is.getType() == Material.AIR) {
                inventory.setItem(i, fillerItem);
            }
        }
    }

    public void addFillers(ItemStack fillerItem, int rangeMin, int rangeMax) {
        for (int i = rangeMin; i <= rangeMax && i < inventory.getSize(); i++) {
            ItemStack is = inventory.getItem(i);
            if (is == null || is.getType() == Material.AIR) {
                inventory.setItem(i, fillerItem);
            }
        }
    }

    public void addFillerBorder(ItemStack fillerItem) {
        int slots = getMenuSlots();
        if (slots < 27) {
            return;
        }

        addFillers(fillerItem, 0, 8);
        addFillers(fillerItem, (slots - 1) - 9, slots - 1);

        for (int i = 9; i < (slots - 9); i += 9) {
            ItemStack is = inventory.getItem(i);
            if (is == null || is.getType() == Material.AIR) {
                inventory.setItem(i, fillerItem);
            }
        }

        for (int j = 17; j < slots - 9; j += 9) {
            ItemStack is = inventory.getItem(j);
            if (is == null || is.getType() == Material.AIR) {
                inventory.setItem(j, fillerItem);
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
