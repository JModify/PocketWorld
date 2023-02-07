package me.modify.pocketworld.menu;

import lombok.NonNull;
import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.util.ColorFormat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public abstract class PocketMenu implements InventoryHolder {

    /** Player opening the menu */
    protected Player player;

    /** Instance of plugin */
    protected PocketWorldPlugin plugin;

    /** Inventory object to create an modify */
    private Inventory inventory;

    /**
     * Create a new instance of a menu
     * @param player player opening the menu
     */
    public PocketMenu(Player player, PocketWorldPlugin plugin) {
        this.player = player;
        this.plugin = plugin;
    }

    /**
     * Create the inventory, set it's required fields, set the items
     * then force the player to open it.
     */
    public void open() {
        inventory = Bukkit.createInventory(this, getMenuSlots(), ColorFormat.format(getMenuName()));
        setMenuItems();
        player.openInventory(inventory);
    }

    /**
     * Retrieve the name of this menu. Colorized/formatted on open().
     * @return display name of this menu/inventory.
     */
    public abstract String getMenuName();

    /**
     * Retrieve the number of inventory slots for this inventory.
     * @return inventory size/slots
     */
    public abstract int getMenuSlots();

    /**
     * Set the menu items displayed inside the inventory interface.
     */
    public abstract void setMenuItems();

    /**
     * Handle an inventory click event. Should be executed
     * in the appropriate player listener.
     * @param e event instance
     */
    public abstract void handleMenuClick(InventoryClickEvent e);

    /**
     * Adds fillers to any null/air slots in the inventory.
     * @param fillerItem filler item to add.
     */
    public void addFillers(ItemStack fillerItem){
        for(int i = 0; i < inventory.getSize(); i++){
            ItemStack is = inventory.getItem(i);
            if(is == null || is.getType() == Material.AIR){
                inventory.setItem(i, fillerItem);
            }
        }
    }

    /**
     * Adds fillers to null/air slots within the range provided.
     *
     * @param fillerItem filler item to add
     * @param rangeMin range min (inclusive)
     * @param rangeMax range max (inclusive)
     */
    public void addFillers(ItemStack fillerItem, int rangeMin, int rangeMax){
        for(int i = 0; i < inventory.getSize(); i++){
            ItemStack is = inventory.getItem(i);
            if (i >= rangeMin && i <= rangeMax) {
                if(is == null || is.getType() == Material.AIR){
                    inventory.setItem(i, fillerItem);
                }
            }
        }
    }

    /**
     * Adds filler items to the border of the menu.
     * @param fillerItem filler item to create border
     */
    public void addFillerBorder(ItemStack fillerItem) {
        int slots = getMenuSlots();
        if (slots < 27) {
            return;
        }

        // Top row border
        addFillers(fillerItem, 0, 8);

        // Bottom row border
        addFillers(fillerItem, (slots - 1) - 9, (slots - 1));

        // Left hand side
        for (int i = 9; i < (slots - 9); i += 9) {
            ItemStack is = inventory.getItem(i);
            if (is == null || is.getType() == Material.AIR) {
                inventory.setItem(i, fillerItem);
            }
        }

        // Right hand side
        for (int j = 17; j < slots - 9; j += 9) {
            ItemStack is = inventory.getItem(j);
            if (is == null || is.getType() == Material.AIR) {
                inventory.setItem(j, fillerItem);
            }
        }
    }

    @NonNull
    public Inventory getInventory() {
        return inventory;
    }
}
