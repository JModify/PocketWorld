package me.modify.pocketworld.util;

import lombok.Getter;
import lombok.Setter;
import me.modify.pocketworld.PocketWorldPlugin;

public class PocketDebugger {

    @Getter @Setter private boolean debugMode;

    private final PocketWorldPlugin plugin;
    public PocketDebugger(PocketWorldPlugin plugin) {
        this.plugin = plugin;
        this.debugMode = false;
    }

    public void info(String s) {
        if (isDebugMode()) {
            plugin.getLogger().info(s);
        }
    }

    public void severe(String s) {
        if (isDebugMode()) {
            plugin.getLogger().severe(s);
        }
    }

    public void warning(String s) {
        if (isDebugMode()) {
            plugin.getLogger().warning(s);
        }
    }

}
