package com.pocketworld.plugin.ui.theme_menus;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.theme.creation.ThemeCreationController;
import com.pocketworld.plugin.theme.creation.ThemeCreationRegistry;
import com.pocketworld.plugin.ui.PocketItem;
import com.pocketworld.plugin.ui.PocketMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** Lets an admin place any item into the middle slot to use it as their theme's icon. */
public class SelectIconMenu extends PocketMenu {

    private static final int ICON_SLOT = 13;

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
                .lore(List.of("&7Place desired item in empty slot to set the icon for this theme",
                        "&8Close inventory once done."))
                .build();

        addFillers(fillerItem.get(), 0, 8);
        addFillers(fillerItem.get(), 9, 12);
        addFillers(fillerItem.get(), 14, 17);
        addFillers(fillerItem.get(), 18, 26);
    }

    @Override
    public void handleMenuClick(InventoryClickEvent e) {
        if (e.getCurrentItem() == null) {
            return;
        }
        if (e.getSlot() != ICON_SLOT) {
            e.setCancelled(true);
        }
    }

    public void handleMenuClose(InventoryCloseEvent e) {
        ItemStack slot = e.getInventory().getItem(ICON_SLOT);
        if (slot != null) {
            ThemeCreationController controller = ThemeCreationRegistry.getInstance().getController(player.getUniqueId());
            controller.setIcon(slot.getType());
            controller.nextState();
        }
    }
}
