package com.pocketworld.plugin.command;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.util.ColorFormat;
import com.pocketworld.plugin.util.PocketPermission;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CommandPocketWorldAdmin implements CommandExecutor {

    private final PocketWorldPlugin plugin;

    public CommandPocketWorldAdmin(PocketWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageReader().send("must-be-player", sender);
            return true;
        }

        if (!PocketPermission.has(player, PocketPermission.POCKET_WORLD_ADMIN)) {
            plugin.getMessageReader().send("insufficient-permissions", player);
            return true;
        }

        if (args.length == 0) {
            List<String> menu = List.of(
                    "&7&m---------------------------",
                    "&6&lPocketWorld Admin",
                    "&e/" + label + " reload &f- &7Reload configuration files.",
                    "&7&m---------------------------");
            menu.forEach(line -> player.sendMessage(ColorFormat.format(line)));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.getConfigFile().reload();
            plugin.getMessageFile().reload();
            plugin.getMessageReader().send("plugin-reloaded", player);
            return true;
        }

        plugin.getMessageReader().send("invalid-usage", player, "{USAGE}:/pocketworldadmin <reload>");
        return true;
    }
}
