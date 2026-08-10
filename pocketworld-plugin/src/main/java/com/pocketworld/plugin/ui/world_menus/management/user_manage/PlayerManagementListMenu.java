package com.pocketworld.plugin.ui.world_menus.management.user_manage;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.ui.PocketItem;
import com.pocketworld.plugin.ui.PocketPaginatedMenu;
import com.pocketworld.plugin.ui.world_menus.invitations.InvitationsSendOrManageMenu;
import com.pocketworld.plugin.ui.world_menus.management.ManageWorldMenu;
import com.pocketworld.plugin.user.PocketUser;
import com.pocketworld.plugin.util.MessageReader;
import com.pocketworld.plugin.world.PocketWorld;
import com.pocketworld.plugin.world.WorldAction;
import com.pocketworld.plugin.world.WorldRank;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PlayerManagementListMenu extends PocketPaginatedMenu {

    private final ManageWorldMenu previousMenu;
    private final PocketWorld world;

    public PlayerManagementListMenu(Player player, PocketWorldPlugin plugin, PocketWorld world,
                                     ManageWorldMenu previousMenu) {
        super(player, plugin);
        this.previousMenu = previousMenu;
        this.world = world;
    }

    @Override
    public String getMenuName() {
        return "&4&lPlayer Management Menu";
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setMenuItems() {
        Inventory inventory = getInventory();

        addMenuBorder(Material.BLUE_STAINED_GLASS_PANE);

        ItemStack globe = new PocketItem.Builder(plugin)
                .material(Material.PLAYER_HEAD)
                .displayName("&d" + world.getWorldName())
                .lore(List.of("&7Members (" + world.getUsers().size() + "): " + world.getMembersFormatted(", "),
                        "&7World Size: " + world.getWorldSizeFormatted(), " ", "&8" + world.getId()))
                .build().getAsSkull("BlockminersTV");
        inventory.setItem(4, globe);

        ItemStack invitations = new PocketItem.Builder(plugin)
                .material(Material.BOOK)
                .displayName("&6Invitations")
                .lore(List.of("&7Click here to manage/view invitations for this world."))
                .tag("is-invitations-menu")
                .glow(true)
                .build().get();
        inventory.setItem(53, invitations);

        List<UUID> userIds = new ArrayList<>(world.getUsers().keySet());

        for (int i = 0; i < maxItemsPerPage; i++) {
            index = maxItemsPerPage * page + i;
            if (index >= userIds.size()) {
                break;
            }

            UUID id = userIds.get(index);
            if (id == null) {
                continue;
            }

            String name = Bukkit.getOfflinePlayer(id).getName();
            WorldRank rank = world.getUsers().get(id);

            ItemStack userIcon = new PocketItem.Builder(plugin)
                    .material(Material.PLAYER_HEAD)
                    .displayName("&a" + name)
                    .lore(List.of("&7Rank: " + rank.name(), " ", "&8" + id))
                    .tag(id.toString())
                    .build().getAsSkull(name);

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
        } else if (tag.equalsIgnoreCase("is-invitations-menu")) {
            new InvitationsSendOrManageMenu(player, plugin, world, this).open();
        } else if (tag.equalsIgnoreCase("is-page-next")) {
            if (index + 1 < world.getUsers().size()) {
                page++;
                open();
            }
        } else if (tag.equalsIgnoreCase("is-page-back")) {
            if (page > 0) {
                page--;
                open();
            }
        } else {
            Map<UUID, WorldRank> users = world.getUsers();

            if (!world.hasPermission(player.getUniqueId(), WorldAction.KICK)) {
                return;
            }

            UUID targetId = UUID.fromString(tag);
            Optional<UUID> userId = users.keySet().stream().filter(id -> id.equals(targetId)).findFirst();
            if (userId.isEmpty()) {
                return;
            }

            MessageReader reader = plugin.getMessageReader();
            UUID id = userId.get();
            if (id.equals(player.getUniqueId())) {
                reader.send("world-manage-yourself", player);
                player.closeInventory();
                return;
            }

            if (users.get(id) == WorldRank.OWNER) {
                reader.send("world-manage-higher-rank", player);
                player.closeInventory();
                return;
            }

            PocketUser userToManage = plugin.getUserCache().readThrough(id);
            new ManagePlayerMenu(player, plugin, world, userToManage, this).open();
        }
    }
}
