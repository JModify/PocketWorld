package com.pocketworld.plugin.ui.world_menus;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.ui.PocketItem;
import com.pocketworld.plugin.ui.PocketMenu;
import com.pocketworld.plugin.ui.world_menus.creation.WorldCreationMainMenu;
import com.pocketworld.plugin.ui.world_menus.invitations.IncomingInvitationsMenu;
import com.pocketworld.plugin.ui.world_menus.management.WorldManagementListMenu;
import com.pocketworld.plugin.ui.world_menus.teleport.WorldTeleportMainMenu;
import com.pocketworld.plugin.user.PocketUser;
import com.pocketworld.plugin.world.PocketWorld;
import org.bukkit.Bukkit;
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

    private final List<PocketWorld> worlds;

    public PocketWorldMainMenu(Player player, PocketWorldPlugin plugin, List<PocketWorld> worlds) {
        super(player, plugin);
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
                .lore(List.of("&7Create a new pocket world.", " ", "&8Maximum Worlds: " + maxWorlds))
                .tag("world-creation-icon")
                .build().get();

        ItemStack worldManagement = new PocketItem.Builder(plugin)
                .material(Material.ANVIL)
                .displayName("&aWorld Management")
                .lore(List.of("&7Manage an existing pocket world.", " ", "&8World Count: " + worlds.size()))
                .tag("world-management-icon")
                .build().get();

        ItemStack worldTeleport = new PocketItem.Builder(plugin)
                .material(Material.ENDER_PEARL)
                .displayName("&aWorld Teleportation")
                .lore(List.of("&7Travel to a pocket world."))
                .tag("world-teleportation-icon")
                .build().get();

        ItemStack invitations = new PocketItem.Builder(plugin)
                .material(Material.BOOK)
                .glow(true)
                .displayName("&aWorld Invitations")
                .lore(List.of("&7Accept/decline world invitations."))
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
            if (worlds.size() >= maxWorlds) {
                plugin.getMessageReader().send("world-creation-max-worlds", player);
                player.closeInventory();
                return;
            }
            new WorldCreationMainMenu(player, plugin, this).open();
        } else if (tag.equalsIgnoreCase("world-management-icon")) {
            new WorldManagementListMenu(player, plugin, worlds, this).open();
        } else if (tag.equalsIgnoreCase("world-teleportation-icon")) {
            new WorldTeleportMainMenu(player, plugin, worlds, this).open();
        } else if (tag.equalsIgnoreCase("world-invitations-icon")) {
            openInvitationsMenu();
        }
    }

    private void openInvitationsMenu() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<PocketWorld> invitedWorlds = new ArrayList<>();
            PocketUser user = plugin.getUserCache().readThrough(player.getUniqueId());
            for (UUID worldId : Set.copyOf(user.getInvitations())) {
                PocketWorld world = plugin.getWorldCache().readThrough(worldId);
                if (world != null) {
                    invitedWorlds.add(world);
                } else {
                    // Deleted/revoked since the user's own invitation reference was recorded.
                    user.getInvitations().remove(worldId);
                }
            }

            Bukkit.getScheduler().runTask(plugin, () -> new IncomingInvitationsMenu(player, plugin, invitedWorlds, this).open());
        });
    }
}
