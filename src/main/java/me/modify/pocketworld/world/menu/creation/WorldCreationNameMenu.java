package me.modify.pocketworld.world.menu.creation;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.menu.PocketAnvilMenu;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WorldCreationNameMenu extends PocketAnvilMenu {

    private final WorldCreationMainMenu mainMenu;
    private final PocketWorldPlugin plugin;
    public WorldCreationNameMenu(Player player, PocketWorldPlugin plugin, WorldCreationMainMenu mainMenu) {
        super(player);
        this.mainMenu = mainMenu;
        this.plugin = plugin;
    }

    @Override
    public void open() {
        new AnvilGUI.Builder()
                .onComplete((p, text) -> {
                    if (isNameTooShort(text)) {
                        return AnvilGUI.Response.text("Name is too short.");
                    }

                    if (isNameTooLong(text)) {
                        return AnvilGUI.Response.text("Name is too long.");
                    }

                    if (hasIllegalCharacters(text)) {
                        return AnvilGUI.Response.text("Invalid world name.");
                    }

                    mainMenu.setWorldName(text);
                    mainMenu.open();
                    return AnvilGUI.Response.close();
                })
                .itemLeft(getItemLeft())
                .title("Name your PocketWorld")
                .plugin(plugin)
                .open(player);
    }

    private ItemStack getItemLeft() {
        ItemStack item = new ItemStack(Material.PAPER, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("Enter name here...");
        item.setItemMeta(meta);
        return item;
    }

    private boolean isNameTooShort(String s) {
        return s.length() < 3;
    }

    private boolean isNameTooLong(String s) {
        return s.length() > 16;
    }

    private boolean hasIllegalCharacters(String s) {
        Pattern pattern = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(s);

        return matcher.find();
    }
}
