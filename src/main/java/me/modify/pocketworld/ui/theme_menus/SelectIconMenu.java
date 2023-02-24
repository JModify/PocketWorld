package me.modify.pocketworld.ui.theme_menus;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.ui.PocketMenu;
import me.modify.pocketworld.theme.creation.ThemeCreationController;
import me.modify.pocketworld.theme.creation.ThemeCreationRegistry;
import me.modify.pocketworld.ui.PocketItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class SelectIconMenu extends PocketMenu {

    public SelectIconMenu(Player player, PocketWorldPlugin plugin) {
        super(player, plugin);
    }

    @Override
    public String getMenuName() {
        return "&4&lIcon Select";
    }

    @Override
    public int getMenuSlots() {
        return 27;
    }

    @Override
    public void setMenuItems() {

        PocketItem fillerItem = new PocketItem.Builder(plugin)
                .material(Material.BLACK_STAINED_GLASS_PANE)
                .stackSize(1)
                .displayName("&cSelect Icon")
                .lore(List.of("&7Place desired item in empty slot to set the icon for this theme", "&8Close inventory once done."))
                .build();

        addFillers(fillerItem.get(), 0, 8);
        addFillers(fillerItem.get(), 9, 12);
        addFillers(fillerItem.get(), 14, 17);
        addFillers(fillerItem.get(), 18, 26);
    }

    @Override
    public void handleMenuClick(InventoryClickEvent e) {

        int slot = e.getSlot();

        ItemStack item = e.getCurrentItem();
        if (item == null) {
            return;
        }

        if (slot != 13) {
            e.setCancelled(true);
        }
    }

    public void handleMenuClose(InventoryCloseEvent e) {
        ItemStack slot = e.getInventory().getItem(13);

        if (slot != null) {
            ThemeCreationController controller = ThemeCreationRegistry.getInstance().getController(player.getUniqueId());
            controller.setIcon(slot.getType());
            controller.nextState();
        }
    }
}
