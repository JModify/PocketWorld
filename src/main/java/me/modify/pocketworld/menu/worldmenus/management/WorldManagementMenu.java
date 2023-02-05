package me.modify.pocketworld.menu.worldmenus.management;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.menu.PocketMenu;
import me.modify.pocketworld.util.PocketItem;
import me.modify.pocketworld.world.PocketWorld;
import me.modify.pocketworld.world.WorldRank;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WorldManagementMenu extends PocketMenu {

    private WorldManagementMainMenu previousMenu;

    /** World being managed */
    private PocketWorld world;

    private PocketWorldPlugin plugin;
    public WorldManagementMenu(Player player, PocketWorldPlugin plugin, PocketWorld world, WorldManagementMainMenu previousMenu) {
        super(player);
        this.plugin = plugin;
        this.world = world;
        this.previousMenu = previousMenu;
    }

    @Override
    public String getMenuName() {
        return "&4&lManage World";
    }

    @Override
    public int getMenuSlots() {
        return 36;
    }

    @Override
    public void setMenuItems() {
        Inventory inventory = getInventory();

        String members = world.getUsers().keySet().stream()
                .map(uuid -> Bukkit.getPlayer(uuid).getName())
                .collect(Collectors.joining(", "));

        ItemStack globe = new PocketItem.Builder(plugin)
                .material(Material.PLAYER_HEAD)
                .displayName("&d" + world.getWorldName())
                .lore(List.of("&7Members (" + world.getUsers().size() + "): " + members, " ", "&8" + world.getId().toString()))
                .build().getAsSkull("BlockminersTV");

        ItemStack playerManagement = new PocketItem.Builder(plugin)
                .material(Material.BOOK)
                .displayName("&cUser Management")
                .lore(List.of("&7View or manage world members here."))
                .tag("world-player-management")
                .build().get();

        ItemStack homePage = new PocketItem.Builder(plugin)
                .material(Material.ARROW)
                .stackSize(1)
                .displayName("&aMain Menu")
                .lore(List.of("&7Click to return to main menu."))
                .tag("is-home-button")
                .build().get();

        ItemStack leaveOrDelete = getLeaveOrDeleteWorldIcon();

        inventory.setItem(13, globe);
        inventory.setItem(20, playerManagement);
        inventory.setItem(24, leaveOrDelete);
        inventory.setItem(27, homePage);

        // Only world owners have access to edit world properties.
        if (world.getUsers().get(player.getUniqueId()) == WorldRank.OWNER) {
            ItemStack worldProperties = new PocketItem.Builder(plugin)
                    .material(Material.DIAMOND_AXE)
                    .displayName("&cWorld Properties")
                    .lore(List.of("&7Manage world properties."))
                    .tag("world-properties")
                    .build().get();

            inventory.setItem(22, worldProperties);
        }

        ItemStack fillerItem = new PocketItem.Builder(plugin)
                .material(Material.BLACK_STAINED_GLASS_PANE)
                .stackSize(1)
                .displayName(" ")
                .build().get();

        // Top and bottom filler rows
        addFillers(fillerItem, 0, 8);
        addFillers(fillerItem, 27, 35);

        // Filler items on left and right border
        inventory.setItem(9, fillerItem);
        inventory.setItem(17, fillerItem);
        inventory.setItem(18, fillerItem);
        inventory.setItem(26, fillerItem);
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

        if (tag.equalsIgnoreCase("player-management")) {
            // Open player management menu specific to world rank

        } else if (tag.equalsIgnoreCase("world-properties")) {
            WorldPropertiesMenu propertiesMenu = new WorldPropertiesMenu(player, plugin, world, this);
            propertiesMenu.open();
        } else if (tag.equalsIgnoreCase("world-leave-or-delete")) {
            // Open leave or delete confirmation menu
        } else if (tag.equalsIgnoreCase("is-home-button")) {
            previousMenu.open();
        }
    }

    private ItemStack getLeaveOrDeleteWorldIcon() {
        WorldRank rank = world.getUsers().get(player.getUniqueId());

        if (rank == null) {
            plugin.getLogger().severe("Failed to get world rank of " + player.getName() + ". " +
                    "Player opening menu when they are not a member of the world?");
            return null;
        }

        String displayName = "&cLeave World";
        List<String> lore = new ArrayList<>();
        if (rank == WorldRank.OWNER) {
            displayName = "&cDelete World";
            lore.add("&7Permanently delete this pocket world.");
        }else {
            lore.add("&7Leave this pocket world.");
        }

        PocketItem leaveOrDelete = new PocketItem.Builder(plugin)
                .material(Material.BARRIER)
                .displayName(displayName)
                .lore(lore)
                .tag("world-leave-or-delete")
                .build();

        return leaveOrDelete.get();
    }

}
