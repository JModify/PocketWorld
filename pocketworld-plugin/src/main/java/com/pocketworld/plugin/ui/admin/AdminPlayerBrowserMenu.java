package com.pocketworld.plugin.ui.admin;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.ui.PocketItem;
import com.pocketworld.plugin.ui.PocketPaginatedMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** {@code /pocketworldadmin manage} with no name given - a paginated grid of every online player's
 *  skull, clicking one opens {@link AdminManageUserMenu} for them. The top-level entry point into
 *  the admin management flow, so its home button just closes the inventory. */
public class AdminPlayerBrowserMenu extends PocketPaginatedMenu {

    private final List<Player> players;

    public AdminPlayerBrowserMenu(Player player, PocketWorldPlugin plugin, List<Player> players) {
        super(player, plugin);
        this.players = players;
    }

    @Override
    public String getMenuName() {
        return "&4&lManage a Player";
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setMenuItems() {
        addMenuBorder(Material.GRAY_STAINED_GLASS_PANE);

        for (int i = 0; i < maxItemsPerPage; i++) {
            index = maxItemsPerPage * page + i;
            if (index >= players.size()) {
                break;
            }

            Player target = players.get(index);
            if (target == null) {
                continue;
            }

            ItemStack skull = new PocketItem.Builder(plugin)
                    .material(Material.PLAYER_HEAD)
                    .displayName("&e" + target.getName())
                    .lore(List.of("&7Click to manage this player.", " ", "&8" + target.getUniqueId()))
                    .tag(target.getUniqueId().toString())
                    .build().getAsSkull(target.getName());

            getInventory().addItem(skull);
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
            player.closeInventory();
        } else if (tag.equalsIgnoreCase("is-page-next")) {
            if (index + 1 < players.size()) {
                page++;
                open();
            }
        } else if (tag.equalsIgnoreCase("is-page-back")) {
            if (page > 0) {
                page--;
                open();
            }
        } else {
            UUID targetId = UUID.fromString(tag);
            Optional<Player> target = players.stream().filter(p -> p.getUniqueId().equals(targetId)).findFirst();
            target.ifPresent(p -> new AdminManageUserMenu(player, plugin, p.getUniqueId(), p.getName(), this::open).open());
        }
    }
}
