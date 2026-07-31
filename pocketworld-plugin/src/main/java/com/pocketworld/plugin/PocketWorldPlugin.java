package com.pocketworld.plugin;

import com.pocketworld.plugin.api.PocketWorldAPI;
import com.pocketworld.plugin.cache.UserCache;
import com.pocketworld.plugin.cache.WorldCache;
import com.pocketworld.plugin.command.CommandPocketWorld;
import com.pocketworld.plugin.command.CommandPocketWorldAdmin;
import com.pocketworld.plugin.command.CommandTheme;
import com.pocketworld.plugin.data.DataSource;
import com.pocketworld.plugin.data.config.ConfigFile;
import com.pocketworld.plugin.data.config.MessageFile;
import com.pocketworld.plugin.exceptions.DataSourceConnectionException;
import com.pocketworld.plugin.listener.ChatInputListener;
import com.pocketworld.plugin.listener.InventoryListener;
import com.pocketworld.plugin.listener.PlayerListener;
import com.pocketworld.plugin.listener.ThemeCreationListener;
import com.pocketworld.plugin.listener.WorldAutoUnloadTracker;
import com.pocketworld.plugin.listener.WorldListener;
import com.pocketworld.plugin.runtime.PocketWorldRuntime;
import com.pocketworld.plugin.runtime.bridge.BridgeSelector;
import com.pocketworld.plugin.runtime.bridge.WorldRuntimeBridge;
import com.pocketworld.plugin.theme.ThemeRegistry;
import com.pocketworld.plugin.util.ChatInputRegistry;
import com.pocketworld.plugin.util.MessageReader;
import com.pocketworld.plugin.util.PocketDebugger;
import com.pocketworld.slime.storage.WorldLoader;
import com.pocketworld.slime.storage.loader.file.FileWorldLoader;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.UncheckedIOException;

public final class PocketWorldPlugin extends JavaPlugin {

    private ConfigFile configFile;
    private MessageFile messageFile;
    private MessageReader messageReader;
    private PocketDebugger debugger;
    private ChatInputRegistry chatInputRegistry;

    private PocketWorldRuntime runtime;
    private PocketWorldRuntime themeRuntime;

    private DataSource dataSource;
    private WorldCache worldCache;
    private UserCache userCache;
    private ThemeRegistry themeRegistry;

    @Override
    public void onEnable() {
        configFile = new ConfigFile(this);
        messageFile = new MessageFile(this);
        messageReader = new MessageReader(messageFile);

        debugger = new PocketDebugger(this);
        debugger.setDebugMode(configFile.getYaml().getBoolean("debug", false));

        chatInputRegistry = new ChatInputRegistry();

        try {
            WorldLoader worldStorage = new FileWorldLoader(getDataFolder().toPath().resolve("worlds"));
            WorldLoader themeStorage = new FileWorldLoader(getDataFolder().toPath().resolve("themes"));

            WorldRuntimeBridge bridge = new BridgeSelector(getLogger()).current();
            runtime = new PocketWorldRuntime(worldStorage, bridge);
            themeRuntime = new PocketWorldRuntime(themeStorage, bridge);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to initialize PocketWorld's storage/runtime layer", e);
        }

        dataSource = new DataSource(this);
        try {
            dataSource.connect();
        } catch (DataSourceConnectionException e) {
            getLogger().severe("Failed to connect to PocketWorld's metadata data source: " + e.getMessage());
            return;
        }

        worldCache = new WorldCache(this);
        userCache = new UserCache(this);

        themeRegistry = new ThemeRegistry(this);
        themeRegistry.load();

        WorldAutoUnloadTracker autoUnloadTracker = new WorldAutoUnloadTracker(this);
        getServer().getPluginManager().registerEvents(new ThemeCreationListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this, autoUnloadTracker), this);
        getServer().getPluginManager().registerEvents(new WorldListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(), this);
        getServer().getPluginManager().registerEvents(new ChatInputListener(this), this);

        getCommand("pocketworld").setExecutor(new CommandPocketWorld(this));
        getCommand("pocketworldadmin").setExecutor(new CommandPocketWorldAdmin(this));
        getCommand("theme").setExecutor(new CommandTheme(this));

        getServer().getServicesManager().register(PocketWorldAPI.class, PocketWorldAPI.create(this), this, ServicePriority.Normal);

        getLogger().info("PocketWorld " + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (worldCache != null) {
            worldCache.flush();
        }
        if (userCache != null) {
            userCache.flush();
        }
        if (dataSource != null && dataSource.getConnection() != null) {
            dataSource.shutdown();
        }

        getLogger().info("PocketWorld disabled.");
    }

    public ConfigFile getConfigFile() {
        return configFile;
    }

    public MessageFile getMessageFile() {
        return messageFile;
    }

    public MessageReader getMessageReader() {
        return messageReader;
    }

    public PocketDebugger getDebugger() {
        return debugger;
    }

    public ChatInputRegistry getChatInputRegistry() {
        return chatInputRegistry;
    }

    public PocketWorldRuntime getRuntime() {
        return runtime;
    }

    public PocketWorldRuntime getThemeRuntime() {
        return themeRuntime;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public WorldCache getWorldCache() {
        return worldCache;
    }

    public UserCache getUserCache() {
        return userCache;
    }

    public ThemeRegistry getThemeRegistry() {
        return themeRegistry;
    }
}
