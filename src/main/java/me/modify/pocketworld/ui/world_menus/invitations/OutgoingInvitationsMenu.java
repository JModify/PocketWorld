package me.modify.pocketworld.ui.world_menus.invitations;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.ui.PocketItem;
import me.modify.pocketworld.ui.PocketPaginatedMenu;
import me.modify.pocketworld.user.PocketUser;
import me.modify.pocketworld.util.ColorFormat;
import me.modify.pocketworld.world.PocketWorld;
import me.modify.pocketworld.world.WorldRank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class OutgoingInvitationsMenu extends PocketPaginatedMenu {

    private PocketWorld world;
    private InvitationsSendOrManageMenu previousMenu;
    public OutgoingInvitationsMenu(Player player, PocketWorldPlugin plugin, PocketWorld world,
                                   InvitationsSendOrManageMenu previousMenu) {
        super(player, plugin);
        this.world = world;
        this.previousMenu = previousMenu;
    }

    @Override
    public String getMenuName() {
        return "&4&lManage World Invites";
    }

    @Override
    public void setMenuItems() {
        Inventory inventory = getInventory();

        addMenuBorder(Material.LIGHT_BLUE_STAINED_GLASS_PANE);

        ItemStack globe = new PocketItem.Builder(plugin)
                .material(Material.PLAYER_HEAD)
                .displayName("&d" + world.getWorldName())
                .lore(List.of("&7Members (" + world.getUsers().size() + "): " + world.getMembersFormatted(", "),
                        "&7Size: " + world.getWorldSizeFormatted(), " ", "&8" + world.getId().toString()))
                .build().getAsSkull("BlockminersTV");
        inventory.setItem(4, globe);

        List<UUID> invitations = new ArrayList<>(world.getInvitations().keySet());
        WorldRank rank = world.getUsers().get(player.getUniqueId());

        if (!invitations.isEmpty()) {
            for (int i = 0; i < maxItemsPerPage; i++) {
                index = maxItemsPerPage * page + i;
                if (index >= invitations.size()) break;

                UUID recipientId = invitations.get(i);
                if (recipientId == null) continue;
                String recipientName = Bukkit.getOfflinePlayer(recipientId).getName();

                UUID senderId = world.getInvitations().get(recipientId);
                String senderName = Bukkit.getOfflinePlayer(senderId).getName();

                String online = ColorFormat.format(Bukkit.getPlayer(recipientId) != null
                        ? "&a&lONLINE" : "&c&lOFFLINE");

                List<String> lore = new ArrayList<>();
                lore.add("&7Sent By: " + senderName);
                lore.add(online);

                if (rank == WorldRank.OWNER || rank == WorldRank.MOD) {
                    lore.add(" ");
                    lore.add("&8Right click to revoke invitation");
                }
                ItemStack userIcon = new PocketItem.Builder(plugin)
                        .material(Material.PLAYER_HEAD)
                        .displayName("&a" + recipientName)
                        .lore(lore)
                        .tag(recipientId.toString())
                        .build().getAsSkull(recipientName);

                inventory.addItem(userIcon);
            }
        }

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

        if (tag.equalsIgnoreCase("is-home-button")) {
            previousMenu.open();
        } else if (tag.equalsIgnoreCase("is-page-next")) {
            if (!((index + 1) >= world.getUsers().size())) {
                page = page + 1;
                super.open();
            }
        } else if (tag.equalsIgnoreCase("is-page-back")) {
            if (page > 0) {
                page = page - 1;
                super.open();
            }
        } else {
            WorldRank rank = world.getUsers().get(player.getUniqueId());
            if (rank != WorldRank.OWNER && rank != WorldRank.MOD) {
                return;
            }

            UUID recipientId = UUID.fromString(ChatColor.stripColor(tag));

            if (e.getClick() != ClickType.RIGHT) {
                return;
            }

            world.revokeInvitation(plugin, player, recipientId);
            open();
        }
    }
}
