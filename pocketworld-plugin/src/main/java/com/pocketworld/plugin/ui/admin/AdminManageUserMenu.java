package com.pocketworld.plugin.ui.admin;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.ui.PocketItem;
import com.pocketworld.plugin.ui.PocketMenu;
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

/**
 * The root of an admin's management view of a single target player - reachable either directly
 * from {@code /pocketworldadmin manage <player>} (no {@code onBack}, so the back button just closes
 * the inventory) or by clicking a skull in {@link AdminPlayerBrowserMenu} (which supplies one).
 */
public class AdminManageUserMenu extends PocketMenu {

    private final UUID targetId;
    private final String targetName;
    private final Runnable onBack;

    public AdminManageUserMenu(Player player, PocketWorldPlugin plugin, UUID targetId, String targetName, Runnable onBack) {
        super(player, plugin);
        this.targetId = targetId;
        this.targetName = targetName;
        this.onBack = onBack;
    }

    @Override
    public String getMenuName() {
        return "&4&lManage " + targetName;
    }

    @Override
    public int getMenuSlots() {
        return 27;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setMenuItems() {
        Inventory inventory = getInventory();

        ItemStack worlds = new PocketItem.Builder(plugin)
                .material(Material.GRASS_BLOCK)
                .displayName("&aWorlds")
                .lore(List.of("&7View every pocket world " + targetName + " is a member of."))
                .tag("admin-user-worlds")
                .build().get();

        ItemStack punish = new PocketItem.Builder(plugin)
                .material(Material.IRON_SWORD)
                .displayName("&cPunish")
                .lore(List.of("&7Punishment actions for " + targetName + "."))
                .tag("admin-user-punish")
                .build().get();

        ItemStack back = new PocketItem.Builder(plugin)
                .material(Material.ARROW)
                .stackSize(1)
                .displayName("&aReturn")
                .lore(List.of("&7Click to return to previous menu."))
                .tag("is-back-button")
                .build().get();

        inventory.setItem(11, worlds);
        inventory.setItem(15, punish);
        inventory.setItem(22, back);

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

        if (tag.equalsIgnoreCase("admin-user-worlds")) {
            openWorldsMenu();
        } else if (tag.equalsIgnoreCase("admin-user-punish")) {
            new AdminPunishMenu(player, plugin, targetId, targetName, this::open).open();
        } else if (tag.equalsIgnoreCase("is-back-button")) {
            if (onBack != null) {
                onBack.run();
            } else {
                player.closeInventory();
            }
        }
    }

    /** Resolves every world {@code targetId} is a member of off the main thread (mirrors the same
     *  cache-then-DAO lookup {@code CommandPocketWorld} does for a player's own worlds), then opens
     *  {@link AdminUserWorldsMenu} back on the main thread. */
    private void openWorldsMenu() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<PocketWorld> worlds = new ArrayList<>();
            PocketUser user = plugin.getUserCache().readThrough(targetId);
            for (UUID worldId : Set.copyOf(user.getWorlds())) {
                PocketWorld world = plugin.getWorldCache().readThrough(worldId);
                if (world != null) {
                    worlds.add(world);
                } else {
                    // Deleted since the user's own reference was recorded - drop the stale entry.
                    user.getWorlds().remove(worldId);
                }
            }

            Bukkit.getScheduler().runTask(plugin, () ->
                    new AdminUserWorldsMenu(player, plugin, worlds, targetId, targetName, this::open).open());
        });
    }
}
