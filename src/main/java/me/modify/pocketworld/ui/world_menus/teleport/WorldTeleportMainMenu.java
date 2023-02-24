package me.modify.pocketworld.ui.world_menus.teleport;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.ui.PocketPaginatedMenu;
import me.modify.pocketworld.ui.world_menus.PocketWorldMainMenu;
import me.modify.pocketworld.ui.PocketItem;
import me.modify.pocketworld.world.PocketWorld;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class WorldTeleportMainMenu extends PocketPaginatedMenu {

    private List<PocketWorld> worlds;
    private final PocketWorldMainMenu mainMenu;

    public WorldTeleportMainMenu(Player player, PocketWorldPlugin plugin, List<PocketWorld> worlds,
                                 PocketWorldMainMenu mainMenu) {
        super(player, plugin);
        this.mainMenu = mainMenu;
        this.worlds = worlds;
    }

    @Override
    public String getMenuName() {
        return "&4&lTravel to a PocketWorld";
    }

    @Override
    public void setMenuItems() {

        addMenuBorder(Material.PURPLE_STAINED_GLASS_PANE);

        if (!worlds.isEmpty()) {
            for (int i = 0; i < maxItemsPerPage; i++) {
                index = maxItemsPerPage * page + i;
                if (index >= worlds.size()) break;
                PocketWorld world = worlds.get(i);

                if (world == null) continue;

                String members = world.getUsers().keySet().stream()
                        .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                        .collect(Collectors.joining(", "));

                String status = world.isLoaded() ? "&aLOADED" : "&cNOT LOADED";
                ItemStack worldIcon = new PocketItem.Builder(plugin)
                        .material(world.getIcon())
                        .stackSize(1)
                        .displayName("&b" + world.getWorldName())
                        .lore(List.of("&7Click to teleport to this world.", " ",
                                "&6Properties", "&eBiome: " + world.getBiome(), "&eMembers: " + members,
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
                return;
            }
            PocketWorld world = optionalWorld.get();

            if (!world.isLoaded()) {
                world.load(plugin, player.getUniqueId(), true, false);
                return;
            }

            world.teleport(player);
        }
    }
}
