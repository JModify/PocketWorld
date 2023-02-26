package me.modify.pocketworld.ui.world_menus.management.user_manage;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.ui.PocketItem;
import me.modify.pocketworld.ui.PocketMenu;
import me.modify.pocketworld.user.PocketUser;
import me.modify.pocketworld.world.PocketWorld;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class KickPlayerConfirmationMenu extends PocketMenu {

    private PocketWorld world;
    private PocketUser userToKick;
    private ManagePlayerMenu previousMenu;

    public KickPlayerConfirmationMenu(Player player, PocketWorldPlugin plugin, PocketWorld world, PocketUser userToKick,
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
                .lore(List.of("&aClick to confirm kick of player '" + userToKick.getName() + "'"))
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

            world.announce(plugin.getMessageReader().read("world-kick-player",
                    "{PLAYER}:" + player.getName(),
                    "{TARGET}:" + Bukkit.getOfflinePlayer(userToKick.getId()).getName(),
                    "{WORLD_NAME}:" + world.getWorldName()));

            world.getUsers().remove(userToKick.getId());
            userToKick.removeWorld(world.getId());

            if (userToKick.isInPocketWorld(plugin, world.getId())) {
                World defaultWorld = Bukkit.getWorlds().get(0);
                // Player is never null, checked in user.isInPocketWorld method.
                Bukkit.getPlayer(userToKick.getId()).teleport(defaultWorld.getSpawnLocation());
            }

            player.closeInventory();
        }

    }

}
