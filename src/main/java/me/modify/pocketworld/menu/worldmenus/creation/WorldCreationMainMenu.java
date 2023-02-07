package me.modify.pocketworld.menu.worldmenus.creation;

import lombok.Setter;
import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.data.DAO;
import me.modify.pocketworld.menu.PocketMenu;
import me.modify.pocketworld.theme.PocketTheme;
import me.modify.pocketworld.user.PocketUser;
import me.modify.pocketworld.util.PocketItem;
import me.modify.pocketworld.world.PocketWorld;
import me.modify.pocketworld.world.WorldRank;
import me.modify.pocketworld.menu.worldmenus.PocketWorldMainMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class WorldCreationMainMenu extends PocketMenu {

    @Setter
    private String worldName;

    @Setter
    private PocketTheme pocketTheme;

    private PocketWorldMainMenu mainMenu;
    public WorldCreationMainMenu(Player player, PocketWorldPlugin plugin, PocketWorldMainMenu mainMenu) {
        super(player, plugin);
        this.pocketTheme = null;
        this.worldName = null;
        this.mainMenu = mainMenu;
    }

    @Override
    public String getMenuName() {
        return "&4&lCreate a PocketWorld";
    }

    @Override
    public int getMenuSlots() {
        return 27;
    }

    @Override
    public void setMenuItems() {
        Inventory inventory = getInventory();

        ItemStack enterName = getNameEntryIcon();
        ItemStack selectTheme = getThemeSelectIcon();
        ItemStack confirmCreate = getConfirmIcon();

        ItemStack backButton = new PocketItem.Builder(plugin)
                .material(Material.ARROW)
                .stackSize(1)
                .displayName("&aGo Back")
                .lore(List.of("&7Return to main menu."))
                .tag("is-back-button")
                .build().get();

        inventory.setItem(11, enterName);
        inventory.setItem(13, selectTheme);
        inventory.setItem(15, confirmCreate);
        inventory.setItem(18, backButton);

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

        // Will never be null, checked in event listener class.
        ItemStack item = e.getCurrentItem();
        if (item == null) {
            return;
        }

        String tag = PocketItem.getTag(plugin, item);
        if (tag == null) {
            return;
        }

        if (tag.equalsIgnoreCase("world-name-icon")) {
            WorldCreationNameMenu nameEntry = new WorldCreationNameMenu(player, plugin, this);
            nameEntry.open();
        } else if (tag.equalsIgnoreCase("world-theme-icon")) {
            WorldCreationThemeMenu themeSelect = new WorldCreationThemeMenu(player, plugin, this);
            themeSelect.open();
        }  else if (tag.equalsIgnoreCase("is-back-button")) {
            mainMenu.open();
        }else if (tag.equalsIgnoreCase("world-confirm-create") && (worldName != null && pocketTheme != null)) {
            DAO dao = plugin.getDataSource().getConnection().getDAO();

            PocketWorld world = PocketWorld.create(worldName, pocketTheme.getId());
            world.getUsers().put(player.getUniqueId(), WorldRank.OWNER);
            world.load(plugin, player.getUniqueId(), true, true);

            PocketUser user = dao.getPocketUser(player.getUniqueId());
            user.addWorld(world.getId());
            user.update(plugin);

            player.closeInventory();
        }
    }

    public ItemStack getNameEntryIcon() {
        List<String> lore = new ArrayList<>(List.of("&7Enter a name for your pocket world.",
                " ",
                "&6Restrictions: ",
                "&e* Must be greater than 3 characters.",
                "&e* Must be less than 16 characters.",
                " "));
        Material material;
        if (worldName != null) {
            lore.add("&8Selected: " + worldName);
            material = Material.BOOK;
        } else {
            lore.add("&8Selected: None");
            material = Material.WRITABLE_BOOK;
        }

        return new PocketItem.Builder(plugin)
                .material(material)
                .stackSize(1)
                .displayName("&bName Entry")
                .lore(lore)
                .tag("world-name-icon")
                .build().get();
    }

    public ItemStack getThemeSelectIcon() {
        List<String> lore = new ArrayList<>();
        lore.add("&7Select a theme for your pocket world.");
        lore.add(" ");
        lore.add("&6Note:");
        lore.add("&eTheme cannot be modified later once chosen.");
        lore.add(" ");

        Material material;
        if (pocketTheme != null) {
            lore.add("&8Selected: " + pocketTheme.getName());
            material = pocketTheme.getIcon();
        } else {
            lore.add("&8Selected: None");
            material = Material.BEDROCK;
        }

        return new PocketItem.Builder(plugin)
                .material(material)
                .stackSize(1)
                .displayName("&bTheme Select")
                .lore(lore)
                .tag("world-theme-icon")
                .build().get();
    }

    public ItemStack getConfirmIcon() {
        List<String> lore = new ArrayList<>();
        String displayName;
        Material material;

        if (worldName != null && pocketTheme != null) {
            material = Material.LIME_WOOL;
            displayName = "&a&lCreate Pocket World";
            lore.add("&aCreate your pocket world using the selected options.");
            lore.add(" ");
            lore.add("&7Name: " + worldName);
            lore.add("&7Theme: " + pocketTheme.getName());
        } else {
            material = Material.RED_WOOL;
            displayName = "&c&lCreate PocketWorld";
            lore.add("&cWorld name and theme selection must be set.");
        }

        return new PocketItem.Builder(plugin)
                .material(material)
                .stackSize(1)
                .displayName(displayName)
                .lore(lore)
                .tag("world-confirm-create")
                .build().get();
    }
}
