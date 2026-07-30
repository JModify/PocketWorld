package com.pocketworld.plugin;

import org.bukkit.plugin.java.JavaPlugin;

public final class PocketWorldPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("PocketWorld " + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("PocketWorld disabled.");
    }
}
