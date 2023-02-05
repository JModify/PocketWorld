package me.modify.pocketworld.menu.worldmenus;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.menu.PocketMenu;
import me.modify.pocketworld.menu.worldmenus.teleport.WorldTeleportMainMenu;
import me.modify.pocketworld.util.PocketItem;
import me.modify.pocketworld.menu.worldmenus.creation.WorldCreationMainMenu;
import me.modify.pocketworld.menu.worldmenus.management.WorldManagementMainMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class PocketWorldMainMenu extends PocketMenu {


    private final PocketWorldPlugin plugin;
    public PocketWorldMainMenu(Player player, PocketWorldPlugin plugin) {
        super(player);
        this.plugin = plugin;
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
        PocketItem worldCreation = new PocketItem.Builder(plugin)
                .material(Material.CRAFTING_TABLE)
                .stackSize(1)
                .displayName("&aWorld Creation")
                .lore(List.of("&7Create a new pocket world."," ", "&fMaximum: " + maxWorlds))
                .tag("world-creation-icon")
                .build();

        int worldCount = plugin.getDataSource().getConnection().getDAO().countPocketWorlds(player.getUniqueId());
        PocketItem worldManagement = new PocketItem.Builder(plugin)
                .material(Material.ANVIL)
                .stackSize(1)
                .displayName("&aWorld Management")
                .lore(List.of("&7Manage an existing pocket world.", " ", "&fWorld Count: " + worldCount))
                .tag("world-management-icon")
                .build();

        PocketItem worldTeleport = new PocketItem.Builder(plugin)
                .material(Material.ENDER_PEARL)
                .stackSize(1)
                .displayName("&aWorld Teleportation")
                .lore(List.of("&7Travel to a pocket world."))
                .tag("world-teleportation-icon")
                .build();

        PocketItem fillerItem = new PocketItem.Builder(plugin)
                .material(Material.BLACK_STAINED_GLASS_PANE)
                .stackSize(1)
                .displayName(" ")
                .build();

        inventory.setItem(11, worldCreation.get());
        inventory.setItem(13, worldManagement.get());
        inventory.setItem(15, worldTeleport.get());

        // Add filler items in a border formation
        addFillers(fillerItem.get(), 0, 8);
        addFillers(fillerItem.get(), 27, 35);
        inventory.setItem(9, fillerItem.get());
        inventory.setItem(17, fillerItem.get());
        inventory.setItem(18, fillerItem.get());
        inventory.setItem(26, fillerItem.get());
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
            WorldCreationMainMenu worldCreationMainMenu = new WorldCreationMainMenu(player, plugin, this);
            worldCreationMainMenu.open();
        } else if (tag.equalsIgnoreCase("world-management-icon")) {
            WorldManagementMainMenu managementMainMenu = new WorldManagementMainMenu(player, plugin, this);
            managementMainMenu.open();
        } else if (tag.equalsIgnoreCase("world-teleportation-icon")) {
            WorldTeleportMainMenu teleportMainMenu = new WorldTeleportMainMenu(player, plugin, this);
            teleportMainMenu.open();
        }
    }
}
