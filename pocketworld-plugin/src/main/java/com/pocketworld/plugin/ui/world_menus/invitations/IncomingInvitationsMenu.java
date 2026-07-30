package com.pocketworld.plugin.ui.world_menus.invitations;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.ui.PocketItem;
import com.pocketworld.plugin.ui.PocketPaginatedMenu;
import com.pocketworld.plugin.ui.world_menus.PocketWorldMainMenu;
import com.pocketworld.plugin.user.PocketUser;
import com.pocketworld.plugin.util.MessageReader;
import com.pocketworld.plugin.world.PocketWorld;
import com.pocketworld.plugin.world.WorldRank;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class IncomingInvitationsMenu extends PocketPaginatedMenu {

    private final List<PocketWorld> invitedWorlds;
    private final PocketWorldMainMenu previousMenu;

    public IncomingInvitationsMenu(Player player, PocketWorldPlugin plugin, List<PocketWorld> invitedWorlds,
                                    PocketWorldMainMenu previousMenu) {
        super(player, plugin);
        this.previousMenu = previousMenu;
        this.invitedWorlds = invitedWorlds;
    }

    @Override
    public String getMenuName() {
        return "&4&lIncoming Invitations";
    }

    @Override
    public void setMenuItems() {
        Inventory inventory = getInventory();
        addMenuBorder(Material.LIGHT_BLUE_STAINED_GLASS_PANE);

        for (int i = 0; i < maxItemsPerPage; i++) {
            index = maxItemsPerPage * page + i;
            if (index >= invitedWorlds.size()) {
                break;
            }

            PocketWorld world = invitedWorlds.get(index);
            if (world == null) {
                continue;
            }

            ItemStack worldIcon = new PocketItem.Builder(plugin)
                    .material(world.getIcon())
                    .displayName("&d" + world.getWorldName())
                    .lore(List.of("&7Members (" + world.getUsers().size() + "): " + world.getMembersFormatted(", "),
                            "&7Size: " + world.getWorldSizeFormatted(), " ",
                            "&8Left-Click = ACCEPT | Right-Click = DECLINE"))
                    .tag(world.getId().toString())
                    .build().get();

            inventory.addItem(worldIcon);
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
            if (index + 1 < invitedWorlds.size()) {
                page++;
                open();
            }
        } else if (tag.equalsIgnoreCase("is-page-back")) {
            if (page > 0) {
                page--;
                open();
            }
        } else {
            handleWorldResponse(e, tag);
        }
    }

    private void handleWorldResponse(InventoryClickEvent e, String tag) {
        UUID worldId = UUID.fromString(tag);
        Optional<PocketWorld> optionalWorld = invitedWorlds.stream().filter(w -> w.getId().equals(worldId)).findFirst();
        if (optionalWorld.isEmpty()) {
            return;
        }

        MessageReader reader = plugin.getMessageReader();
        PocketWorld world = optionalWorld.get();
        PocketUser user = plugin.getUserCache().readThrough(player.getUniqueId());

        if (e.getClick() == ClickType.RIGHT) {
            world.getInvitations().remove(player.getUniqueId());
            user.getInvitations().remove(world.getId());

            reader.send("world-invite-decline", player, "{WORLD_NAME}:" + world.getWorldName());
            world.announce(reader.read("world-announce-invite-decline",
                    "{PLAYER}:" + player.getName(), "{WORLD_NAME}:" + world.getWorldName()));
        } else if (e.getClick() == ClickType.LEFT) {
            world.getInvitations().remove(player.getUniqueId());
            world.getUsers().put(player.getUniqueId(), WorldRank.MEMBER);
            user.getInvitations().remove(world.getId());
            user.getWorlds().add(world.getId());

            world.announce(reader.read("world-announce-invite-accept",
                    "{PLAYER}:" + player.getName(), "{WORLD_NAME}:" + world.getWorldName()));
        }

        player.closeInventory();
    }
}
