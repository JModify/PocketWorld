package me.modify.pocketworld.world.menu.creation;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.menu.PocketMenu;
import me.modify.pocketworld.theme.PocketTheme;
import me.modify.pocketworld.util.PocketItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class WorldCreationThemeMenu extends PocketMenu {

    private final WorldCreationMainMenu mainMenu;
    private final PocketWorldPlugin plugin;
    public WorldCreationThemeMenu(Player player, PocketWorldPlugin plugin, WorldCreationMainMenu mainMenu) {
        super(player);
        this.mainMenu = mainMenu;
        this.plugin = plugin;
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

        //TODO: Make all menus call get() only once for each ItemStack
        ItemStack fillerItem = new PocketItem.Builder(plugin)
                .material(Material.BLACK_STAINED_GLASS_PANE)
                .stackSize(1)
                .displayName(" ")
                .build().get();

        addFillers(fillerItem, 0, 8);
        inventory.setItem(9, fillerItem);
        inventory.setItem(17, fillerItem);
        addFillers(fillerItem, 18, 26);

        ItemStack emptyThemeItem = new PocketItem.Builder(plugin)
                .material(Material.BARRIER)
                .stackSize(1)
                .displayName("&4Unavailable")
                .lore(List.of("&7More themes coming soon."))
                .build().get();

        List<PocketTheme> themes = plugin.getThemeCache().getThemes();

        for (int i = 0; i < 7; i++) {
            try {
                PocketTheme theme = themes.get(i);
                PocketItem item = new PocketItem.Builder(plugin)
                        .material(theme.getIcon())
                        .stackSize(1)
                        .displayName("&b" + theme.getName())
                        .lore(List.of("&7Biome: " + theme.getBiome()))
                        .tag(theme.getId().toString())
                        .build();

                inventory.addItem(item.get());
            } catch (IndexOutOfBoundsException e) {
                int empty = inventory.firstEmpty();
                inventory.setItem(empty, emptyThemeItem);
            }
        }

    }

    @Override
    public void handleMenuClick(InventoryClickEvent e) {
        e.setCancelled(true);

        // Will never be null, checked in event listener class.
        ItemStack item = e.getCurrentItem();
        if (item == null) {
            return;
        }

        String tag = PocketItem.getTag(plugin, item);
        if (tag == null) {
            return;
        }

        UUID themeId = UUID.fromString(tag);
        PocketTheme theme = plugin.getThemeCache().getThemeByID(themeId);
        mainMenu.setPocketTheme(theme);
        mainMenu.open();
    }
}
