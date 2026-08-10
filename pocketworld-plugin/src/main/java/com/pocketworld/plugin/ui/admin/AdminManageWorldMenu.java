package com.pocketworld.plugin.ui.admin;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.ui.PocketItem;
import com.pocketworld.plugin.ui.PocketMenu;
import com.pocketworld.plugin.world.PocketWorld;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** An admin's view of a single pocket world - view members, resize, wipe, or teleport to/into it,
 *  all regardless of the admin's own membership (or lack of it) in the world. */
public class AdminManageWorldMenu extends PocketMenu {

    private final PocketWorld world;
    private final Runnable onBack;

    public AdminManageWorldMenu(Player player, PocketWorldPlugin plugin, PocketWorld world, Runnable onBack) {
        super(player, plugin);
        this.world = world;
        this.onBack = onBack;
    }

    @Override
    public String getMenuName() {
        return "&4&lAdmin: " + world.getWorldName();
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

        ItemStack viewMembers = new PocketItem.Builder(plugin)
                .material(Material.BOOK)
                .displayName("&aView Members")
                .lore(List.of("&7View every member of this world and their rank."))
                .tag("admin-view-members")
                .build().get();

        ItemStack setSize = new PocketItem.Builder(plugin)
                .material(Material.GRASS_BLOCK)
                .displayName("&aSet World Size")
                .lore(List.of("&7Change this world's border size."))
                .tag("admin-set-size")
                .build().get();

        ItemStack teleport = new PocketItem.Builder(plugin)
                .material(Material.ENDER_PEARL)
                .displayName("&aTeleport")
                .lore(List.of("&7Teleport yourself or another player here."))
                .tag("admin-teleport")
                .build().get();

        ItemStack wipe = new PocketItem.Builder(plugin)
                .material(Material.BARRIER)
                .displayName("&cWipe World")
                .lore(List.of("&7Permanently delete this pocket world."))
                .tag("admin-wipe-world")
                .build().get();

        ItemStack back = new PocketItem.Builder(plugin)
                .material(Material.ARROW)
                .stackSize(1)
                .displayName("&aReturn")
                .lore(List.of("&7Click to return to previous menu."))
                .tag("is-back-button")
                .build().get();

        inventory.setItem(11, viewMembers);
        inventory.setItem(13, globe);
        inventory.setItem(15, setSize);
        inventory.setItem(20, teleport);
        inventory.setItem(24, wipe);
        inventory.setItem(31, back);

        ItemStack fillerItem = new PocketItem.Builder(plugin)
                .material(Material.GRAY_STAINED_GLASS_PANE)
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

        if (tag.equalsIgnoreCase("admin-view-members")) {
            new AdminViewMembersMenu(player, plugin, world, this::open).open();
        } else if (tag.equalsIgnoreCase("admin-set-size")) {
            new AdminWorldSizeMenu(player, plugin, world, this::open).open();
        } else if (tag.equalsIgnoreCase("admin-teleport")) {
            new AdminTeleportMenu(player, plugin, world, this::open).open();
        } else if (tag.equalsIgnoreCase("admin-wipe-world")) {
            new AdminWipeWorldConfirmationMenu(player, plugin, world, this::open).open();
        } else if (tag.equalsIgnoreCase("is-back-button")) {
            onBack.run();
        }
    }
}
