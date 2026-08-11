package com.pocketworld.plugin.command;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.theme.creation.ThemeCreationRegistry;
import com.pocketworld.plugin.ui.theme_menus.ThemeManagementMenu;
import com.pocketworld.plugin.util.ColorFormat;
import com.pocketworld.plugin.util.MessageReader;
import com.pocketworld.plugin.util.PocketPermission;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CommandTheme implements CommandExecutor {

    private final PocketWorldPlugin plugin;

    public CommandTheme(PocketWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Must be a player to execute this command.");
            return true;
        }

        MessageReader reader = plugin.getMessageReader();
        if (ThemeCreationRegistry.getInstance().blocksCommand(plugin, player)) {
            return true;
        }

        if (!PocketPermission.has(player, PocketPermission.COMMAND_THEME)) {
            reader.send("insufficient-permissions", player);
            return true;
        }

        if (args.length == 0) {
            reader.send("invalid-usage", player, "{USAGE}:/theme <create|manage>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (!PocketPermission.has(player, PocketPermission.THEME_CREATE)) {
                    reader.send("insufficient-permissions", player);
                    return true;
                }
                ThemeCreationRegistry.getInstance().addCreator(plugin, player.getUniqueId());
            }
            case "manage", "list" -> {
                if (!PocketPermission.has(player, PocketPermission.THEME_MANAGE)) {
                    reader.send("insufficient-permissions", player);
                    return true;
                }
                new ThemeManagementMenu(player, plugin, plugin.getThemeRegistry().getThemes()).open();
            }
            case "import" -> {
                if (!PocketPermission.has(player, PocketPermission.THEME_IMPORT)) {
                    reader.send("insufficient-permissions", player);
                    return true;
                }
                player.sendMessage(ColorFormat.format("&cThis feature is not yet implemented."));
            }
            case "edit" -> {
                if (!PocketPermission.has(player, PocketPermission.THEME_EDIT)) {
                    reader.send("insufficient-permissions", player);
                    return true;
                }
                player.sendMessage(ColorFormat.format("&cThis feature is not yet implemented."));
            }
            default -> reader.send("invalid-usage", player, "{USAGE}:/theme <create|manage>");
        }

        return true;
    }
}
