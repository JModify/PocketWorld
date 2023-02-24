package me.modify.pocketworld.command;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.user.PocketUserInventory;
import me.modify.pocketworld.util.ColorFormat;
import me.modify.pocketworld.util.InteractiveText;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CommandTest extends BukkitCommand {

    private final PocketWorldPlugin plugin;
    public CommandTest(PocketWorldPlugin plugin) {
        super("test");
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
            if (args[0].equalsIgnoreCase("save")) {
                PocketUserInventory.saveUserInventory(plugin, player);
            } else if (args[0].equalsIgnoreCase("load")) {
                PocketUserInventory.restoreUserInventory(plugin, player);
            } else if (args[0].equalsIgnoreCase("rename")) {

                if (length < 2) {
                    return true;
                }

                String newName = ColorFormat.format(args[1]);

                ItemMeta meta = player.getInventory().getItemInMainHand().getItemMeta();

                if (meta == null) return true;

                meta.setDisplayName(newName);

                if (length > 2) {
                    String lore = ColorFormat.format(args[2]);
                    meta.setLore(List.of(lore));
                }

                player.getInventory().getItemInMainHand().setItemMeta(meta);
                return true;
            } else if (args[0].equalsIgnoreCase("send")) {

                StringBuilder builder = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    builder.append(args[i]).append(" ");
                }

                player.sendMessage(ColorFormat.format(builder.toString().trim()));
            } else if (args[0].equalsIgnoreCase("clickable")) {

                InteractiveText interactiveText = new InteractiveText.Builder("Click me please!")
                        .color(ChatColor.LIGHT_PURPLE)
                        .italic(true)
                        .clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/gamemode survival"))
                        .hoverText("I am hovering!", ChatColor.RED, true, false)
                        .build();
                player.spigot().sendMessage(interactiveText.getMessage());
            }
        }

        return false;
    }
}
