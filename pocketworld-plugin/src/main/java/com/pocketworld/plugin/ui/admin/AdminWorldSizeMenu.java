package com.pocketworld.plugin.ui.admin;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.ui.PocketItem;
import com.pocketworld.plugin.ui.PocketMenu;
import com.pocketworld.plugin.world.PocketWorld;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** Lets an admin change a world's border size: a grass block in the middle shows the pending size,
 *  up/down arrows adjust it (in-menu only, re-rendering the grass block's lore each click), and
 *  Confirm applies it to the world - only live-updating the border if the world is currently loaded,
 *  same as every other property change in this plugin. */
public class AdminWorldSizeMenu extends PocketMenu {

    private static final int STEP = 10;
    private static final int MIN_SIZE = 10;

    private final PocketWorld world;
    private final Runnable onBack;
    private int pendingSize;

    public AdminWorldSizeMenu(Player player, PocketWorldPlugin plugin, PocketWorld world, Runnable onBack) {
        super(player, plugin);
        this.world = world;
        this.onBack = onBack;
        this.pendingSize = world.getWorldSize();
    }

    @Override
    public String getMenuName() {
        return "&4&lSet World Size";
    }

    @Override
    public int getMenuSlots() {
        return 27;
    }

    @Override
    public void setMenuItems() {
        Inventory inventory = getInventory();

        ItemStack up = new PocketItem.Builder(plugin)
                .material(Material.LIME_STAINED_GLASS_PANE)
                .displayName("&a+ " + STEP)
                .lore(List.of("&7Click to increase the pending size."))
                .tag("admin-size-up")
                .build().get();

        ItemStack down = new PocketItem.Builder(plugin)
                .material(Material.RED_STAINED_GLASS_PANE)
                .displayName("&c- " + STEP)
                .lore(List.of("&7Click to decrease the pending size."))
                .tag("admin-size-down")
                .build().get();

        ItemStack confirm = new PocketItem.Builder(plugin)
                .material(Material.LIME_WOOL)
                .displayName("&a&lConfirm")
                .lore(List.of("&7Click to apply this world size."))
                .tag("admin-size-confirm")
                .build().get();

        ItemStack back = new PocketItem.Builder(plugin)
                .material(Material.ARROW)
                .stackSize(1)
                .displayName("&aReturn")
                .lore(List.of("&7Click to return to previous menu, discarding changes."))
                .tag("is-back-button")
                .build().get();

        inventory.setItem(4, up);
        inventory.setItem(13, getSizeIcon());
        inventory.setItem(22, down);
        inventory.setItem(16, confirm);
        inventory.setItem(10, back);

        ItemStack fillerItem = new PocketItem.Builder(plugin)
                .material(Material.GRAY_STAINED_GLASS_PANE)
                .displayName(" ")
                .build().get();
        addFillerBorder(fillerItem);
    }

    private ItemStack getSizeIcon() {
        return new PocketItem.Builder(plugin)
                .material(Material.GRASS_BLOCK)
                .displayName("&aPending Size: " + pendingSize + "x" + pendingSize)
                .lore(List.of("&7Current: " + world.getWorldSizeFormatted(), " ",
                        "&8Click Confirm below to apply."))
                .build().get();
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

        if (tag.equalsIgnoreCase("admin-size-up")) {
            pendingSize += STEP;
            refreshSizeIcon();
        } else if (tag.equalsIgnoreCase("admin-size-down")) {
            pendingSize = Math.max(MIN_SIZE, pendingSize - STEP);
            refreshSizeIcon();
        } else if (tag.equalsIgnoreCase("admin-size-confirm")) {
            world.setWorldSize(pendingSize);
            world.setWorldBorder();
            onBack.run();
        } else if (tag.equalsIgnoreCase("is-back-button")) {
            onBack.run();
        }
    }

    private void refreshSizeIcon() {
        getInventory().setItem(13, getSizeIcon());
    }
}
