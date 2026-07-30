package com.pocketworld.plugin.debug;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.runtime.PocketWorldRuntime;
import com.pocketworld.plugin.runtime.WorldProperties;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * TEMPORARY Stage 4 smoke-test command for exercising {@link PocketWorldRuntime} end-to-end
 * against a real server. Not part of the product - removed once Stage 5 replaces it with the
 * real command/UI layer.
 */
public final class Stage4SmokeTestCommand implements CommandExecutor {

    private final PocketWorldPlugin plugin;
    private final PocketWorldRuntime runtime;
    private final Map<String, World> loadedTestWorlds = new HashMap<>();

    public Stage4SmokeTestCommand(PocketWorldPlugin plugin, PocketWorldRuntime runtime) {
        this.plugin = plugin;
        this.runtime = runtime;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /pwtest <import|create|load|unload|export> ...");
            return true;
        }
        try {
            switch (args[0]) {
                case "import" -> {
                    Path folder = Path.of(args[1]);
                    int dataVersion = Bukkit.getUnsafe().getDataVersion();
                    runtime.importWorld(folder, args[2], dataVersion);
                    sender.sendMessage("Imported " + folder + " as \"" + args[2] + "\"");
                }
                case "create" -> {
                    World world = runtime.create(args[1], args[2], WorldProperties.defaults());
                    loadedTestWorlds.put(args[2], world);
                    sender.sendMessage("Created+loaded \"" + args[2] + "\" -> bukkit world "
                            + world.getName() + ", loaded chunks=" + world.getLoadedChunks().length);
                }
                case "load" -> {
                    World world = runtime.load(args[1], WorldProperties.defaults());
                    loadedTestWorlds.put(args[1], world);
                    sender.sendMessage("Loaded \"" + args[1] + "\" -> bukkit world " + world.getName());
                }
                case "unload" -> {
                    World world = loadedTestWorlds.remove(args[1]);
                    if (world == null) {
                        sender.sendMessage("Not tracked as loaded by this command: " + args[1]);
                        return true;
                    }
                    runtime.unload(world, args[1], true);
                    sender.sendMessage("Unloaded+saved \"" + args[1] + "\"");
                }
                case "export" -> {
                    runtime.exportWorld(args[1], Path.of(args[2]));
                    sender.sendMessage("Exported \"" + args[1] + "\" to " + args[2]);
                }
                case "worldfolder" -> {
                    World world = Bukkit.getWorld(args[1]);
                    sender.sendMessage(world == null ? "not loaded" : world.getWorldFolder().getAbsolutePath());
                }
                default -> sender.sendMessage("Unknown subcommand: " + args[0]);
            }
        } catch (Exception e) {
            sender.sendMessage("ERROR: " + e);
            plugin.getLogger().log(Level.SEVERE, "pwtest command failed", e);
        }
        return true;
    }
}
