package com.pocketworld.plugin.ui.theme_menus;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.theme.creation.ThemeCreationController;
import com.pocketworld.plugin.theme.creation.ThemeCreationRegistry;
import com.pocketworld.plugin.ui.PocketItem;
import com.pocketworld.plugin.ui.PocketMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class SelectBiomeMenu extends PocketMenu {

    public SelectBiomeMenu(Player player, PocketWorldPlugin plugin) {
        super(player, plugin);
    }

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
        inventory.setItem(10, biomeItem(Material.GRASS_BLOCK, "&a&lPlains",
                "Most common Minecraft biome, makes grass look great.", "plains"));
        inventory.setItem(11, biomeItem(Material.SAND, "&e&lDesert",
                "Warm biome mainly intended for large amounts of sand", "desert"));
        inventory.setItem(12, biomeItem(Material.SLIME_BLOCK, "&2&lSwamp",
                "Can sometimes spawn slimes, makes grass and water look darker.", "swamp"));
        inventory.setItem(13, biomeItem(Material.SPRUCE_LOG, "&3&lTaiga",
                "Colder biome, no snow though.", "taiga"));
        inventory.setItem(14, biomeItem(Material.SNOW_BLOCK, "&f&lSnowy Taiga",
                "Cold biome, snow will fall.", "snowy_taiga"));
        inventory.setItem(15, biomeItem(Material.ACACIA_LOG, "&e&lSavanna",
                "Common warm biome. Great for acacia trees.", "savanna"));
        inventory.setItem(16, biomeItem(Material.JUNGLE_LOG, "&6&lJungle",
                "Grass is greenest in jungle biomes. Good for high vegetation.", "jungle"));
        inventory.setItem(20, biomeItem(Material.LIGHT_BLUE_STAINED_GLASS, "&b&lBeach",
                "Water is a lighter shade in beach biomes.", "beach"));
        inventory.setItem(21, biomeItem(Material.BLUE_TERRACOTTA, "&1&lOcean",
                "Darker water shade.", "ocean"));
        inventory.setItem(23, biomeItem(Material.NETHERRACK, "&4&lThe Nether",
                "Biome used in the nether.", "nether_wastes"));
        inventory.setItem(24, biomeItem(Material.END_STONE, "&5&lThe End",
                "Biome used in the end.", "the_end"));

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

    private ItemStack biomeItem(Material material, String displayName, String description, String biomeTag) {
        return new PocketItem.Builder(plugin)
                .material(material)
                .stackSize(1)
                .displayName(displayName)
                .lore(List.of("&7" + description))
                .tag("theme-" + biomeTag + "-biome")
                .build()
                .get();
    }
}
