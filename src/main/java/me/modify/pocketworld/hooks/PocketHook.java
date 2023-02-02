package me.modify.pocketworld.hooks;

import me.modify.pocketworld.PocketWorldPlugin;
import org.bukkit.Bukkit;

/**
 * Represents an abstract hook to another plugin.
 */
public abstract class PocketHook {

    private final String name;
    private boolean hooked;
    private final boolean depends;

    private PocketWorldPlugin plugin;

    /**
     * Initializes a plugin hook.
     * @param name name of plugin to hook.
     */
    public PocketHook(PocketWorldPlugin plugin, String name, boolean depends) {
        this.hooked = false;
        this.name = name;
        this.depends = depends;
        this.plugin = plugin;
    }

    public boolean isHooked() {
        return hooked;
    }

    public void hook() {
        if (Bukkit.getServer().getPluginManager().getPlugin(name) != null) {
            hooked = true;
            plugin.getLogger().info(name + " detected. Plugin successfully hooked.");
        } else {
            if (depends) {
                plugin.getLogger().severe("Failed to hook into dependency " + name + ". Plugin shutting down.");
                Bukkit.getServer().getPluginManager().disablePlugin(plugin);
            }
        }
    }

}
