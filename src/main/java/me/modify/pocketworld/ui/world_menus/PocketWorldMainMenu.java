package me.modify.pocketworld.ui.world_menus;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.ui.PocketMenu;
import me.modify.pocketworld.ui.world_menus.invitations.IncomingInvitationsMenu;
import me.modify.pocketworld.ui.world_menus.teleport.WorldTeleportMainMenu;
import me.modify.pocketworld.user.PocketUser;
import me.modify.pocketworld.ui.PocketItem;
import me.modify.pocketworld.ui.world_menus.creation.WorldCreationMainMenu;
import me.modify.pocketworld.ui.world_menus.management.WorldManagementListMenu;
import me.modify.pocketworld.util.ColorFormat;
import me.modify.pocketworld.world.PocketWorld;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Consumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PocketWorldMainMenu extends PocketMenu {

    private final List<PocketWorld> worlds;
    public PocketWorldMainMenu(Player player, PocketWorldPlugin plugin, List<PocketWorld> worlds) {
        super(player, plugin);
        this.plugin = plugin;
        this.worlds = worlds;
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

        int worldCount = worlds.size();
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

        int invitationsCount = plugin.getUserCache().readThrough(player.getUniqueId()).getInvitations().size();
        ItemStack invitations = new PocketItem.Builder(plugin)
                .material(Material.BOOK)
                .glow(true)
                .displayName("&aWorld Invitations")
                .lore(List.of("&7Accept/decline world invitations.", "&8Pending Invitations: " + invitationsCount))
                .tag("world-invitations-icon")
                .build().get();

        inventory.setItem(11, worldCreation);
        inventory.setItem(13, worldManagement);
        inventory.setItem(15, worldTeleport);
        inventory.setItem(22, invitations);

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
            int worldCount = worlds.size();

            if (worldCount >= maxWorlds) {
                player.sendMessage(ColorFormat.format("&4&lERROR &r&cMaximum PocketWorld's reached."));
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
        } else if (tag.equalsIgnoreCase("world-invitations-icon")) {

            plugin.getUserCache().readThrough(player.getUniqueId()).getInvitations().forEach(u -> plugin.getDebugger().severe(u.toString()));

            Consumer<List<PocketWorld>> invitedWorldsConsumer = incomingInvitations -> {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        plugin.getDebugger().severe("Called open IncomingInvitationsMenu");
                        IncomingInvitationsMenu incomingInvitationsMenu = new IncomingInvitationsMenu(player, plugin,
                                incomingInvitations, PocketWorldMainMenu.this);
                        incomingInvitationsMenu.open();
                    }
                }.runTask(plugin);
            };

            new BukkitRunnable() {
                @Override
                public void run() {
                    List<PocketWorld> invitedWorlds = new ArrayList<>();
                    PocketUser user = plugin.getUserCache().readThrough(player.getUniqueId());
                    for (UUID worldInvited : user.getInvitations()) {
                        PocketWorld world = plugin.getWorldCache().readThrough(worldInvited);
                        // If the world is not null, add it to list of invited worlds
                        if (world != null) {
                            invitedWorlds.add(plugin.getWorldCache().readThrough(worldInvited));
                        } else {
                            // If it is null, it has been deleted and so user reference to it should be removed.
                            // World is also not included in list f invited worlds.
                            user.getInvitations().remove(worldInvited);
                        }
                    }

                    invitedWorldsConsumer.accept(invitedWorlds);
                    plugin.getDebugger().severe("Accepted consumer");
                }
            }.runTaskAsynchronously(plugin);

        }
    }
}
