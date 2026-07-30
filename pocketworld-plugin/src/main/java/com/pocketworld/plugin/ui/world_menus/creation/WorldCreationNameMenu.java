package com.pocketworld.plugin.ui.world_menus.creation;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.ui.PocketAnvilMenu;
import com.pocketworld.plugin.util.ColorFormat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.regex.Pattern;

/** Prompts the player to type their new pocket world's name in chat. */
public class WorldCreationNameMenu extends PocketAnvilMenu {

    private static final Pattern NAME_PATTERN = Pattern.compile("[a-z0-9 ]+", Pattern.CASE_INSENSITIVE);

    private final WorldCreationMainMenu mainMenu;

    public WorldCreationNameMenu(Player player, PocketWorldPlugin plugin, WorldCreationMainMenu mainMenu) {
        super(player, plugin);
        this.mainMenu = mainMenu;
    }

    @Override
    public void open() {
        player.closeInventory();
        ColorFormat.formatList(java.util.List.of(
                "&6&lEnter World Name",
                "&eType a name for your pocket world in chat.")).forEach(player::sendMessage);
        plugin.getChatInputRegistry().await(player.getUniqueId(), this::handleInput);
    }

    private boolean handleInput(String text) {
        if (text.length() < 3) {
            player.sendMessage(ColorFormat.format("&cName is too short."));
            return false;
        }
        if (text.length() > 16) {
            player.sendMessage(ColorFormat.format("&cName is too long."));
            return false;
        }
        if (!NAME_PATTERN.matcher(text).matches()) {
            player.sendMessage(ColorFormat.format("&cInvalid world name."));
            return false;
        }

        mainMenu.setWorldName(text);
        Bukkit.getScheduler().runTask(plugin, mainMenu::open);
        return true;
    }
}
