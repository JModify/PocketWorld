package me.modify.pocketworld.command;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.theme.PocketTheme;
import me.modify.pocketworld.theme.creation.ThemeCreationRegistry;
import me.modify.pocketworld.util.ColorFormat;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandTheme extends BukkitCommand {

    private PocketWorldPlugin plugin;
    public CommandTheme(PocketWorldPlugin plugin) {
        super("theme");
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Must be a player to execute this command.");
            return true;
        }

        int length = args.length;
        Player player = (Player) sender;

        if (length > 0) {
            if (args[0].equalsIgnoreCase("create")) {
                ThemeCreationRegistry.getInstance().addCreator(plugin, player.getUniqueId());
                player.sendMessage(ColorFormat.format("&aWelcome to"));
            } else if (args[0].equalsIgnoreCase("list")) {
                printThemeList(player);
            } else if (args[0].equalsIgnoreCase("import")) {
                //TODO: Implement theme importing
            } else if (args[0].equalsIgnoreCase("delete")) {
                //TODO: Implement theme deletion
            }

        }

        return false;
    }

    private void printThemeList(Player player) {
        List<PocketTheme> themes = plugin.getThemeCache().getThemes();

        player.sendMessage(ColorFormat.format("&8&m------------------------------"));
        for (int i = 0; i < themes.size(); i++) {
            PocketTheme theme = themes.get(i);
            player.sendMessage(ColorFormat.format("&2&l" + theme.getId().toString()));
            player.sendMessage(ColorFormat.format("&aName &f- &7" + theme.getName()));
            player.sendMessage(ColorFormat.format("&aBiome &f- &7" + theme.getBiome()));

            if (i != themes.size() - 1) {
                player.sendMessage(" ");
            }
        }
        player.sendMessage(ColorFormat.format("&8&m------------------------------"));
    }
}
