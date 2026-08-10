package com.pocketworld.plugin.ui.admin;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.ui.PocketItem;
import com.pocketworld.plugin.ui.PocketPaginatedMenu;
import com.pocketworld.plugin.world.PocketWorld;
import com.pocketworld.plugin.world.WorldRank;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Read-only listing of every member of a world and their rank - no click action per member, this
 *  is purely informational for an admin (unlike the owner-facing {@code PlayerManagementListMenu}). */
public class AdminViewMembersMenu extends PocketPaginatedMenu {

    private final PocketWorld world;
    private final Runnable onBack;
    private final List<UUID> memberIds;

    public AdminViewMembersMenu(Player player, PocketWorldPlugin plugin, PocketWorld world, Runnable onBack) {
        super(player, plugin);
        this.world = world;
        this.onBack = onBack;
        this.memberIds = new ArrayList<>(world.getUsers().keySet());
    }

    @Override
    public String getMenuName() {
        return "&4&l" + world.getWorldName() + "'s Members";
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setMenuItems() {
        addMenuBorder(Material.GRAY_STAINED_GLASS_PANE);

        Map<UUID, WorldRank> users = world.getUsers();
        for (int i = 0; i < maxItemsPerPage; i++) {
            index = maxItemsPerPage * page + i;
            if (index >= memberIds.size()) {
                break;
            }

            UUID id = memberIds.get(index);
            String name = Bukkit.getOfflinePlayer(id).getName();
            WorldRank rank = users.get(id);

            ItemStack userIcon = new PocketItem.Builder(plugin)
                    .material(Material.PLAYER_HEAD)
                    .displayName("&a" + name)
                    .lore(List.of("&7Rank: " + rank.name(), " ", "&8" + id))
                    .build().getAsSkull(name);

            getInventory().addItem(userIcon);
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
            onBack.run();
        } else if (tag.equalsIgnoreCase("is-page-next")) {
            if (index + 1 < memberIds.size()) {
                page++;
                open();
            }
        } else if (tag.equalsIgnoreCase("is-page-back")) {
            if (page > 0) {
                page--;
                open();
            }
        }
    }
}
