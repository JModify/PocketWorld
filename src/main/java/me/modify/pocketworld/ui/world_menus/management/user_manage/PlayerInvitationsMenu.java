package me.modify.pocketworld.ui.world_menus.management.user_manage;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.ui.PocketItem;
import me.modify.pocketworld.ui.PocketMenu;
import me.modify.pocketworld.world.PocketWorld;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class PlayerInvitationsMenu extends PocketMenu {

    private PocketWorld world;
    private PlayerManagementListMenu previousMenu;
    public PlayerInvitationsMenu(Player player, PocketWorldPlugin plugin, PocketWorld world,
                                 PlayerManagementListMenu previousMenu) {
        super(player, plugin);
        this.world = world;
        this.previousMenu = previousMenu;
    }

    @Override
    public String getMenuName() {
        return "&4&lInvitations";
    }

    @Override
    public int getMenuSlots() {
        return 36;
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
        inventory.setItem(13, globe);

        ItemStack sendInvite = new PocketItem.Builder(plugin)
                .material(Material.WRITABLE_BOOK)
                .displayName("&aSend Invitation")
                .lore(List.of("&7Send a new invite to a player.", "&8Note: Player must be online."))
                .tag("is-send-invite")
                .build().get();
        inventory.setItem(20, sendInvite);

        ItemStack manageInvites = new PocketItem.Builder(plugin)
                .material(Material.WRITTEN_BOOK)
                .displayName("&aView/Manage Invitations")
                .lore(List.of("&7View all sent invitations, or revoke invites here."))
                .tag("is-manage-invites")
                .build().get();
        inventory.setItem(24, manageInvites);

        ItemStack backButton = new PocketItem.Builder(plugin)
                .material(Material.ARROW)
                .displayName("&aMain Menu")
                .lore(List.of("&7Click to return to previous menu."))
                .tag("is-back-button")
                .build().get();
        inventory.setItem(27, backButton);

        ItemStack fillerItem = new PocketItem.Builder(plugin)
                .material(Material.BLUE_STAINED_GLASS_PANE)
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
        } else if (tag.equalsIgnoreCase("is-send-invite")) {
            SendInviteMenu sendInviteMenu = new SendInviteMenu(player, plugin, world);
            sendInviteMenu.open();
        } else if (tag.equalsIgnoreCase("is-manage-invites")) {

        }

    }
}
