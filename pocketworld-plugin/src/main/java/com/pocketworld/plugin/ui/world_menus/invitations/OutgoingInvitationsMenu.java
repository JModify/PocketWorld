package com.pocketworld.plugin.ui.world_menus.invitations;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.ui.PocketItem;
import com.pocketworld.plugin.ui.PocketPaginatedMenu;
import com.pocketworld.plugin.util.ColorFormat;
import com.pocketworld.plugin.world.Invitation;
import com.pocketworld.plugin.world.PocketWorld;
import com.pocketworld.plugin.world.WorldRank;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OutgoingInvitationsMenu extends PocketPaginatedMenu {

    private final PocketWorld world;
    private final InvitationsSendOrManageMenu previousMenu;

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

    @SuppressWarnings("deprecation")
    @Override
    public void setMenuItems() {
        Inventory inventory = getInventory();
        addMenuBorder(Material.LIGHT_BLUE_STAINED_GLASS_PANE);

        ItemStack globe = new PocketItem.Builder(plugin)
                .material(Material.PLAYER_HEAD)
                .displayName("&d" + world.getWorldName())
                .lore(List.of("&7Members (" + world.getUsers().size() + "): " + world.getMembersFormatted(", "),
                        "&7World Size: " + world.getWorldSizeFormatted(), " ", "&8" + world.getId()))
                .build().getAsSkull("BlockminersTV");
        inventory.setItem(4, globe);

        List<UUID> recipients = new ArrayList<>(world.getInvitations().keySet());
        WorldRank rank = world.getUsers().get(player.getUniqueId());

        for (int i = 0; i < maxItemsPerPage; i++) {
            index = maxItemsPerPage * page + i;
            if (index >= recipients.size()) {
                break;
            }

            UUID recipientId = recipients.get(index);
            Invitation invitation = world.getInvitations().get(recipientId);
            String recipientName = Bukkit.getOfflinePlayer(recipientId).getName();
            String senderName = Bukkit.getOfflinePlayer(invitation.sender()).getName();

            String online = ColorFormat.format(Bukkit.getPlayer(recipientId) != null ? "&a&lONLINE" : "&c&lOFFLINE");

            List<String> lore = new ArrayList<>(List.of("&7Sent By: " + senderName, online));
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
            if (index + 1 < world.getInvitations().size()) {
                page++;
                open();
            }
        } else if (tag.equalsIgnoreCase("is-page-back")) {
            if (page > 0) {
                page--;
                open();
            }
        } else {
            WorldRank rank = world.getUsers().get(player.getUniqueId());
            if (rank != WorldRank.OWNER && rank != WorldRank.MOD) {
                return;
            }
            if (e.getClick() != ClickType.RIGHT) {
                return;
            }

            UUID recipientId = UUID.fromString(tag);
            world.revokeInvitation(plugin, player, recipientId);
            open();
        }
    }
}
