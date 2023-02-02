package me.modify.pocketworld.theme.creation.menus;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.menu.PocketAnvilMenu;
import me.modify.pocketworld.theme.creation.ThemeCreationController;
import me.modify.pocketworld.theme.creation.ThemeCreationRegistry;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EnterThemeNameMenu extends PocketAnvilMenu {

    private final PocketWorldPlugin plugin;
    public EnterThemeNameMenu(Player player, PocketWorldPlugin plugin) {
        super(player);
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
                        return AnvilGUI.Response.text("Invalid theme name.");
                    }

                    ThemeCreationController controller = ThemeCreationRegistry.getInstance().getController(player.getUniqueId());
                    controller.setName(text);
                    controller.nextState();
                    return AnvilGUI.Response.close();
                })
                .itemLeft(getItemLeft())
                .title("Enter theme name")
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
