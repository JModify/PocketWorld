package me.modify.pocketworld.menu.worldmenus.management;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.menu.PocketMenu;
import me.modify.pocketworld.util.ColorFormat;
import me.modify.pocketworld.util.PocketItem;
import me.modify.pocketworld.world.PocketWorld;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class DeleteWorldConfirmationMenu extends PocketMenu {

    private PocketWorld world;
    private ManageWorldMenu previousMenu;
    public DeleteWorldConfirmationMenu(Player player, PocketWorldPlugin plugin, PocketWorld world,
                                       ManageWorldMenu previousMenu) {
        super(player, plugin);
        this.world = world;
        this.previousMenu = previousMenu;
    }

    @Override
    public String getMenuName() {
        return "&4&lDelete World Confirmation";
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
                .lore(List.of("&aClick to confirm deletion of this PocketWorld", " ",
                        "&7Note: Deleting a PocketWorld is permanent and", "&7the world cannot be recovered &7All ",
                        "&7members of this world will be unable ", "&7to access it indefinitely."))
                .tag("world-confirm-delete")
                .build().get();

        ItemStack cancel = new PocketItem.Builder(plugin)
                .material(Material.RED_WOOL)
                .displayName("&c&lCancel")
                .lore(List.of("&cClick to cancel and go to the previous menu."))
                .tag("world-cancel-delete")
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

        if (tag.equalsIgnoreCase("world-cancel-delete")) {
            previousMenu.open();
        } else if (tag.equalsIgnoreCase("world-confirm-delete")) {
            world.delete(plugin);
            player.sendMessage(ColorFormat.format("&2&lSUCCESS &r&aPocketWorld '" + world.getWorldName()
                    + "' deleted."));
            player.closeInventory();
        }

    }
}
