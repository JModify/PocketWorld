package me.modify.pocketworld.menu.worldmenus.management;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.data.DAO;
import me.modify.pocketworld.menu.PocketMenu;
import me.modify.pocketworld.user.PocketUser;
import me.modify.pocketworld.util.ColorFormat;
import me.modify.pocketworld.util.PocketItem;
import me.modify.pocketworld.world.PocketWorld;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class KickPlayerConfirmationMenu extends PocketMenu {

    private PocketWorld world;
    private UUID userToKick;
    private ManagePlayerMenu previousMenu;

    public KickPlayerConfirmationMenu(Player player, PocketWorldPlugin plugin, PocketWorld world, UUID userToKick,
                                      ManagePlayerMenu previousMenu) {
        super(player, plugin);
        this.world = world;
        this.userToKick = userToKick;
        this.previousMenu = previousMenu;
    }

    @Override
    public String getMenuName() {
        return "&4&lKick Player Confirmation";
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
                .lore(List.of("&aClick to confirm kick of player '" + Bukkit.getPlayer(userToKick).getName() + "'"))
                .tag("player-confirm-kick")
                .build().get();

        ItemStack cancel = new PocketItem.Builder(plugin)
                .material(Material.RED_WOOL)
                .displayName("&c&lCancel")
                .lore(List.of("&cClick to cancel and go to the previous menu."))
                .tag("player-cancel-kick")
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

        if (tag.equalsIgnoreCase("player-cancel-kick")) {
            previousMenu.open();
        } else if (tag.equalsIgnoreCase("player-confirm-kick")) {
            world.getUsers().remove(userToKick);

            DAO dao = plugin.getDataSource().getConnection().getDAO();
            PocketUser user = dao.getPocketUser(userToKick);
            user.removeWorld(world.getId());
            user.update(plugin);

            if (user.isInPocketWorld(plugin, world.getId())) {
                World defaultWorld = Bukkit.getWorlds().get(0);

                // Player is never null, checked in user.isInPocketWorld method.
                Bukkit.getPlayer(userToKick).teleport(defaultWorld.getSpawnLocation());
            }

            player.sendMessage(ColorFormat.format("&2&lSUCCESS &r&aPlayer '" + Bukkit.getPlayer(userToKick).getName() +
                    "' kicked from the world."));
            player.closeInventory();

        }

    }

}
