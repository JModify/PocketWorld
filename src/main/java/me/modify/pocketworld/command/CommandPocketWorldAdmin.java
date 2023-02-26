package me.modify.pocketworld.command;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.ui.world_menus.PocketWorldMainMenu;
import me.modify.pocketworld.user.PocketUser;
import me.modify.pocketworld.util.ColorFormat;
import me.modify.pocketworld.util.PocketPermission;
import me.modify.pocketworld.world.PocketWorld;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Consumer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CommandPocketWorldAdmin extends BukkitCommand {

    private final PocketWorldPlugin plugin;
    public CommandPocketWorldAdmin(PocketWorldPlugin plugin) {
        super("pocketworldadmin");
        setAliases(List.of("pwa"));
        setUsage("/pocketworldadmin <reload>");
        this.plugin = plugin;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            plugin.getMessageReader().send("must-be-player", sender);
            return true;
        }

        if (!PocketPermission.has(player, PocketPermission.POCKET_WORLD_ADMIN)) {
            plugin.getMessageReader().send("insufficient-permissions", player);
            return true;
        }


        int length = args.length;
        if (length == 0) {
            List<String> menu = new ArrayList<>();
            menu.add("&7&m---------------------------");
            menu.add("&6&lPocketWorld Admin");
            menu.add("&e/" + label + " reload &f- &7Reload configuration files.");
            menu.add("&7&m---------------------------");

            menu.forEach(line -> player.sendMessage(ColorFormat.format(line)));
            return true;
        } else if (length == 1) {
            if (args[0].equalsIgnoreCase("reload")) {
                plugin.getConfigFile().reload();
                plugin.getMessageFile().reload();
                plugin.getMessageReader().send("plugin-reloaded", player);
                return true;
            }
        }

        plugin.getMessageReader().send("invalid-usage", player, "{USAGE}:" + getUsage());
        return false;
    }
}
