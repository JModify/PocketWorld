package me.modify.pocketworld.command;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.ui.world_menus.PocketWorldMainMenu;
import me.modify.pocketworld.user.PocketUser;
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

public class CommandPocketWorld extends BukkitCommand {

    private final PocketWorldPlugin plugin;
    public CommandPocketWorld(PocketWorldPlugin plugin) {
        super("pocketworld");
        setAliases(List.of("pw"));
        this.plugin = plugin;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Must be a player to execute this command.");
            return true;
        }

        int length = args.length;
        if (length == 0) {
            // After retrieving all worlds the user is associated too, open the menu synchronously using this list.
            Consumer<List<PocketWorld>> worldsConsumer = worlds -> {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        PocketWorldMainMenu menu = new PocketWorldMainMenu(player, plugin, worlds);
                        menu.open();
                    }
                }.runTask(plugin);
            };

            // Asynchronously retrieve all worlds the user is associated too.
            // Accept consumer with that list of worlds when done.
            new BukkitRunnable() {
                @Override
                public void run() {
                    List<PocketWorld> worlds = new ArrayList<>();

                    PocketUser user = plugin.getUserCache().readThrough(player.getUniqueId());
                    Set<UUID> worldIds = user.getWorlds();
                    worldIds.forEach(id -> {
                        PocketWorld world = plugin.getWorldCache().readThrough(id);

                        if (world != null) {
                            worlds.add(world);
                        } else {
                            // World has been deleted and so user reference to it should be removed.
                            user.getWorlds().remove(id);
                        }
                    });

                    worldsConsumer.accept(worlds);
                }
            }.runTaskAsynchronously(plugin);
        } else {

            String argument = args[0];

        }

        return false;
    }
}
