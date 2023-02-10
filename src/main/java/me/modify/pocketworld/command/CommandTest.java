package me.modify.pocketworld.command;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.data.Connection;
import me.modify.pocketworld.user.UserInventory;
import me.modify.pocketworld.util.ColorFormat;
import me.modify.pocketworld.world.LoadedWorldRegistry;
import me.modify.pocketworld.world.PocketWorld;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;

public class CommandTest extends BukkitCommand {

    private PocketWorldPlugin plugin;
    public CommandTest(PocketWorldPlugin plugin) {
        super("test");
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
                //idk
            } else if (args[0].equalsIgnoreCase("save")) {
                UserInventory.saveUserInventory(plugin, player);
            } else if (args[0].equalsIgnoreCase("load")) {
                UserInventory.restoreUserInventory(plugin, player);
            } else if (args[0].equalsIgnoreCase("getspawn")) {
                UUID id = UUID.fromString(args[1]);

                LoadedWorldRegistry registry = LoadedWorldRegistry.getInstance();
                PocketWorld worldInRegistry = registry.getWorld(id);
                PocketWorld worldInData = plugin.getDataSource().getConnection().getDAO().getPocketWorld(id);

                player.sendMessage(id.toString());
                player.sendMessage(worldInRegistry != null ? "registry - "
                        + worldInRegistry.getWorldSpawn().toString() : "null");
                player.sendMessage("datasource - " + worldInData.getWorldSpawn().toString());

            }  else if (args[0].equalsIgnoreCase("rename")) {

                if (length < 2) {
                    return true;
                }

                String newName = ColorFormat.format(args[1]);

                ItemMeta meta = player.getInventory().getItemInMainHand().getItemMeta();
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
