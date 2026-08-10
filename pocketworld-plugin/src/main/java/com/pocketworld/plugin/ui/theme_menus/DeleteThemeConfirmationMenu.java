package com.pocketworld.plugin.ui.theme_menus;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.theme.PocketTheme;
import com.pocketworld.plugin.ui.PocketItem;
import com.pocketworld.plugin.ui.PocketMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** Mirrors {@code DeleteWorldConfirmationMenu}'s confirm/cancel pattern for themes. */
public class DeleteThemeConfirmationMenu extends PocketMenu {

    private final PocketTheme theme;
    private final Runnable onCancel;

    public DeleteThemeConfirmationMenu(Player player, PocketWorldPlugin plugin, PocketTheme theme, Runnable onCancel) {
        super(player, plugin);
        this.theme = theme;
        this.onCancel = onCancel;
    }

    @Override
    public String getMenuName() {
        return "&4&lDelete Theme Confirmation";
    }

    @Override
    public int getMenuSlots() {
        return 27;
    }

    @Override
    public void setMenuItems() {
        Inventory inventory = getInventory();

        ItemStack confirm = new PocketItem.Builder(plugin)
                .material(Material.LIME_WOOL)
                .displayName("&a&lConfirm")
                .lore(List.of("&aClick to confirm deletion of theme \"" + theme.getName() + "\"", " ",
                        "&7Note: Deleting a theme is permanent. Pocket", "&7worlds already created from it are unaffected."))
                .tag("theme-confirm-delete")
                .build().get();

        ItemStack cancel = new PocketItem.Builder(plugin)
                .material(Material.RED_WOOL)
                .displayName("&c&lCancel")
                .lore(List.of("&cClick to cancel and go to the previous menu."))
                .tag("theme-cancel-delete")
                .build().get();

        inventory.setItem(11, cancel);
        inventory.setItem(15, confirm);

        ItemStack fillerItem = new PocketItem.Builder(plugin)
                .material(Material.RED_STAINED_GLASS_PANE)
                .displayName(" ")
                .build().get();
        addFillerBorder(fillerItem);
    }

    @Override
    public void handleMenuClick(InventoryClickEvent e) {
        e.setCancelled(true);

        ItemStack item = e.getCurrentItem();
        if (item == null) {
            return;
        }

        String tag = PocketItem.getTag(plugin, item);
        if (tag == null) {
            return;
        }

        if (tag.equalsIgnoreCase("theme-cancel-delete")) {
            onCancel.run();
        } else if (tag.equalsIgnoreCase("theme-confirm-delete")) {
            String name = theme.getName();
            theme.delete(plugin);
            plugin.getMessageReader().send("theme-deleted", player, "{NAME}:" + name);
            player.closeInventory();
        }
    }
}
