package me.modify.pocketworld.hooks;

import com.grinderwolf.swm.api.SlimePlugin;
import me.modify.pocketworld.PocketWorldPlugin;
import org.bukkit.Bukkit;

/**
 * Hook to advanced slime world manager
 */
public class SlimeHook extends PocketHook{

    public SlimeHook(PocketWorldPlugin plugin) {
        super(plugin, "SlimeWorldManager", true);
    }

    public SlimePlugin getAPI() {
        return (SlimePlugin) Bukkit.getPluginManager().getPlugin("SlimeWorldManager");
    }

}
