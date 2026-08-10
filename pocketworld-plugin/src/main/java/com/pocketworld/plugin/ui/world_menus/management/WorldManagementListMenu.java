package com.pocketworld.plugin.ui.world_menus.management;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.ui.PocketItem;
import com.pocketworld.plugin.ui.PocketPaginatedMenu;
import com.pocketworld.plugin.ui.world_menus.PocketWorldMainMenu;
import com.pocketworld.plugin.world.PocketWorld;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class WorldManagementListMenu extends PocketPaginatedMenu {

    private final PocketWorldMainMenu mainMenu;
    private final List<PocketWorld> worlds;

    public WorldManagementListMenu(Player player, PocketWorldPlugin plugin, List<PocketWorld> worlds,
                                    PocketWorldMainMenu mainMenu) {
        super(player, plugin);
        this.mainMenu = mainMenu;
        this.worlds = worlds;
    }

    @Override
    public String getMenuName() {
        return "&4&lManage Your PocketWorlds";
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setMenuItems() {
        addMenuBorder(Material.BLUE_STAINED_GLASS_PANE);

        for (int i = 0; i < maxItemsPerPage; i++) {
            index = maxItemsPerPage * page + i;
            if (index >= worlds.size()) {
                break;
            }

            PocketWorld world = worlds.get(index);
            if (world == null) {
                continue;
            }

            String members = world.getUsers().keySet().stream()
                    .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                    .collect(Collectors.joining(", "));
            String status = world.isLoaded() ? "&aLOADED" : "&cNOT LOADED";

            List<String> lore = new ArrayList<>(List.of("&7Click to manage this world.", " ",
                    "&6Properties", "&eBiome: " + world.getBiome(), "&eMembers: " + members,
                    "&eWorld Size: " + world.getWorldSize() + "x" + world.getWorldSize(),
                    " ", status, "&8" + world.getId()));
            if (isPlayerInside(world)) {
                lore.add("&aYou are currently inside this PocketWorld");
            }

            ItemStack worldIcon = new PocketItem.Builder(plugin)
                    .material(world.getIcon())
                    .displayName("&b" + world.getWorldName())
                    .lore(lore)
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
            mainMenu.open();
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
            Optional<PocketWorld> optionalWorld = worlds.stream().filter(t -> t.getId().equals(worldId)).findFirst();
            optionalWorld.ifPresent(world -> new ManageWorldMenu(player, plugin, world, this).open());
        }
    }

    private boolean isPlayerInside(PocketWorld world) {
        return player.getWorld().getName().equals(world.getId().toString());
    }
}
