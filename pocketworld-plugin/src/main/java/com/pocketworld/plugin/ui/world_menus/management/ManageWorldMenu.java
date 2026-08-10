package com.pocketworld.plugin.ui.world_menus.management;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.ui.PocketItem;
import com.pocketworld.plugin.ui.PocketMenu;
import com.pocketworld.plugin.ui.world_menus.management.user_manage.PlayerManagementListMenu;
import com.pocketworld.plugin.util.MessageReader;
import com.pocketworld.plugin.world.PocketWorld;
import com.pocketworld.plugin.world.WorldAction;
import com.pocketworld.plugin.world.WorldRank;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ManageWorldMenu extends PocketMenu {

    private final WorldManagementListMenu previousMenu;
    private final PocketWorld world;

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

    @SuppressWarnings("deprecation")
    @Override
    public void setMenuItems() {
        Inventory inventory = getInventory();

        ItemStack globe = new PocketItem.Builder(plugin)
                .material(Material.PLAYER_HEAD)
                .displayName("&d" + world.getWorldName())
                .lore(List.of("&7Members (" + world.getUsers().size() + "): " + world.getMembersFormatted(", "),
                        "&7World Size: " + world.getWorldSizeFormatted(), " ", "&8" + world.getId()))
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

        if (world.hasPermission(player.getUniqueId(), WorldAction.SET_SPAWN)) {
            ItemStack spawnPoint = new PocketItem.Builder(plugin)
                    .material(Material.ENDER_EYE)
                    .displayName("&5Spawn Point")
                    .lore(List.of("&7Click to set world spawn point to your current position.",
                            "&7You must be standing in your pocket world to set this."))
                    .tag("world-spawn-point")
                    .build().get();

            inventory.setItem(16, spawnPoint);
        }

        if (world.getUsers().get(player.getUniqueId()) == WorldRank.OWNER) {
            ItemStack worldProperties = new PocketItem.Builder(plugin)
                    .material(Material.DIAMOND_AXE)
                    .displayName("&cWorld Properties")
                    .lore(List.of("&7Manage world properties."))
                    .tag("world-properties")
                    .build().get();

            inventory.setItem(22, worldProperties);

            ItemStack permissions = new PocketItem.Builder(plugin)
                    .material(Material.IRON_BARS)
                    .displayName("&cPermissions")
                    .lore(List.of("&7Configure what visitors, members and mods can do here."))
                    .tag("world-permissions")
                    .build().get();

            inventory.setItem(11, permissions);

            ItemStack expelVisitors = new PocketItem.Builder(plugin)
                    .material(Material.IRON_DOOR)
                    .displayName("&cExpel Visitors")
                    .lore(List.of("&7Teleport every non-member currently in this world back out."))
                    .tag("world-expel-visitors")
                    .build().get();

            inventory.setItem(15, expelVisitors);
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
            new PlayerManagementListMenu(player, plugin, world, this).open();
        } else if (tag.equalsIgnoreCase("world-properties")) {
            new WorldPropertiesMenu(player, plugin, world, this).open();
        } else if (tag.equalsIgnoreCase("world-permissions")) {
            new WorldPermissionsMenu(player, plugin, world, this).open();
        } else if (tag.equalsIgnoreCase("world-expel-visitors")) {
            expelVisitors();
        } else if (tag.equalsIgnoreCase("world-spawn-point")) {
            setSpawnPoint();
        } else if (tag.equalsIgnoreCase("world-leave")) {
            new LeaveWorldConfirmationMenu(player, plugin, world, this).open();
        } else if (tag.equalsIgnoreCase("world-delete")) {
            new DeleteWorldConfirmationMenu(player, plugin, world, this).open();
        } else if (tag.equalsIgnoreCase("is-back-button")) {
            previousMenu.open();
        }
    }

    private void setSpawnPoint() {
        if (!world.hasPermission(player.getUniqueId(), WorldAction.SET_SPAWN)) {
            return;
        }

        Location playerLoc = player.getLocation();
        MessageReader reader = plugin.getMessageReader();
        if (playerLoc.getWorld() == null || !playerLoc.getWorld().getName().equals(world.getId().toString())) {
            reader.send("world-set-spawn-outside", player);
            return;
        }

        world.setWorldSpawn(playerLoc);
        player.closeInventory();
        reader.send("world-set-spawn-success", player);
    }

    /** Teleports every player physically in this world who isn't a member back to the default world. */
    private void expelVisitors() {
        World bukkitWorld = Bukkit.getWorld(world.getId().toString());
        if (bukkitWorld == null) {
            return;
        }

        World defaultWorld = Bukkit.getWorlds().get(0);
        for (Player visitor : bukkitWorld.getPlayers()) {
            if (!world.getUsers().containsKey(visitor.getUniqueId())) {
                visitor.teleport(defaultWorld.getSpawnLocation());
            }
        }

        plugin.getMessageReader().send("world-visitors-expelled", player, "{WORLD_NAME}:" + world.getWorldName());
        player.closeInventory();
    }

    private ItemStack getLeaveOrDeleteWorldIcon() {
        WorldRank rank = world.getUsers().get(player.getUniqueId());

        if (rank == null) {
            plugin.getLogger().severe("Failed to get world rank of " + player.getName() + " in world "
                    + world.getId() + ". Player opening menu when they are not a member of the world?");
            return null;
        }

        String displayName = "&cLeave World";
        List<String> lore = new ArrayList<>();
        String leaveOrDeleteTag = "leave";

        if (rank == WorldRank.OWNER) {
            displayName = "&cDelete World";
            leaveOrDeleteTag = "delete";
            lore.add("&7Permanently delete this pocket world.");
        } else {
            lore.add("&7Leave this pocket world.");
        }

        return new PocketItem.Builder(plugin)
                .material(Material.BARRIER)
                .displayName(displayName)
                .lore(lore)
                .tag("world-" + leaveOrDeleteTag)
                .build().get();
    }
}
