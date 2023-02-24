package me.modify.pocketworld.command;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.ui.world_menus.PocketWorldMainMenu;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CommandPocketWorld extends BukkitCommand {

    private final PocketWorldPlugin plugin;
    public CommandPocketWorld(PocketWorldPlugin plugin) {
        super("pocketworld");
        setAliases(List.of("pw"));
        this.plugin = plugin;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Must be a player to execute this command.");
            return true;
        }

        PocketWorldMainMenu menu = new PocketWorldMainMenu((Player) sender, plugin);
        menu.open();
        return false;
    }
}
