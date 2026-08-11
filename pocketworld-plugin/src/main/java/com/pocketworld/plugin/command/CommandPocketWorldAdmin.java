package com.pocketworld.plugin.command;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.runtime.PocketWorldRuntime;
import com.pocketworld.plugin.ui.admin.AdminManageUserMenu;
import com.pocketworld.plugin.ui.admin.AdminPlayerBrowserMenu;
import com.pocketworld.plugin.util.ColorFormat;
import com.pocketworld.plugin.util.PocketPermission;
import com.pocketworld.slime.format.SlimeFormatException;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CommandPocketWorldAdmin implements CommandExecutor {

    private final PocketWorldPlugin plugin;

    public CommandPocketWorldAdmin(PocketWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageReader().send("must-be-player", sender);
            return true;
        }

        if (!PocketPermission.has(player, PocketPermission.COMMAND_ADMIN)) {
            plugin.getMessageReader().send("insufficient-permissions", player);
            return true;
        }

        if (args.length == 0) {
            sendHelp(player, label);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                if (checkPermission(player, PocketPermission.ADMIN_RELOAD)) {
                    handleReload(player);
                }
            }
            case "import" -> {
                if (checkPermission(player, PocketPermission.ADMIN_IMPORT)) {
                    handleImport(player, args);
                }
            }
            case "export" -> {
                if (checkPermission(player, PocketPermission.ADMIN_EXPORT)) {
                    handleExport(player, args);
                }
            }
            case "validate" -> {
                if (checkPermission(player, PocketPermission.ADMIN_VALIDATE)) {
                    handleValidate(player, args);
                }
            }
            case "manage" -> {
                if (checkPermission(player, PocketPermission.ADMIN_MANAGE)) {
                    handleManage(player, args);
                }
            }
            case "bypass" -> {
                if (checkPermission(player, PocketPermission.ADMIN_BYPASS)) {
                    handleBypass(player);
                }
            }
            default -> sendHelp(player, label);
        }
        return true;
    }

    private boolean checkPermission(Player player, PocketPermission permission) {
        if (PocketPermission.has(player, permission)) {
            return true;
        }
        plugin.getMessageReader().send("insufficient-permissions", player);
        return false;
    }

    private void sendHelp(Player player, String label) {
        List<String> menu = List.of(
                "&7&m---------------------------",
                "&6&lPocketWorld Admin",
                "&e/" + label + " reload &f- &7Reload configuration files.",
                "&e/" + label + " import <folder> <worldId> [dataVersion] &f- &7Import a real Anvil world folder as a stored pocket world.",
                "&e/" + label + " export <worldId> <folder> &f- &7Export a stored pocket world as a real Anvil world folder.",
                "&e/" + label + " validate <worldId|all> &f- &7Check stored pocket world(s) for corruption.",
                "&e/" + label + " manage [player] &f- &7Browse and manage a player's pocket worlds.",
                "&e/" + label + " bypass &f- &7Toggle bypassing visitor build/break/interact permissions.",
                "&7&m---------------------------");
        menu.forEach(line -> player.sendMessage(ColorFormat.format(line)));
    }

    private void handleReload(Player player) {
        plugin.getConfigFile().reload();
        plugin.getMessageFile().reload();
        plugin.getMessageReader().send("plugin-reloaded", player);
    }

    private void handleImport(Player player, String[] args) {
        if (args.length < 3 || args.length > 4) {
            plugin.getMessageReader().send("invalid-usage", player, "{USAGE}:/pocketworldadmin import <folder> <worldId> [dataVersion]");
            return;
        }

        Path folder = Bukkit.getWorldContainer().toPath().getParent().resolve(args[1]).normalize();
        String worldId = args[2];
        int dataVersion;
        if (args.length == 4) {
            try {
                dataVersion = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                player.sendMessage(ColorFormat.format("&cInvalid data version \"" + args[3] + "\" - must be an integer."));
                return;
            }
        } else {
            dataVersion = Bukkit.getUnsafe().getDataVersion();
            player.sendMessage(ColorFormat.format("&7No data version given - assuming this server's current version ("
                    + dataVersion + "). Pass one explicitly if the folder is from an older Minecraft version."));
        }

        if (!java.nio.file.Files.isDirectory(folder)) {
            player.sendMessage(ColorFormat.format("&cNo such folder: " + folder));
            return;
        }

        int finalDataVersion = dataVersion;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PocketWorldRuntime runtime = plugin.getRuntime();
                if (runtime.exists(worldId)) {
                    player.sendMessage(ColorFormat.format("&cA pocket world already exists with id \"" + worldId + "\" - refusing to overwrite it."));
                    return;
                }

                runtime.importWorld(folder, worldId, finalDataVersion);
                player.sendMessage(ColorFormat.format("&aImported \"" + folder + "\" as pocket world \"" + worldId + "\"."));
            } catch (IOException e) {
                player.sendMessage(ColorFormat.format("&cImport failed: " + e.getMessage()));
                plugin.getLogger().severe("Failed to import world folder " + folder + " as \"" + worldId + "\": " + e);
            }
        });
    }

    private void handleExport(Player player, String[] args) {
        if (args.length != 3) {
            plugin.getMessageReader().send("invalid-usage", player, "{USAGE}:/pocketworldadmin export <worldId> <folder>");
            return;
        }

        String worldId = args[1];
        Path folder = Bukkit.getWorldContainer().toPath().getParent().resolve(args[2]).normalize();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PocketWorldRuntime runtime = plugin.getRuntime();
                if (!runtime.exists(worldId)) {
                    player.sendMessage(ColorFormat.format("&cNo stored pocket world found with id \"" + worldId + "\"."));
                    return;
                }
                if (java.nio.file.Files.exists(folder)) {
                    player.sendMessage(ColorFormat.format("&cRefusing to export into an already-existing path: " + folder));
                    return;
                }

                runtime.exportWorld(worldId, folder);
                player.sendMessage(ColorFormat.format("&aExported pocket world \"" + worldId + "\" to \"" + folder + "\"."));
            } catch (IOException e) {
                player.sendMessage(ColorFormat.format("&cExport failed: " + e.getMessage()));
                plugin.getLogger().severe("Failed to export pocket world \"" + worldId + "\" to " + folder + ": " + e);
            }
        });
    }

    /** No name given -> a paginated skull grid of every online player. A name given -> resolves that
     *  player directly (online or offline, so an admin can manage someone who's currently logged off). */
    private void handleManage(Player player, String[] args) {
        if (args.length == 1) {
            new AdminPlayerBrowserMenu(player, plugin, new ArrayList<>(Bukkit.getOnlinePlayers())).open();
            return;
        }
        if (args.length != 2) {
            plugin.getMessageReader().send("invalid-usage", player, "{USAGE}:/pocketworldadmin manage [player]");
            return;
        }

        String name = args[1];
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            @SuppressWarnings("deprecation")
            OfflinePlayer target = Bukkit.getOfflinePlayer(name);

            if (!target.hasPlayedBefore() && !target.isOnline()) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        player.sendMessage(ColorFormat.format("&cNo player found with name \"" + name + "\".")));
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () ->
                    new AdminManageUserMenu(player, plugin, target.getUniqueId(), target.getName(), null).open());
        });
    }

    /** Toggles this admin bypassing visitor build/break/interact permission enforcement (see
     *  {@link com.pocketworld.plugin.listener.WorldListener}) - session-only, off again on restart. */
    private void handleBypass(Player player) {
        boolean nowBypassing = plugin.toggleBypass(player.getUniqueId());
        plugin.getMessageReader().send(nowBypassing ? "admin-bypass-enabled" : "admin-bypass-disabled", player);
    }

    private void handleValidate(Player player, String[] args) {
        if (args.length != 2) {
            plugin.getMessageReader().send("invalid-usage", player, "{USAGE}:/pocketworldadmin validate <worldId|all>");
            return;
        }

        String target = args[1];
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PocketWorldRuntime runtime = plugin.getRuntime();
            if (target.equalsIgnoreCase("all")) {
                validateAll(player, runtime);
            } else {
                validateOne(player, runtime, target, true);
            }
        });
    }

    private void validateAll(Player player, PocketWorldRuntime runtime) {
        List<String> ids;
        try {
            ids = runtime.list();
        } catch (IOException e) {
            player.sendMessage(ColorFormat.format("&cFailed to list stored pocket worlds: " + e.getMessage()));
            return;
        }

        player.sendMessage(ColorFormat.format("&7Validating " + ids.size() + " stored pocket world(s)..."));
        int corrupted = 0;
        for (String id : ids) {
            if (!validateOne(player, runtime, id, false)) {
                corrupted++;
            }
        }
        if (corrupted == 0) {
            player.sendMessage(ColorFormat.format("&aAll " + ids.size() + " stored pocket world(s) are valid."));
        } else {
            player.sendMessage(ColorFormat.format("&c" + corrupted + " of " + ids.size() + " stored pocket world(s) are corrupted - see above."));
        }
    }

    /** @return true if valid, false if corrupted. Reports "not found" or a valid result in both cases;
     *  only reports a "checking..." line when {@code announce} is true (skipped for bulk validation, which
     *  already announced the total count up front). */
    private boolean validateOne(Player player, PocketWorldRuntime runtime, String worldId, boolean announce) {
        try {
            if (!runtime.exists(worldId)) {
                player.sendMessage(ColorFormat.format("&cNo stored pocket world found with id \"" + worldId + "\"."));
                return true;
            }
            if (announce) {
                player.sendMessage(ColorFormat.format("&7Validating \"" + worldId + "\"..."));
            }

            runtime.validate(worldId);
            player.sendMessage(ColorFormat.format("&a\"" + worldId + "\" is valid."));
            return true;
        } catch (SlimeFormatException e) {
            player.sendMessage(ColorFormat.format("&c\"" + worldId + "\" is CORRUPTED: " + e.getMessage()));
            return false;
        } catch (IOException e) {
            player.sendMessage(ColorFormat.format("&cFailed to check \"" + worldId + "\": " + e.getMessage()));
            return false;
        }
    }
}
