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

/** Teleport into a world as an admin - either yourself, or another (online) player, prompted for by
 *  username in chat via {@link AdminTeleportPlayerPrompt}. Loads the world first if needed, the same
 *  "load if needed, then land the target once ready" way {@code WorldTeleportMainMenu} does. */
public class AdminTeleportMenu extends PocketMenu {

    private final PocketWorld world;
    private final Runnable onBack;

    public AdminTeleportMenu(Player player, PocketWorldPlugin plugin, PocketWorld world, Runnable onBack) {
        super(player, plugin);
        this.world = world;
        this.onBack = onBack;
    }

    @Override
    public String getMenuName() {
        return "&4&lTeleport";
    }

    @Override
    public int getMenuSlots() {
        return 27;
    }

    @Override
    public void setMenuItems() {
        Inventory inventory = getInventory();

        ItemStack teleportSelf = new PocketItem.Builder(plugin)
                .material(Material.ENDER_PEARL)
                .displayName("&aTeleport Me Here")
                .lore(List.of("&7Teleport yourself into \"" + world.getWorldName() + "\"."))
                .tag("admin-teleport-self")
                .build().get();

        ItemStack teleportPlayer = new PocketItem.Builder(plugin)
                .material(Material.PLAYER_HEAD)
                .displayName("&aTeleport a Player Here")
                .lore(List.of("&7Teleport another online player into \"" + world.getWorldName() + "\"."))
                .tag("admin-teleport-player")
                .build().get();

        ItemStack back = new PocketItem.Builder(plugin)
                .material(Material.ARROW)
                .stackSize(1)
                .displayName("&aReturn")
                .lore(List.of("&7Click to return to previous menu."))
                .tag("is-back-button")
                .build().get();

        inventory.setItem(11, teleportSelf);
        inventory.setItem(15, teleportPlayer);
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

        if (tag.equalsIgnoreCase("admin-teleport-self")) {
            if (!world.isLoaded()) {
                world.load(plugin, player.getUniqueId(), true, true);
            } else {
                world.teleport(player);
            }
            player.closeInventory();
        } else if (tag.equalsIgnoreCase("admin-teleport-player")) {
            new AdminTeleportPlayerPrompt(player, plugin, world).open();
        } else if (tag.equalsIgnoreCase("is-back-button")) {
            onBack.run();
        }
    }
}
