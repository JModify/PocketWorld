package me.modify.pocketworld.menu.worldmenus.management;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.data.DAO;
import me.modify.pocketworld.menu.PocketMenu;
import me.modify.pocketworld.user.PocketUser;
import me.modify.pocketworld.util.PocketItem;
import me.modify.pocketworld.world.PocketWorld;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class WorldLeaveConfirmationMenu extends PocketMenu {

    private PocketWorldPlugin plugin;
    private PocketWorld world;
    private WorldManagementMenu previousMenu;
    public WorldLeaveConfirmationMenu(Player player, PocketWorldPlugin plugin, WorldManagementMenu previousMenu, PocketWorld world) {
        super(player);
        this.plugin = plugin;
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
                .material(Material.EMERALD_BLOCK)
                .displayName("&a&lConfirm")
                .lore(List.of("&cClick to confirm leaving this pocket world."))
                .tag("world-confirm-leave")
                .build().get();

        ItemStack cancel = new PocketItem.Builder(plugin)
                .material(Material.RED_STAINED_GLASS)
                .displayName("&c&lCancel")
                .lore(List.of("&cClick to cancel and go to the previous menu."))
                .tag("world-cancel-leave")
                .build().get();

        inventory.setItem(11, cancel);
        inventory.setItem(15, confirm);

        ItemStack fillerItem = new PocketItem.Builder(plugin)
                .material(Material.BLACK_STAINED_GLASS_PANE)
                .displayName(" ")
                .build().get();

        addFillers(fillerItem);
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
            world.update(plugin);

            DAO dao = plugin.getDataSource().getConnection().getDAO();
            PocketUser user = dao.getPocketUser(player.getUniqueId());
            user.addWorld(world.getId());
            user.update(plugin);

            player.sendMessage("You left PocketWorld " + world.getWorldName());
        }

    }
}
