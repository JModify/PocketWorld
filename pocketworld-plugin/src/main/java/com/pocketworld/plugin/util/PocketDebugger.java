package com.pocketworld.plugin.util;

import com.pocketworld.plugin.PocketWorldPlugin;

/** Gated logger wrapper - only emits when {@code debug: true} in config.yml. */
public final class PocketDebugger {

    private final PocketWorldPlugin plugin;
    private boolean debugMode;

    public PocketDebugger(PocketWorldPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public void info(String message) {
        if (debugMode) {
            plugin.getLogger().info(message);
        }
    }

    public void severe(String message) {
        if (debugMode) {
            plugin.getLogger().severe(message);
        }
    }

    public void warning(String message) {
        if (debugMode) {
            plugin.getLogger().warning(message);
        }
    }
}
