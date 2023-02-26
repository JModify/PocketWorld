package me.modify.pocketworld.ui.world_menus.invitations;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.ui.PocketItem;
import me.modify.pocketworld.ui.PocketPaginatedMenu;
import me.modify.pocketworld.ui.world_menus.PocketWorldMainMenu;
import me.modify.pocketworld.user.PocketUser;
import me.modify.pocketworld.util.MessageReader;
import me.modify.pocketworld.world.PocketWorld;
import me.modify.pocketworld.world.WorldRank;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class IncomingInvitationsMenu extends PocketPaginatedMenu {

    private List<PocketWorld> invitedWorlds;
    private PocketWorldMainMenu previousMenu;
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

        if (!invitedWorlds.isEmpty()) {
            for (int i = 0; i < maxItemsPerPage; i++) {
                index = maxItemsPerPage * page + i;
                if (index >= invitedWorlds.size()) break;

                PocketWorld world = invitedWorlds.get(i);

                if (world == null) continue;

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
            if (!((index + 1) >= invitedWorlds.size())) {
                page = page + 1;
                super.open();
            }
        } else if (tag.equalsIgnoreCase("is-page-back")) {
            if (page > 0) {
                page = page - 1;
                super.open();
            }
        } else {
            UUID worldID = UUID.fromString(ChatColor.stripColor(tag));

            plugin.getDebugger().severe(String.valueOf(invitedWorlds.size()));
            Optional<PocketWorld> optionalWorld = invitedWorlds.stream().filter(w -> w.getId().equals(worldID)).findFirst();

            if (optionalWorld.isEmpty()) {
                return;
            }

            MessageReader reader = plugin.getMessageReader();

            PocketWorld world = optionalWorld.get();
            PocketUser user = plugin.getUserCache().readThrough(player.getUniqueId());
            if (e.getClick() == ClickType.RIGHT) {
                // User declines invitation
                world.getInvitations().remove(player.getUniqueId());
                user.getInvitations().remove(world.getId());

                // Send announcement to player saying that they declined invitation.
                reader.send("world-invite-decline", player, "{WORLD_NAME}:" + world.getWorldName());

                // Send announcement to world saying the user declined invitation.
                world.announce(reader.read("world-announce-invite-decline",
                        "{PLAYER}:" + player.getName(),
                        "{WORLD_NAME}:" + world.getWorldName()));
            } else if (e.getClick() == ClickType.LEFT) {
                // Player accepts invitation
                world.getInvitations().remove(player.getUniqueId());
                world.getUsers().put(player.getUniqueId(), WorldRank.MEMBER);
                user.getInvitations().remove(world.getId());
                user.getWorlds().add(world.getId());

                // Only need to send announcement to world since player is now a member of the world.
                world.announce(reader.read("world-announce-invite-accept",
                        "{PLAYER}:" + player.getName(),
                        "{WORLD_NAME}:" + world.getWorldName()));
            }

            player.closeInventory();
        }
    }
}
