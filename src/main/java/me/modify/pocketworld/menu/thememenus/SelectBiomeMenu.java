package me.modify.pocketworld.menu.thememenus;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.menu.PocketMenu;
import me.modify.pocketworld.theme.creation.ThemeCreationController;
import me.modify.pocketworld.theme.creation.ThemeCreationRegistry;
import me.modify.pocketworld.util.PocketItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class SelectBiomeMenu extends PocketMenu {

    private final PocketWorldPlugin plugin;
    public SelectBiomeMenu(Player player, PocketWorldPlugin plugin) {
        super(player);
        this.plugin = plugin;
    }


    // plains, desert, swamp, taiga, snowy_taiga, savanna, jungle, beach, ocean, the_end, nether_wastes

    @Override
    public String getMenuName() {
        return "&4&lBiome Select";
    }

    @Override
    public int getMenuSlots() {
        return 36;
    }

    @Override
    public void setMenuItems() {
        Inventory inventory = getInventory();
        inventory.setItem(10, getPlainsItem());
        inventory.setItem(11, getDesertItem());
        inventory.setItem(12, getSwampItem());
        inventory.setItem(13, getTaigaItem());
        inventory.setItem(14, getSnowyTaigaItem());
        inventory.setItem(15, getSavannaItem());
        inventory.setItem(16, getJungleItem());
        inventory.setItem(20, getBeachItem());
        inventory.setItem(21, getOceanItem());
        inventory.setItem(23, getNetherItem());
        inventory.setItem(24, getEndItem());

        PocketItem fillerItem = new PocketItem.Builder(plugin)
                .material(Material.BLACK_STAINED_GLASS_PANE)
                .stackSize(1)
                .displayName(" ")
                .build();

        addFillers(fillerItem.get());
    }

    @Override
    public void handleMenuClick(InventoryClickEvent e) {

        e.setCancelled(true);

        int slot = e.getSlot();
        ItemStack item = e.getCurrentItem();
        if (item == null) {
            return;
        }

        String tag = PocketItem.getTag(plugin, item);
        if (tag == null) {
            return;
        }

        String[] tagParts = tag.split("-");
        if (tagParts.length != 3) {
            return;
        }

        player.closeInventory();

        ThemeCreationController controller = ThemeCreationRegistry.getInstance().getController(player.getUniqueId());
        controller.setBiome(tagParts[1]);
        controller.nextState();
    }

    private ItemStack getPlainsItem() {
        PocketItem item = new PocketItem.Builder(plugin)
                .material(Material.GRASS_BLOCK)
                .stackSize(1)
                .displayName("&a&lPlains")
                .lore(List.of("&7Most common Minecraft biome, makes grass look great."))
                .tag("theme-plains-biome")
                .build();

        return item.get();
    }

    private ItemStack getDesertItem() {
        PocketItem item = new PocketItem.Builder(plugin)
                .material(Material.SAND)
                .stackSize(1)
                .displayName("&e&lDesert")
                .lore(List.of("&7Warm biome mainly intended for large amounts of sand"))
                .tag("theme-desert-biome")
                .build();

        return item.get();
    }

    private ItemStack getSwampItem() {
        PocketItem item = new PocketItem.Builder(plugin)
                .material(Material.SLIME_BLOCK)
                .stackSize(1)
                .displayName("&2&lSwamp")
                .lore(List.of("&7Can sometimes spawn slimes, makes grass and water look darker."))
                .tag("theme-swamp-biome")
                .build();

        return item.get();
    }

    private ItemStack getTaigaItem() {
        PocketItem item = new PocketItem.Builder(plugin)
                .material(Material.SPRUCE_LOG)
                .stackSize(1)
                .displayName("&3&lTaiga")
                .lore(List.of("&7Colder biome, no snow though."))
                .tag("theme-taiga-biome")
                .build();

        return item.get();
    }

    private ItemStack getSnowyTaigaItem() {
        PocketItem item = new PocketItem.Builder(plugin)
                .material(Material.SNOW_BLOCK)
                .stackSize(1)
                .displayName("&f&lSnowy Taiga")
                .lore(List.of("&7Cold biome, snow will fall."))
                .tag("theme-snowy_taiga-biome")
                .build();

        return item.get();
    }

    private ItemStack getSavannaItem() {
        PocketItem item = new PocketItem.Builder(plugin)
                .material(Material.ACACIA_LOG)
                .stackSize(1)
                .displayName("&e&lSavanna")
                .lore(List.of("&7Common warm biome. Great for acacia trees."))
                .tag("theme-savanna-biome")
                .build();

        return item.get();
    }

    private ItemStack getJungleItem() {
        PocketItem item = new PocketItem.Builder(plugin)
                .material(Material.JUNGLE_LOG)
                .stackSize(1)
                .displayName("&6&lJungle")
                .lore(List.of("&7Grass is greenest in jungle biomes. Good for high vegetation."))
                .tag("theme-jungle-biome")
                .build();

        return item.get();
    }

    private ItemStack getBeachItem() {
        PocketItem item = new PocketItem.Builder(plugin)
                .material(Material.LIGHT_BLUE_STAINED_GLASS)
                .stackSize(1)
                .displayName("&b&lBeach")
                .lore(List.of("&7Water is a lighter shade in beach biomes."))
                .tag("theme-beach-biome")
                .build();

        return item.get();
    }

    private ItemStack getOceanItem() {
        PocketItem item = new PocketItem.Builder(plugin)
                .material(Material.BLUE_TERRACOTTA)
                .stackSize(1)
                .displayName("&1&lOcean")
                .lore(List.of("&7Darker water shade. Under the sea... under the sea.. sorry"))
                .tag("theme-ocean-biome")
                .build();

        return item.get();
    }

    private ItemStack getEndItem() {
        PocketItem item = new PocketItem.Builder(plugin)
                .material(Material.END_STONE)
                .stackSize(1)
                .displayName("&5&lThe End")
                .lore(List.of("&7Biome used in the end."))
                .tag("theme-the_end-biome")
                .build();

        return item.get();
    }

    private ItemStack getNetherItem() {
        PocketItem item = new PocketItem.Builder(plugin)
                .material(Material.NETHERRACK)
                .stackSize(1)
                .displayName("&4&lThe Nether")
                .lore(List.of("&7Biome used in the nether."))
                .tag("theme-nether_wastes-biome")
                .build();

        return item.get();
    }
}
