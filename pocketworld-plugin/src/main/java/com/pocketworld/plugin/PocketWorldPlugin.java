package com.pocketworld.plugin;

import com.pocketworld.plugin.debug.Stage4SmokeTestCommand;
import com.pocketworld.plugin.runtime.PocketWorldRuntime;
import com.pocketworld.plugin.runtime.bridge.BridgeSelector;
import com.pocketworld.slime.storage.WorldLoader;
import com.pocketworld.slime.storage.loader.file.FileWorldLoader;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.UncheckedIOException;

public final class PocketWorldPlugin extends JavaPlugin {

    private PocketWorldRuntime runtime;

    @Override
    public void onEnable() {
        getLogger().info("PocketWorld " + getPluginMeta().getVersion() + " enabled.");

        try {
            WorldLoader storage = new FileWorldLoader(getDataFolder().toPath().resolve("worlds"));
            BridgeSelector bridgeSelector = new BridgeSelector(getLogger());
            runtime = new PocketWorldRuntime(storage, bridgeSelector.current());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to initialize PocketWorld's storage/runtime layer", e);
        }

        // TEMPORARY: Stage 4 smoke-test command, exercises PocketWorldRuntime end-to-end.
        // Removed once Stage 5 replaces it with the real command/UI layer.
        getCommand("pwtest").setExecutor(new Stage4SmokeTestCommand(this, runtime));
    }

    @Override
    public void onDisable() {
        getLogger().info("PocketWorld disabled.");
    }
}
