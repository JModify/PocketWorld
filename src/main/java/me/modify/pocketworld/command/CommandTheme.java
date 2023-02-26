package me.modify.pocketworld.command;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.theme.PocketTheme;
import me.modify.pocketworld.theme.creation.ThemeCreationRegistry;
import me.modify.pocketworld.util.ColorFormat;
import me.modify.pocketworld.util.InteractiveText;
import me.modify.pocketworld.util.PocketPermission;
import me.modify.pocketworld.util.PocketUtils;
import me.modify.pocketworld.util.MessageReader;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class CommandTheme extends BukkitCommand {

    private final PocketWorldPlugin plugin;
    public CommandTheme(PocketWorldPlugin plugin) {
        super("theme");
        this.plugin = plugin;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Must be a player to execute this command.");
            return true;
        }

        int length = args.length;

        if (length > 0) {
            MessageReader reader = plugin.getMessageReader();
            if (args[0].equalsIgnoreCase("create")) {

                // Check player has permission to create themes.
                if (!PocketPermission.has(player, PocketPermission.POCKET_WORLD_THEME_CREATE)) {
                    reader.send("insufficient-permissions", player);
                    return true;
                }

                ThemeCreationRegistry.getInstance().addCreator(plugin, player.getUniqueId());
            } else if (args[0].equalsIgnoreCase("manage") || args[0].equalsIgnoreCase("list")) {

                if (!PocketPermission.has(player, PocketPermission.POCKET_WORLD_THEME_MANAGE)) {
                    reader.send("insufficient-permissions", player);
                    return true;
                }

                printThemeManage(player);
                return true;
            } else if (args[0].equalsIgnoreCase("import")) {
                //TODO: Implement theme importing
                return true;
            } else if (args[0].equalsIgnoreCase("delete")) {

                if (!PocketPermission.has(player, PocketPermission.POCKET_WORLD_THEME_MANAGE)) {
                    reader.send("insufficient-permissions", player);
                    return true;
                }

                if (length != 2) {
                    reader.send("invalid-usage", player, "{USAGE}:/theme delete <id>");
                    return true;
                }

                String idRaw = args[1];
                if (!PocketUtils.isUUID(idRaw)) {
                    reader.send("theme-delete-not-uuid", player, "{ID}:" + idRaw);
                    return true;
                }

                UUID id = UUID.fromString(idRaw);
                PocketTheme theme = plugin.getThemeRegistry().getThemeByID(id);
                theme.delete(plugin);
                return true;
            } else if (args[0].equalsIgnoreCase("edit")) {
                //TODO: Implement theme editing
                return true;
            }

        }

        plugin.getMessageReader().send("invalid-usage", player, "{USAGE}:" + getUsage());
        return false;
    }

    @SuppressWarnings("deprecation")
    private void printThemeManage(Player player) {
        List<PocketTheme> themes = plugin.getThemeRegistry().getThemes();

        player.sendMessage(ColorFormat.format("&8&m------------------------------"));
        player.sendMessage(ColorFormat.format("&6&lPocketThemes"));
        player.sendMessage(" ");
        for (PocketTheme theme : themes) {
            InteractiveText interactiveText = new InteractiveText.Builder(theme.getName())
                    .color(ChatColor.YELLOW)
                    .hoverText(theme.getId().toString(), ChatColor.GRAY, false, true)
                    .build();

            InteractiveText deleteButton = new InteractiveText.Builder("[☓]")
                    .color(ChatColor.RED)
                    .clickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/theme delete " + theme.getId().toString()))
                    .hoverText("Click to delete theme.", ChatColor.GRAY, false, false)
                    .build();

            InteractiveText editButton = new InteractiveText.Builder("[✎]")
                    .color(ChatColor.LIGHT_PURPLE)
                    .clickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/theme edit " + theme.getId().toString()))
                    .hoverText("Click to edit theme.", ChatColor.GRAY, false, false)
                    .build();

            // Send player a message with the appended delete button and edit button onto original theme name text.
            player.spigot().sendMessage(interactiveText.append(deleteButton, editButton));
        }

        if (themes.isEmpty()) {
            player.sendMessage(ColorFormat.format("&eNo themes to display."));
        }

        player.sendMessage(" ");
        player.sendMessage(ColorFormat.format("&7Click on management icons with your mouse click."));
        player.sendMessage(ColorFormat.format("&8&m------------------------------"));
    }
}
