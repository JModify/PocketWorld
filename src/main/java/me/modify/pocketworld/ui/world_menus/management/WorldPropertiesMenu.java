package me.modify.pocketworld.ui.world_menus.management;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.data.DAO;
import me.modify.pocketworld.ui.PocketMenu;
import me.modify.pocketworld.util.ColorFormat;
import me.modify.pocketworld.ui.PocketItem;
import me.modify.pocketworld.world.PocketWorld;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class WorldPropertiesMenu extends PocketMenu {

    private PocketWorld world;
    private ManageWorldMenu previousMenu;

    public WorldPropertiesMenu(Player player, PocketWorldPlugin plugin, PocketWorld world,
                               ManageWorldMenu previousMenu) {
        super(player, plugin);
        this.world = world;
        this.previousMenu = previousMenu;
    }

    @Override
    public String getMenuName() {
        return "&4&lWorld Properties";
    }

    @Override
    public int getMenuSlots() {
        return 45;
    }

    @Override
    public void setMenuItems() {
        Inventory inventory = getInventory();

        ItemStack globe = new PocketItem.Builder(plugin)
                .material(Material.PLAYER_HEAD)
                .displayName("&d" + world.getWorldName())
                .lore(List.of("&7Members (" + world.getUsers().size() + "): " + world.getMembersFormatted(", "),
                        "&7Size: " + world.getWorldSizeFormatted(), " ", "&8" + world.getId().toString()))
                .build().getAsSkull("BlockminersTV");

        ItemStack spawnPoint = new PocketItem.Builder(plugin)
                .material(Material.ENDER_EYE)
                .displayName("&5Spawn Point")
                .lore(List.of("&7Click to set world spawn point to your current position.",
                        "&7You must be standing in your pocket world to set this."))
                .tag("property-spawn-point")
                .build().get();

        ItemStack back = new PocketItem.Builder(plugin)
                .material(Material.ARROW)
                .stackSize(1)
                .displayName("&aGo Back")
                .lore(List.of("&7Click to return to previous menu."))
                .tag("is-back-button")
                .build().get();

        ItemStack pvp = getAllowPvp();
        ItemStack monsterSpawns = getAllowMonsters();
        ItemStack animalSpawns = getAllowAnimals();

        inventory.setItem(13, globe);
        inventory.setItem(20, monsterSpawns);
        inventory.setItem(22, animalSpawns);
        inventory.setItem(24, pvp);
        inventory.setItem(31, spawnPoint);
        inventory.setItem(36, back);

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

        DAO dao = plugin.getDataSource().getConnection().getDAO();
        if (tag.equalsIgnoreCase("property-animal-spawns")) {
            world.setAllowAnimals(!world.isAllowAnimals());
            open();
        } else if (tag.equalsIgnoreCase("property-monster-spawns")) {
            world.setAllowMonsters(!world.isAllowMonsters());
            open();
        } else if (tag.equalsIgnoreCase("property-pvp")) {
            world.setPvp(!world.isPvp());
            open();
        } else if (tag.equalsIgnoreCase("property-spawn-point")) {
            Location playerLoc = player.getLocation();

            if (playerLoc.getWorld() == null || !playerLoc.getWorld().getName().equals(world.getId().toString())) {
                player.sendMessage(ColorFormat.format("&4&lERROR &r&cFailed to set world spawn point. " +
                        "You are not currently inside this pocket world!"));
                return;
            }

            world.setWorldSpawn(playerLoc);
            player.closeInventory();
            player.sendMessage(ColorFormat.format("&2&lSUCCESS &r&aPocket world spawn set to your location."));
        } else if (tag.equalsIgnoreCase("is-back-button")) {
            previousMenu.open();
        }
    }

    private ItemStack getAllowAnimals() {
        boolean state = world.isAllowAnimals();

        List<String> lore = getLoreFromState(state);

        Material material = state ? Material.LIME_DYE : Material.GRAY_DYE;
        String displayName = state ? "&aAnimal Spawns" : "&cAnimal Spawns";

        return new PocketItem.Builder(plugin)
                .material(material)
                .displayName(displayName)
                .lore(lore)
                .tag("property-animal-spawns").build().get();
    }

    private ItemStack getAllowMonsters() {
        boolean state = world.isAllowMonsters();

        List<String> lore = getLoreFromState(state);

        Material material = state ? Material.LIME_DYE : Material.GRAY_DYE;
        String displayName = state ? "&aMonster Spawns" : "&cMonster Spawns";

        return new PocketItem.Builder(plugin)
                .material(material)
                .displayName(displayName)
                .lore(lore)
                .tag("property-monster-spawns").build().get();
    }

    private ItemStack getAllowPvp() {
        boolean state = world.isPvp();

        List<String> lore = getLoreFromState(state);

        Material material = state ? Material.LIME_DYE : Material.GRAY_DYE;
        String displayName = state ? "&aPvp" : "&cPvp";

        return new PocketItem.Builder(plugin)
                .material(material)
                .displayName(displayName)
                .lore(lore)
                .tag("property-pvp").build().get();
    }

    private List<String> getLoreFromState(boolean state) {
        List<String> lore = new ArrayList<>();

        if (state) {
            lore.add("&7Enabled");
            lore.add(" ");
            lore.add("&8Click to disable.");
        } else {
            lore.add("&7Disabled");
            lore.add(" ");
            lore.add("&8Click to enable.");
        }

        return lore;
    }
}
