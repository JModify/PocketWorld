package com.pocketworld.plugin.ui.world_menus.creation;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.theme.PocketTheme;
import com.pocketworld.plugin.ui.PocketItem;
import com.pocketworld.plugin.ui.PocketMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class WorldCreationThemeMenu extends PocketMenu {

    private final WorldCreationMainMenu mainMenu;

    public WorldCreationThemeMenu(Player player, PocketWorldPlugin plugin, WorldCreationMainMenu mainMenu) {
        super(player, plugin);
        this.mainMenu = mainMenu;
    }

    @Override
    public String getMenuName() {
        return "&4&lTheme Select";
    }

    @Override
    public int getMenuSlots() {
        return 27;
    }

    @Override
    public void setMenuItems() {
        Inventory inventory = getInventory();

        ItemStack fillerItem = new PocketItem.Builder(plugin)
                .material(Material.BLACK_STAINED_GLASS_PANE)
                .stackSize(1)
                .displayName(" ")
                .build().get();
        addFillerBorder(fillerItem);

        ItemStack emptyThemeItem = new PocketItem.Builder(plugin)
                .material(Material.BARRIER)
                .stackSize(1)
                .displayName("&4Unavailable")
                .lore(List.of("&7More themes coming soon."))
                .build().get();

        List<PocketTheme> themes = plugin.getThemeRegistry().getThemes();

        for (int i = 0; i < 7; i++) {
            if (i >= themes.size()) {
                int empty = inventory.firstEmpty();
                if (empty >= 0) {
                    inventory.setItem(empty, emptyThemeItem);
                }
                continue;
            }

            PocketTheme theme = themes.get(i);
            PocketItem item = new PocketItem.Builder(plugin)
                    .material(theme.getIcon())
                    .stackSize(1)
                    .displayName("&b" + theme.getName())
                    .lore(List.of("&7Description: " + theme.getDescription(), "&7Biome: " + theme.getBiome()))
                    .tag(theme.getId().toString())
                    .build();
            inventory.addItem(item.get());
        }
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

        UUID themeId = UUID.fromString(tag);
        PocketTheme theme = plugin.getThemeRegistry().getThemeByID(themeId);
        mainMenu.setPocketTheme(theme);
        mainMenu.open();
    }
}
