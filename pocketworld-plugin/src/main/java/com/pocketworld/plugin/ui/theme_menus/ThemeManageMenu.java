package com.pocketworld.plugin.ui.theme_menus;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.theme.PocketTheme;
import com.pocketworld.plugin.ui.PocketItem;
import com.pocketworld.plugin.ui.PocketMenu;
import com.pocketworld.plugin.util.ColorFormat;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** Per-theme actions, opened from {@link ThemeManagementMenu}: delete (with confirmation) or edit
 *  (not yet implemented, matching the previous {@code /theme edit} command's placeholder). */
public class ThemeManageMenu extends PocketMenu {

    private final PocketTheme theme;
    private final Runnable onBack;

    public ThemeManageMenu(Player player, PocketWorldPlugin plugin, PocketTheme theme, Runnable onBack) {
        super(player, plugin);
        this.theme = theme;
        this.onBack = onBack;
    }

    @Override
    public String getMenuName() {
        return "&4&lManage Theme";
    }

    @Override
    public int getMenuSlots() {
        return 27;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setMenuItems() {
        Inventory inventory = getInventory();

        ItemStack info = new PocketItem.Builder(plugin)
                .material(theme.getIcon())
                .displayName("&b" + theme.getName())
                .lore(List.of("&eBiome: " + theme.getBiome(), "&eDescription: " + theme.getDescription(),
                        " ", "&8" + theme.getId()))
                .build().get();

        ItemStack edit = new PocketItem.Builder(plugin)
                .material(Material.WRITABLE_BOOK)
                .displayName("&aEdit")
                .lore(List.of("&7Not yet implemented."))
                .tag("theme-edit")
                .build().get();

        ItemStack delete = new PocketItem.Builder(plugin)
                .material(Material.BARRIER)
                .displayName("&cDelete")
                .lore(List.of("&7Permanently delete this theme."))
                .tag("theme-delete")
                .build().get();

        ItemStack back = new PocketItem.Builder(plugin)
                .material(Material.ARROW)
                .stackSize(1)
                .displayName("&aReturn")
                .lore(List.of("&7Click to return to previous menu."))
                .tag("is-back-button")
                .build().get();

        inventory.setItem(4, info);
        inventory.setItem(11, edit);
        inventory.setItem(15, delete);
        inventory.setItem(22, back);

        ItemStack fillerItem = new PocketItem.Builder(plugin)
                .material(Material.PURPLE_STAINED_GLASS_PANE)
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

        if (tag.equalsIgnoreCase("is-back-button")) {
            onBack.run();
        } else if (tag.equalsIgnoreCase("theme-delete")) {
            new DeleteThemeConfirmationMenu(player, plugin, theme, this::open).open();
        } else if (tag.equalsIgnoreCase("theme-edit")) {
            player.sendMessage(ColorFormat.format("&cThis feature is not yet implemented."));
        }
    }
}
