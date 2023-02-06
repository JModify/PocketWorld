package me.modify.pocketworld.menu.worldmenus.management;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.data.DAO;
import me.modify.pocketworld.menu.PocketPaginatedMenu;
import me.modify.pocketworld.theme.PocketTheme;
import me.modify.pocketworld.util.PocketItem;
import me.modify.pocketworld.world.PocketWorld;
import me.modify.pocketworld.world.LoadedWorldRegistry;
import me.modify.pocketworld.menu.worldmenus.PocketWorldMainMenu;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class WorldManagementMainMenu extends PocketPaginatedMenu {

    private PocketWorldMainMenu mainMenu;
    private final PocketWorldPlugin plugin;
    private List<PocketWorld> worlds;

    public WorldManagementMainMenu(Player player, PocketWorldPlugin plugin, PocketWorldMainMenu mainMenu) {
        super(player);
        this.plugin = plugin;
        this.mainMenu = mainMenu;
    }

    @Override
    public String getMenuName() {
        return "&4&lManage Your PocketWorlds";
    }

    @Override
    public void setMenuItems() {

        addMenuBorder();

        DAO dao = plugin.getDataSource().getConnection().getDAO();

        // Retrieves all pocket worlds a player is associated too.
        worlds = dao.getPocketWorlds(player.getUniqueId());

        if (!worlds.isEmpty()) {
            for (int i = 0; i < maxItemsPerPage; i++) {
                index = maxItemsPerPage * page + i;
                if (index >= worlds.size()) break;
                PocketWorld world = worlds.get(i);

                if (world == null) continue;

                PocketTheme theme = plugin.getThemeCache().getThemeByID(world.getThemeId());
                String members = world.getUsers().keySet().stream()
                        .map(uuid -> Bukkit.getPlayer(uuid).getName())
                        .collect(Collectors.joining(", "));

                String status = LoadedWorldRegistry.getInstance().containsWorld(world.getId())
                        ? "&aLOADED" : "&cNOT LOADED";

                PocketItem worldIcon = new PocketItem.Builder(plugin)
                        .material(theme.getIcon())
                        .stackSize(1)
                        .displayName("&b" + world.getWorldName())
                        .lore(List.of("&7Click to manage this world.", " ",
                                "&6Properties", "&eTheme: " + theme.getName(), "&eMembers: " + members,
                                " ", status, "&8" + world.getId().toString()))
                        .tag(world.getId().toString())
                        .build();

                getInventory().addItem(worldIcon.get());
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

        if (tag.equalsIgnoreCase("is-home-button")) {
            mainMenu.open();
        } else if (tag.equalsIgnoreCase("is-page-next")) {
            if (!((index + 1) >= worlds.size())) {
                page = page + 1;
                super.open();
            }
        } else if (tag.equalsIgnoreCase("is-page-back")) {
            if (page > 0) {
                page = page - 1;
                super.open();
            }
        } else {
            UUID worldId = UUID.fromString(ChatColor.stripColor(tag));
            Optional<PocketWorld> optionalWorld = worlds.stream().filter(t -> t.getId().equals(worldId)).findFirst();

            if (optionalWorld.isEmpty()) {
                // Will never be empty.
                return;
            }

            PocketWorld world = optionalWorld.get();
            WorldManagementMenu managementMenu = new WorldManagementMenu(player, plugin, world, this);
            managementMenu.open();
        }
    }

    @Override
    public void addMenuBorder() {
        Inventory inventory = getInventory();

        PocketItem homePage = new PocketItem.Builder(plugin)
                .material(Material.PLAYER_HEAD)
                .stackSize(1)
                .displayName("&aMain Menu")
                .lore(List.of("&7Click to return to main menu."))
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
                .material(Material.BLUE_STAINED_GLASS_PANE)
                .displayName(" ")
                .build().get();
        addFillers(fillerItem, 0, 9);
        addFillers(fillerItem, 44, 53);

        int[] slotsToReplace = {17, 18, 26, 27, 35, 36};
        for (int i : slotsToReplace) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, fillerItem);
            }
        }
    }
}
