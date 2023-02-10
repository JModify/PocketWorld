package me.modify.pocketworld.menu.worldmenus.teleport;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.data.DAO;
import me.modify.pocketworld.menu.PocketPaginatedMenu;
import me.modify.pocketworld.menu.worldmenus.PocketWorldMainMenu;
import me.modify.pocketworld.util.PocketItem;
import me.modify.pocketworld.world.PocketWorld;
import me.modify.pocketworld.world.LoadedWorldRegistry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class WorldTeleportMainMenu extends PocketPaginatedMenu {

    private PocketWorldMainMenu mainMenu;
    private List<PocketWorld> worlds;

    public WorldTeleportMainMenu(Player player, PocketWorldPlugin plugin, PocketWorldMainMenu mainMenu) {
        super(player, plugin);
        this.mainMenu = mainMenu;
    }

    @Override
    public String getMenuName() {
        return "&4&lTravel to a PocketWorld";
    }

    @Override
    public void setMenuItems() {

        addMenuBorder(Material.PURPLE_STAINED_GLASS_PANE);

        DAO dao = plugin.getDataSource().getConnection().getDAO();
        worlds = dao.getPocketWorlds(player.getUniqueId());

        if (!worlds.isEmpty()) {
            for (int i = 0; i < maxItemsPerPage; i++) {
                index = maxItemsPerPage * page + i;
                if (index >= worlds.size()) break;
                PocketWorld world = worlds.get(i);

                if (world == null) continue;

                String members = world.getUsers().keySet().stream()
                        .map(uuid -> Bukkit.getPlayer(uuid).getName())
                        .collect(Collectors.joining(", "));

                String status = LoadedWorldRegistry.getInstance().containsWorld(world.getId())
                        ? "&aLOADED" : "&cNOT LOADED";

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

            LoadedWorldRegistry registry = LoadedWorldRegistry.getInstance();

            // If world is loaded, just teleport to it
            if (registry.containsWorld(worldId)) {
                PocketWorld world = registry.getWorld(worldId);
                World bWorld = Bukkit.getWorld(worldId.toString());
                world.teleport(bWorld, player);
            } else {
                // If world is not loaded, load it then teleport.
                DAO dao = plugin.getDataSource().getConnection().getDAO();
                PocketWorld pocketWorld = dao.getPocketWorld(worldId);
                pocketWorld.load(plugin, player.getUniqueId(), true, false);
            }
        }
    }
}
