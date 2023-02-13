package me.modify.pocketworld.menu.worldmenus;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.data.DAO;
import me.modify.pocketworld.menu.PocketMenu;
import me.modify.pocketworld.menu.worldmenus.teleport.WorldTeleportMainMenu;
import me.modify.pocketworld.user.PocketUser;
import me.modify.pocketworld.util.PocketItem;
import me.modify.pocketworld.menu.worldmenus.creation.WorldCreationMainMenu;
import me.modify.pocketworld.menu.worldmenus.management.WorldManagementListMenu;
import me.modify.pocketworld.world.PocketWorld;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PocketWorldMainMenu extends PocketMenu {

    private List<PocketWorld> worlds;
    public PocketWorldMainMenu(Player player, PocketWorldPlugin plugin) {
        super(player, plugin);
        this.plugin = plugin;
        this.worlds = new ArrayList<>();
        cacheWorlds();
    }

    /**
     * Caches worlds which aren't already cached and adds all worlds a user is associated
     * to a sub list - to be iterated over later.
     */
    private void cacheWorlds() {
        PocketUser user = plugin.getDataSource().getConnection().getDAO().getPocketUser(player.getUniqueId());
        Set<UUID> worldIds = user.getWorlds();
        worldIds.forEach(id -> {
            PocketWorld world = plugin.getWorldCache().get(id);

            if (world != null) {
                worlds.add(world);
            }
        });
    }

    @Override
    public String getMenuName() {
        return "&4&lPocketWorld";
    }

    @Override
    public int getMenuSlots() {
        return 36;
    }

    @Override
    public void setMenuItems() {
        Inventory inventory = getInventory();

        int maxWorlds = plugin.getConfigFile().getYaml().getInt("general.max-worlds", 5);
        ItemStack worldCreation = new PocketItem.Builder(plugin)
                .material(Material.CRAFTING_TABLE)
                .displayName("&aWorld Creation")
                .lore(List.of("&7Create a new pocket world."," ", "&8Maximum Worlds: " + maxWorlds))
                .tag("world-creation-icon")
                .build().get();

        int worldCount = plugin.getDataSource().getConnection().getDAO().countPocketWorlds(player.getUniqueId());
        ItemStack worldManagement = new PocketItem.Builder(plugin)
                .material(Material.ANVIL)
                .displayName("&aWorld Management")
                .lore(List.of("&7Manage an existing pocket world.", " ", "&8World Count: " + worldCount))
                .tag("world-management-icon")
                .build().get();

        ItemStack worldTeleport = new PocketItem.Builder(plugin)
                .material(Material.ENDER_PEARL)
                .displayName("&aWorld Teleportation")
                .lore(List.of("&7Travel to a pocket world."))
                .tag("world-teleportation-icon")
                .build().get();

        inventory.setItem(11, worldCreation);
        inventory.setItem(13, worldManagement);
        inventory.setItem(15, worldTeleport);

        ItemStack fillerItem = new PocketItem.Builder(plugin)
                .material(Material.BLACK_STAINED_GLASS_PANE)
                .stackSize(1)
                .displayName(" ")
                .build().get();
        addFillerBorder(fillerItem);
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

        if (tag.equalsIgnoreCase("world-creation-icon")) {
            int maxWorlds = plugin.getConfigFile().getYaml().getInt("general.max-worlds", 5);
            int worldCount = plugin.getDataSource().getConnection().getDAO().countPocketWorlds(player.getUniqueId());

            if (worldCount >= maxWorlds) {
                player.sendMessage("&4&lERROR &r&cMaximum PocketWorld's reached.");
                player.closeInventory();
                return;
            }

            WorldCreationMainMenu worldCreationMainMenu = new WorldCreationMainMenu(player, plugin, this);
            worldCreationMainMenu.open();
        } else if (tag.equalsIgnoreCase("world-management-icon")) {
            WorldManagementListMenu managementMainMenu = new WorldManagementListMenu(player, plugin, worlds, this);
            managementMainMenu.open();
        } else if (tag.equalsIgnoreCase("world-teleportation-icon")) {
            WorldTeleportMainMenu teleportMainMenu = new WorldTeleportMainMenu(player, plugin, worlds, this);
            teleportMainMenu.open();
        }
    }
}
