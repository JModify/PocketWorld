package com.pocketworld.plugin.ui.admin;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.ui.PocketItem;
import com.pocketworld.plugin.ui.PocketPaginatedMenu;
import com.pocketworld.plugin.world.PocketWorld;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/** Every pocket world the target player is a member of, at any rank. Clicking one opens
 *  {@link AdminManageWorldMenu} for it. */
public class AdminUserWorldsMenu extends PocketPaginatedMenu {

    private final List<PocketWorld> worlds;
    private final UUID targetId;
    private final String targetName;
    private final Runnable onBack;

    public AdminUserWorldsMenu(Player player, PocketWorldPlugin plugin, List<PocketWorld> worlds, UUID targetId,
                                String targetName, Runnable onBack) {
        super(player, plugin);
        this.worlds = worlds;
        this.targetId = targetId;
        this.targetName = targetName;
        this.onBack = onBack;
    }

    @Override
    public String getMenuName() {
        return "&4&l" + targetName + "'s Worlds";
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setMenuItems() {
        addMenuBorder(Material.GRAY_STAINED_GLASS_PANE);

        for (int i = 0; i < maxItemsPerPage; i++) {
            index = maxItemsPerPage * page + i;
            if (index >= worlds.size()) {
                break;
            }

            PocketWorld world = worlds.get(index);
            if (world == null) {
                continue;
            }

            String rank = String.valueOf(world.getUsers().get(targetId));
            String members = world.getUsers().keySet().stream()
                    .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                    .collect(Collectors.joining(", "));
            String status = world.isLoaded() ? "&aLOADED" : "&cNOT LOADED";

            ItemStack worldIcon = new PocketItem.Builder(plugin)
                    .material(world.getIcon())
                    .displayName("&b" + world.getWorldName())
                    .lore(List.of("&7Click to manage this world.", " ",
                            "&6Properties", "&e" + targetName + "'s Rank: " + rank,
                            "&eMembers: " + members, "&eWorld Size: " + world.getWorldSizeFormatted(),
                            " ", status, "&8" + world.getId()))
                    .tag(world.getId().toString())
                    .build().get();

            getInventory().addItem(worldIcon);
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
            if (index + 1 < worlds.size()) {
                page++;
                open();
            }
        } else if (tag.equalsIgnoreCase("is-page-back")) {
            if (page > 0) {
                page--;
                open();
            }
        } else {
            UUID worldId = UUID.fromString(tag);
            Optional<PocketWorld> world = worlds.stream().filter(w -> w.getId().equals(worldId)).findFirst();
            world.ifPresent(w -> new AdminManageWorldMenu(player, plugin, w, this::open).open());
        }
    }
}
