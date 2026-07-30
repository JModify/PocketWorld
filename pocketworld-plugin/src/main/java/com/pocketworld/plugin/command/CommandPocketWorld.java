package com.pocketworld.plugin.command;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.ui.world_menus.PocketWorldMainMenu;
import com.pocketworld.plugin.user.PocketUser;
import com.pocketworld.plugin.util.PocketPermission;
import com.pocketworld.plugin.world.PocketWorld;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CommandPocketWorld implements CommandExecutor {

    private final PocketWorldPlugin plugin;

    public CommandPocketWorld(PocketWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageReader().send("must-be-player", sender);
            return true;
        }

        if (!PocketPermission.has(player, PocketPermission.POCKET_WORLD_DEFAULT)) {
            plugin.getMessageReader().send("insufficient-permissions", player);
            return true;
        }

        if (args.length != 0) {
            plugin.getMessageReader().send("invalid-usage", player, "{USAGE}:/pocketworld");
            return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<PocketWorld> worlds = new ArrayList<>();

            PocketUser user = plugin.getUserCache().readThrough(player.getUniqueId());
            Set<UUID> worldIds = user.getWorlds();
            for (UUID id : worldIds) {
                PocketWorld world = plugin.getWorldCache().readThrough(id);
                if (world != null) {
                    worlds.add(world);
                } else {
                    // World has been deleted since - drop the stale reference.
                    user.getWorlds().remove(id);
                }
            }

            Bukkit.getScheduler().runTask(plugin, () -> new PocketWorldMainMenu(player, plugin, worlds).open());
        });

        return true;
    }
}
