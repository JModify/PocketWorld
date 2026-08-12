package com.pocketworld.plugin.ui.world_menus.management.user_manage;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.ui.PocketItem;
import com.pocketworld.plugin.ui.PocketPaginatedMenu;
import com.pocketworld.plugin.user.PocketUser;
import com.pocketworld.plugin.util.MessageReader;
import com.pocketworld.plugin.world.PocketWorld;
import com.pocketworld.plugin.world.WorldRank;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Shown instead of directly demoting a world's current Owner, so the world is never left without
 * one: lists every other member, and picking one promotes them to Owner before demoting the
 * previous owner to whichever rank was originally chosen in {@link PlayerSetRankMenu}. Only ever
 * opened when at least one other member exists - {@code PlayerSetRankMenu} blocks the demotion
 * outright with an error message otherwise, since there'd be nobody to list here.
 */
public class TransferOwnershipMenu extends PocketPaginatedMenu {

    private final PocketWorld world;
    private final PocketUser currentOwner;
    private final WorldRank demoteCurrentOwnerTo;
    private final PlayerSetRankMenu previousMenu;

    public TransferOwnershipMenu(Player player, PocketWorldPlugin plugin, PocketWorld world, PocketUser currentOwner,
                                  WorldRank demoteCurrentOwnerTo, PlayerSetRankMenu previousMenu) {
        super(player, plugin);
        this.world = world;
        this.currentOwner = currentOwner;
        this.demoteCurrentOwnerTo = demoteCurrentOwnerTo;
        this.previousMenu = previousMenu;
    }

    @Override
    public String getMenuName() {
        return "&4&lSelect New Owner";
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setMenuItems() {
        Inventory inventory = getInventory();

        addMenuBorder(Material.BLUE_STAINED_GLASS_PANE);

        List<UUID> candidateIds = otherMemberIds();

        for (int i = 0; i < maxItemsPerPage; i++) {
            index = maxItemsPerPage * page + i;
            if (index >= candidateIds.size()) {
                break;
            }

            UUID id = candidateIds.get(index);
            String name = Bukkit.getOfflinePlayer(id).getName();
            WorldRank rank = world.getUsers().get(id);

            ItemStack userIcon = new PocketItem.Builder(plugin)
                    .material(Material.PLAYER_HEAD)
                    .displayName("&a" + name)
                    .lore(List.of("&7Rank: " + rank.name(), "&7Click to make this player the new Owner.",
                            " ", "&8" + id))
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
        } else if (tag.equalsIgnoreCase("is-page-next")) {
            if (index + 1 < otherMemberIds().size()) {
                page++;
                open();
            }
        } else if (tag.equalsIgnoreCase("is-page-back")) {
            if (page > 0) {
                page--;
                open();
            }
        } else {
            UUID newOwnerId;
            try {
                newOwnerId = UUID.fromString(tag);
            } catch (IllegalArgumentException ex) {
                return;
            }

            if (!world.getUsers().containsKey(newOwnerId)) {
                return;
            }

            String newOwnerName = Bukkit.getOfflinePlayer(newOwnerId).getName();

            player.closeInventory();

            world.getUsers().put(newOwnerId, WorldRank.OWNER);
            world.getUsers().put(currentOwner.getId(), demoteCurrentOwnerTo);

            MessageReader reader = plugin.getMessageReader();
            world.announce(reader.read("world-leadership-transfer",
                    "{PLAYER}:" + player.getName(),
                    "{WORLD_NAME}:" + world.getWorldName(),
                    "{TARGET}:" + newOwnerName));
        }
    }

    private List<UUID> otherMemberIds() {
        List<UUID> ids = new ArrayList<>(world.getUsers().keySet());
        ids.remove(currentOwner.getId());
        return ids;
    }
}
