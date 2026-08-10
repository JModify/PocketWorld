package com.pocketworld.plugin.ui.admin;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.ui.PocketItem;
import com.pocketworld.plugin.ui.PocketMenu;
import com.pocketworld.plugin.util.ColorFormat;
import com.pocketworld.plugin.world.PocketWorld;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** Mirrors the owner-facing {@code DeleteWorldConfirmationMenu}, minus the owner-only requirement -
 *  an admin can wipe any world regardless of their own membership in it. */
public class AdminWipeWorldConfirmationMenu extends PocketMenu {

    private final PocketWorld world;
    private final Runnable onBack;

    public AdminWipeWorldConfirmationMenu(Player player, PocketWorldPlugin plugin, PocketWorld world, Runnable onBack) {
        super(player, plugin);
        this.world = world;
        this.onBack = onBack;
    }

    @Override
    public String getMenuName() {
        return "&4&lWipe World Confirmation";
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
                .lore(List.of("&aClick to confirm wiping \"" + world.getWorldName() + "\"", " ",
                        "&7Note: This is permanent and the world cannot", "&7be recovered. All members will",
                        "&7lose access indefinitely."))
                .tag("admin-confirm-wipe")
                .build().get();

        ItemStack cancel = new PocketItem.Builder(plugin)
                .material(Material.RED_WOOL)
                .displayName("&c&lCancel")
                .lore(List.of("&cClick to cancel and go to the previous menu."))
                .tag("admin-cancel-wipe")
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

        if (tag.equalsIgnoreCase("admin-cancel-wipe")) {
            onBack.run();
        } else if (tag.equalsIgnoreCase("admin-confirm-wipe")) {
            world.announce(plugin.getMessageReader().read("world-deleted",
                    "{PLAYER}:" + player.getName(),
                    "{WORLD_NAME}:" + world.getWorldName()));
            world.delete(plugin);
            player.sendMessage(ColorFormat.format("&aWiped pocket world \"" + world.getWorldName() + "\"."));
            player.closeInventory();
        }
    }
}
