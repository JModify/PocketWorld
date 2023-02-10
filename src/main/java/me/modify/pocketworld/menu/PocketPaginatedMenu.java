package me.modify.pocketworld.menu;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.util.PocketItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public abstract class PocketPaginatedMenu extends PocketMenu {

    protected int page;
    protected final int maxItemsPerPage = 28;
    protected int index = 0;

    public PocketPaginatedMenu(Player player, PocketWorldPlugin plugin) {
        super(player, plugin);
        this.plugin = plugin;
    }

    @Override
    public int getMenuSlots() {
        return 54;
    }

    /**
     * Add border for this paginated inventory.
     * This border surrounds the square of items in the menu.
     */
    public void addMenuBorder(Material borderMaterial) {
        Inventory inventory = getInventory();

        PocketItem homePage = new PocketItem.Builder(plugin)
                .material(Material.PLAYER_HEAD)
                .stackSize(1)
                .displayName("&aMain Menu")
                .lore(List.of("&7Click to return to previous menu."))
                .tag("is-home-button")
                .build();
        PocketItem pageBack = new PocketItem.Builder(plugin)
                .material(Material.PLAYER_HEAD)
                .stackSize(1)
                .displayName("&aPrevious Page")
                .lore(List.of("&7Click to go to previous page."))
                .tag("is-page-back")
                .build();
        PocketItem pageNext = new PocketItem.Builder(plugin)
                .material(Material.PLAYER_HEAD)
                .stackSize(1)
                .displayName("&aNext Page")
                .lore(List.of("&7Click to go to next page."))
                .tag("is-page-next")
                .build();
        inventory.setItem(48, pageBack.getAsSkull("MHF_ArrowLeft"));
        inventory.setItem(49, homePage.getAsSkull("MHF_Chest"));
        inventory.setItem(50, pageNext.getAsSkull("MHF_ArrowRight"));

        ItemStack fillerItem = new PocketItem.Builder(plugin)
                .material(borderMaterial)
                .displayName(" ")
                .build().get();
        addFillerBorder(fillerItem);
    }
}
