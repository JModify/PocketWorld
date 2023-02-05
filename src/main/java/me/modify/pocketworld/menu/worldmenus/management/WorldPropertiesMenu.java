package me.modify.pocketworld.menu.worldmenus.management;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.menu.PocketMenu;
import me.modify.pocketworld.util.PocketItem;
import me.modify.pocketworld.world.PocketWorld;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WorldPropertiesMenu extends PocketMenu {

    private PocketWorldPlugin plugin;
    private PocketWorld world;
    private WorldManagementMenu previousMenu;

    public WorldPropertiesMenu(Player player, PocketWorldPlugin plugin, PocketWorld world,
                               WorldManagementMenu previousMenu) {
        super(player);
        this.plugin = plugin;
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

        String members = world.getUsers().keySet().stream()
                .map(uuid -> Bukkit.getPlayer(uuid).getName())
                .collect(Collectors.joining(", "));

        ItemStack globe = new PocketItem.Builder(plugin)
                .material(Material.PLAYER_HEAD)
                .displayName("&d" + world.getWorldName())
                .lore(List.of("&7Members (" + world.getUsers().size() + "): " + members, " ", "&8" + world.getId().toString()))
                .build().getAsSkull("BlockminersTV");

        ItemStack spawnPoint = new PocketItem.Builder(plugin)
                .material(Material.ENDER_EYE)
                .displayName("&5Spawn Point")
                .lore(List.of("&7Click to set world spawnpoint to your current position"))
                .tag("property-spawn-point")
                .build().get();

        ItemStack pvp = getAllowPvp();
        ItemStack monsterSpawns = getAllowMonsters();
        ItemStack animalSpawns = getAllowAnimals();

        inventory.setItem(13, globe);
        inventory.setItem(20, monsterSpawns);
        inventory.setItem(22, animalSpawns);
        inventory.setItem(24, pvp);
        inventory.setItem(31, spawnPoint);

        ItemStack fillerItem = new PocketItem.Builder(plugin)
                .material(Material.BLACK_STAINED_GLASS_PANE)
                .stackSize(1)
                .displayName(" ")
                .build().get();

        // Top and bottom filler rows
        addFillers(fillerItem, 0, 8);
        addFillers(fillerItem, 36, 44);

        int[] slotsToReplace = {9, 18, 27, 17, 26, 35};
        for (int i : slotsToReplace) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, fillerItem);
            }
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
                .tag("property-animals-spawns").build().get();
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
