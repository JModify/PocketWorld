package com.pocketworld.plugin.command;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.theme.PocketTheme;
import com.pocketworld.plugin.theme.creation.ThemeCreationRegistry;
import com.pocketworld.plugin.util.ColorFormat;
import com.pocketworld.plugin.util.InteractiveText;
import com.pocketworld.plugin.util.MessageReader;
import com.pocketworld.plugin.util.PocketPermission;
import com.pocketworld.plugin.util.PocketUtils;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

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
        if (args.length == 0) {
            reader.send("invalid-usage", player, "{USAGE}:/theme <create|manage|delete>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (!PocketPermission.has(player, PocketPermission.POCKET_WORLD_THEME_CREATE)) {
                    reader.send("insufficient-permissions", player);
                    return true;
                }
                ThemeCreationRegistry.getInstance().addCreator(plugin, player.getUniqueId());
            }
            case "manage", "list" -> {
                if (!PocketPermission.has(player, PocketPermission.POCKET_WORLD_THEME_MANAGE)) {
                    reader.send("insufficient-permissions", player);
                    return true;
                }
                printThemeManage(player);
            }
            case "delete" -> {
                if (!PocketPermission.has(player, PocketPermission.POCKET_WORLD_THEME_MANAGE)) {
                    reader.send("insufficient-permissions", player);
                    return true;
                }
                if (args.length != 2) {
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
                if (theme == null) {
                    reader.send("theme-not-found", player, "{ID}:" + idRaw);
                    return true;
                }
                theme.delete(plugin);
            }
            case "import", "edit" -> player.sendMessage(ColorFormat.format("&cThis feature is not yet implemented."));
            default -> reader.send("invalid-usage", player, "{USAGE}:/theme <create|manage|delete>");
        }

        return true;
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

            InteractiveText deleteButton = new InteractiveText.Builder("[X]")
                    .color(ChatColor.RED)
                    .clickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/theme delete " + theme.getId()))
                    .hoverText("Click to delete theme.", ChatColor.GRAY, false, false)
                    .build();

            InteractiveText editButton = new InteractiveText.Builder("[edit]")
                    .color(ChatColor.LIGHT_PURPLE)
                    .clickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/theme edit " + theme.getId()))
                    .hoverText("Click to edit theme.", ChatColor.GRAY, false, false)
                    .build();

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
