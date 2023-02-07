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

public class ManageWorldMenu extends PocketMenu {

    private WorldManagementListMenu previousMenu;

    /** World being managed */
    private PocketWorld world;

    public ManageWorldMenu(Player player, PocketWorldPlugin plugin, PocketWorld world, WorldManagementListMenu previousMenu) {
        super(player, plugin);
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
                .lore(List.of("&7Members (" + world.getUsers().size() + "): " + members,
                        "&7Size: " + world.getWorldSizeFormatted(), " ", "&8" + world.getId().toString()))
                .build().getAsSkull("BlockminersTV");

        ItemStack playerManagement = new PocketItem.Builder(plugin)
                .material(Material.BOOK)
                .displayName("&cUser Management")
                .lore(List.of("&7View or manage world members here."))
                .tag("world-player-management")
                .build().get();

        ItemStack backButton = new PocketItem.Builder(plugin)
                .material(Material.ARROW)
                .stackSize(1)
                .displayName("&aMain Menu")
                .lore(List.of("&7Click to return to previous menu."))
                .tag("is-back-button")
                .build().get();

        ItemStack leaveOrDelete = getLeaveOrDeleteWorldIcon();

        inventory.setItem(13, globe);
        inventory.setItem(20, playerManagement);
        inventory.setItem(24, leaveOrDelete);
        inventory.setItem(27, backButton);

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
                .material(Material.BLUE_STAINED_GLASS_PANE)
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

        if (tag.equalsIgnoreCase("world-player-management")) {
            PlayerManagementListMenu playerMenu = new PlayerManagementListMenu(player, plugin, world, this);
            playerMenu.open();
        } else if (tag.equalsIgnoreCase("world-properties")) {
            WorldPropertiesMenu propertiesMenu = new WorldPropertiesMenu(player, plugin, world, this);
            propertiesMenu.open();
        } else if (tag.equalsIgnoreCase("world-leave")) {
            LeaveWorldConfirmationMenu confirmationMenu = new LeaveWorldConfirmationMenu(player, plugin, world, this);
            confirmationMenu.open();
        } else if (tag.equalsIgnoreCase("world-delete")) {
            DeleteWorldConfirmationMenu confirmationMenu = new DeleteWorldConfirmationMenu(player, plugin, world, this);
            confirmationMenu.open();
        }else if (tag.equalsIgnoreCase("is-back-button")) {
            previousMenu.open();
        }
    }

    private ItemStack getLeaveOrDeleteWorldIcon() {
        WorldRank rank = world.getUsers().get(player.getUniqueId());

        if (rank == null) {
            plugin.getLogger().severe("Failed to get world rank of " + player.getName() + " in world "
                    + world.getId().toString()  + ". " +
                    "Player opening menu when they are not a member of the world?");
            return null;
        }

        String displayName = "&cLeave World";
        List<String> lore = new ArrayList<>();
        String leaveOrDeleteTag = "leave";

        if (rank == WorldRank.OWNER) {
            displayName = "&cDelete World";
            leaveOrDeleteTag = "delete";
            lore.add("&7Permanently delete this pocket world.");
        }else {
            lore.add("&7Leave this pocket world.");
        }

        PocketItem leaveOrDelete = new PocketItem.Builder(plugin)
                .material(Material.BARRIER)
                .displayName(displayName)
                .lore(lore)
                .tag("world-" + leaveOrDeleteTag)
                .build();

        return leaveOrDelete.get();
    }

}
