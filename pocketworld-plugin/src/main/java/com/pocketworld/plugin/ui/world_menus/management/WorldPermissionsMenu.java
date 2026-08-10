package com.pocketworld.plugin.ui.world_menus.management;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.ui.PocketItem;
import com.pocketworld.plugin.ui.PocketMenu;
import com.pocketworld.plugin.world.PermissionRank;
import com.pocketworld.plugin.world.PocketWorld;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Owner-only entry point for configuring the {@link com.pocketworld.plugin.world.WorldAction}s each
 * non-owner {@link PermissionRank} is allowed to perform in this world - one icon per rank, opening
 * {@link WorldPermissionRankMenu} for the actual toggles.
 */
public class WorldPermissionsMenu extends PocketMenu {

    private final PocketWorld world;
    private final ManageWorldMenu previousMenu;

    public WorldPermissionsMenu(Player player, PocketWorldPlugin plugin, PocketWorld world, ManageWorldMenu previousMenu) {
        super(player, plugin);
        this.world = world;
        this.previousMenu = previousMenu;
    }

    @Override
    public String getMenuName() {
        return "&4&lWorld Permissions";
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
                .lore(List.of("&7Choose a rank below to configure what it's allowed to do."))
                .build().getAsSkull("BlockminersTV");

        ItemStack visitor = new PocketItem.Builder(plugin)
                .material(Material.DIRT)
                .displayName("&eVisitor")
                .lore(List.of("&7Anyone physically in the world who isn't a member.",
                        "&7Controls: build, break, interact."))
                .tag("permission-rank-visitor")
                .build().get();

        ItemStack member = new PocketItem.Builder(plugin)
                .material(Material.COAL)
                .displayName("&eMember")
                .lore(List.of("&7Default world rank.",
                        "&7Controls: invite, kick, set spawn."))
                .tag("permission-rank-member")
                .build().get();

        ItemStack mod = new PocketItem.Builder(plugin)
                .material(Material.GOLD_INGOT)
                .displayName("&eMod")
                .lore(List.of("&7Elevated world rank.",
                        "&7Controls: invite, kick, set spawn."))
                .tag("permission-rank-mod")
                .build().get();

        ItemStack back = new PocketItem.Builder(plugin)
                .material(Material.ARROW)
                .stackSize(1)
                .displayName("&aGo Back")
                .lore(List.of("&7Click to return to previous menu."))
                .tag("is-back-button")
                .build().get();

        inventory.setItem(13, globe);
        inventory.setItem(20, visitor);
        inventory.setItem(22, member);
        inventory.setItem(24, mod);
        inventory.setItem(27, back);

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

        if (tag.equalsIgnoreCase("permission-rank-visitor")) {
            new WorldPermissionRankMenu(player, plugin, world, PermissionRank.VISITOR, this).open();
        } else if (tag.equalsIgnoreCase("permission-rank-member")) {
            new WorldPermissionRankMenu(player, plugin, world, PermissionRank.MEMBER, this).open();
        } else if (tag.equalsIgnoreCase("permission-rank-mod")) {
            new WorldPermissionRankMenu(player, plugin, world, PermissionRank.MOD, this).open();
        } else if (tag.equalsIgnoreCase("is-back-button")) {
            previousMenu.open();
        }
    }
}
