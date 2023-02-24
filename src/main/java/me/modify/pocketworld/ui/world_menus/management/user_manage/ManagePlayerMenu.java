package me.modify.pocketworld.ui.world_menus.management.user_manage;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.ui.PocketMenu;
import me.modify.pocketworld.user.PocketUser;
import me.modify.pocketworld.ui.PocketItem;
import me.modify.pocketworld.world.PocketWorld;
import me.modify.pocketworld.world.WorldRank;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ManagePlayerMenu extends PocketMenu {

    private PocketUser userToManage;
    private PocketWorld world;
    private PlayerManagementListMenu previousMenu;

    public ManagePlayerMenu(Player player, PocketWorldPlugin plugin, PocketWorld world, PocketUser userToManage,
                            PlayerManagementListMenu previousMenu) {
        super(player, plugin);
        this.world = world;
        this.userToManage = userToManage;
        this.previousMenu = previousMenu;
    }

    @Override
    public String getMenuName() {
        return "&4&lManage " + userToManage.getName();
    }

    @Override
    public int getMenuSlots()    {
        return 45;
    }

    @Override
    public void setMenuItems() {

        Inventory inventory = getInventory();

        ItemStack globe = new PocketItem.Builder(plugin)
                .material(Material.PLAYER_HEAD)
                .displayName("&d" + world.getWorldName())
                .lore(List.of("&7Members (" + world.getUsers().size() + "): " + world.getMembersFormatted(", "),
                        "&7Size: " + world.getWorldSizeFormatted(), " ", "&8" + world.getId().toString()))
                .build().getAsSkull("BlockminersTV");

        String name = userToManage.getName();
        WorldRank rank = world.getUsers().get(userToManage.getId());
        ItemStack userIcon = new PocketItem.Builder(plugin)
                .material(Material.PLAYER_HEAD)
                .displayName("&a" + name)
                .lore(List.of("&7Rank: " + rank.name(), " ", "&8" + userToManage))
                .build().getAsSkull(name);

        ItemStack backButton = new PocketItem.Builder(plugin)
                .material(Material.ARROW)
                .displayName("&aMain Menu")
                .lore(List.of("&7Click to return to previous menu."))
                .tag("is-back-button")
                .build().get();

        ItemStack kickUser = new PocketItem.Builder(plugin)
                .material(Material.BARRIER)
                .displayName("&6Kick User")
                .lore(List.of("&7Click to kick this user from the world."))
                .tag("user-kick")
                .build().get();

        inventory.setItem(4, globe);
        inventory.setItem(20, userIcon);
        inventory.setItem(36, backButton);

        // Only world owners get access to edit user ranks.
        WorldRank playerRank = world.getUsers().get(player.getUniqueId());
        if (playerRank == WorldRank.OWNER) {
            ItemStack setRank = new PocketItem.Builder(plugin)
                    .material(Material.COMMAND_BLOCK)
                    .displayName("&6Edit Rank")
                    .lore(List.of("&7Click to edit rank of this user."))
                    .tag("user-edit-rank")
                    .build().get();

            inventory.setItem(23, setRank);
        }

        inventory.setItem(24, kickUser);

        ItemStack fillerItem = new PocketItem.Builder(plugin)
                .material(Material.BLUE_STAINED_GLASS_PANE)
                .stackSize(1)
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
            previousMenu.open();
        } else if (tag.equalsIgnoreCase("user-edit-rank")) {
            PlayerSetRankMenu playerSetRank = new PlayerSetRankMenu(player, plugin, world, userToManage, this);
            playerSetRank.open();
        } else if (tag.equalsIgnoreCase("user-kick")) {
            KickPlayerConfirmationMenu kickConfirmation = new KickPlayerConfirmationMenu(player, plugin,
                    world, userToManage, this);
            kickConfirmation.open();
        }
    }
}
