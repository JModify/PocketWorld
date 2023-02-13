package me.modify.pocketworld.menu.worldmenus.management;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.data.DAO;
import me.modify.pocketworld.menu.PocketMenu;
import me.modify.pocketworld.user.PocketUser;
import me.modify.pocketworld.util.ColorFormat;
import me.modify.pocketworld.util.PocketItem;
import me.modify.pocketworld.world.PocketWorld;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class LeaveWorldConfirmationMenu extends PocketMenu {

    private PocketWorld world;
    private ManageWorldMenu previousMenu;
    public LeaveWorldConfirmationMenu(Player player, PocketWorldPlugin plugin, PocketWorld world,
                                      ManageWorldMenu previousMenu) {
        super(player, plugin);
        this.world = world;
        this.previousMenu = previousMenu;
    }

    @Override
    public String getMenuName() {
        return "&4&lLeave World Confirmation";
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
                .lore(List.of("&cClick to confirm leaving this pocket world."))
                .tag("world-confirm-leave")
                .build().get();

        ItemStack cancel = new PocketItem.Builder(plugin)
                .material(Material.RED_WOOL)
                .displayName("&c&lCancel")
                .lore(List.of("&cClick to cancel and go to the previous menu."))
                .tag("world-cancel-leave")
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

        if (tag.equalsIgnoreCase("world-cancel-leave")) {
            previousMenu.open();
        } else if (tag.equalsIgnoreCase("world-confirm-leave")) {

            // Remove user from world then update the world
            world.getUsers().remove(player.getUniqueId());

            DAO dao = plugin.getDataSource().getConnection().getDAO();
            PocketUser user = dao.getPocketUser(player.getUniqueId());
            user.removeWorld(world.getId());
            user.update(plugin);

            player.sendMessage(ColorFormat.format("&2&lSUCCESS &r&aYou left PocketWorld " + world.getWorldName()));
            player.closeInventory();
        }

    }
}
