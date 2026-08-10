package com.pocketworld.plugin.ui.admin;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.ui.PocketItem;
import com.pocketworld.plugin.ui.PocketMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * Placeholder for punishment actions against a player - deliberately empty until specific
 * punishments are decided on. Candidates worth considering here: a pocket-world-specific mute (so
 * a player can still play elsewhere on the server), and a world-ban that blocks someone from being
 * invited to or entering any pocket world without touching their server-wide ban status.
 */
public class AdminPunishMenu extends PocketMenu {

    private final String targetName;
    private final Runnable onBack;

    public AdminPunishMenu(Player player, PocketWorldPlugin plugin, UUID targetId, String targetName, Runnable onBack) {
        super(player, plugin);
        this.targetName = targetName;
        this.onBack = onBack;
    }

    @Override
    public String getMenuName() {
        return "&4&lPunish " + targetName;
    }

    @Override
    public int getMenuSlots() {
        return 27;
    }

    @Override
    public void setMenuItems() {
        Inventory inventory = getInventory();

        ItemStack comingSoon = new PocketItem.Builder(plugin)
                .material(Material.BARRIER)
                .displayName("&cNo Punishments Configured")
                .lore(List.of("&7No punishment actions are available yet."))
                .build().get();

        ItemStack back = new PocketItem.Builder(plugin)
                .material(Material.ARROW)
                .stackSize(1)
                .displayName("&aReturn")
                .lore(List.of("&7Click to return to previous menu."))
                .tag("is-back-button")
                .build().get();

        inventory.setItem(13, comingSoon);
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
        if (tag != null && tag.equalsIgnoreCase("is-back-button")) {
            onBack.run();
        }
    }
}
