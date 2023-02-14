package me.modify.pocketworld.command;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.user.PocketUserInventory;
import me.modify.pocketworld.util.ColorFormat;
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
            } /*else if (args[0].equalsIgnoreCase("get-spawn")) {
                UUID id = UUID.fromString(args[1]);

                LoadedWorldRegistry registry = LoadedWorldRegistry.getInstance();
                PocketWorld worldInRegistry = registry.getWorld(id);
                PocketWorld worldInData = plugin.getDataSource().getConnection().getDAO().getPocketWorld(id);

                player.sendMessage(id.toString());
                player.sendMessage(worldInRegistry != null ? "registry - "
                        + worldInRegistry.getWorldSpawn().toString() : "null");
                player.sendMessage("datasource - " + worldInData.getWorldSpawn().toString());

            } */ else if (args[0].equalsIgnoreCase("rename")) {

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
            }

        }

        return false;
    }
}
