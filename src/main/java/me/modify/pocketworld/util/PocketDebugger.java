package me.modify.pocketworld.util;

import lombok.Getter;
import lombok.Setter;
import me.modify.pocketworld.PocketWorldPlugin;

public class PocketDebugger {

    @Getter
    @Setter
    private boolean debugMode;

    private PocketWorldPlugin plugin;

    public PocketDebugger(PocketWorldPlugin plugin) {
        this.debugMode = false;
    }

    public void sendDebugInfo(String s) {
        if (isDebugMode()) {
            plugin.getLogger().info(s);
        }
    }

    public void sendDebugError(String s) {
        if (isDebugMode()) {
            plugin.getLogger().severe(s);
        }
    }

    public void sendDebugWarning(String s) {
        if (isDebugMode()) {
            plugin.getLogger().warning(s);
        }
    }

}
