package me.modify.pocketworld.menu.worldmenus.management;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.menu.PocketPaginatedMenu;
import me.modify.pocketworld.menu.worldmenus.PocketWorldMainMenu;
import me.modify.pocketworld.util.PocketItem;
import me.modify.pocketworld.world.PocketWorld;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class WorldManagementListMenu extends PocketPaginatedMenu {

    private PocketWorldMainMenu mainMenu;
    private List<PocketWorld> worlds;
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

    @Override
    public void setMenuItems() {

        addMenuBorder(Material.BLUE_STAINED_GLASS_PANE);

        if (!worlds.isEmpty()) {
            for (int i = 0; i < maxItemsPerPage; i++) {
                index = maxItemsPerPage * page + i;
                if (index >= worlds.size()) break;

                PocketWorld world = worlds.get(i);

                if (world == null) continue;

                Material icon = world.getIcon();
                String members = world.getUsers().keySet().stream()
                        .map(uuid -> Bukkit.getPlayer(uuid).getName())
                        .collect(Collectors.joining(", "));

                String status = world.isLoaded() ? "&aLOADED" : "&cNOT LOADED";

                ItemStack worldIcon = new PocketItem.Builder(plugin)
                        .material(icon)
                        .displayName("&b" + world.getWorldName())
                        .lore(List.of("&7Click to manage this world.", " ",
                                "&6Properties", "&eBiome: " + world.getBiome(), "&eMembers: " + members,
                                "&eSize: " + world.getWorldSize() + "x" + world.getWorldSize(),
                                " ", status, "&8" + world.getId().toString()))
                        .tag(world.getId().toString())
                        .build().get();

                getInventory().addItem(worldIcon);
            }
        }

    }

    @Override
    public void handleMenuClick(InventoryClickEvent e) {
        e.setCancelled(true);

        // Will never be null, checked in event listener class.
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
            if (!((index + 1) >= worlds.size())) {
                page = page + 1;
                super.open();
            }
        } else if (tag.equalsIgnoreCase("is-page-back")) {
            if (page > 0) {
                page = page - 1;
                super.open();
            }
        } else {
            UUID worldId = UUID.fromString(ChatColor.stripColor(tag));
            Optional<PocketWorld> optionalWorld = worlds.stream().filter(t -> t.getId().equals(worldId)).findFirst();

            if (optionalWorld.isEmpty()) {
                // Will never be empty.
                return;
            }

            PocketWorld world = optionalWorld.get();
            ManageWorldMenu managementMenu = new ManageWorldMenu(player, plugin, world, this);
            managementMenu.open();
        }
    }
}
