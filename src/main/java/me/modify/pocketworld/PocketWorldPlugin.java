package me.modify.pocketworld;

import com.grinderwolf.swm.api.SlimePlugin;
import lombok.Getter;
import me.modify.pocketworld.command.CommandPocketWorld;
import me.modify.pocketworld.command.CommandTest;
import me.modify.pocketworld.command.CommandTheme;
import me.modify.pocketworld.command.CommandUtil;
import me.modify.pocketworld.data.DataSource;
import me.modify.pocketworld.data.config.ConfigFile;
import me.modify.pocketworld.data.mongo.MongoConnection;
import me.modify.pocketworld.exceptions.DataSourceConnectionException;
import me.modify.pocketworld.hooks.SlimeHook;
import me.modify.pocketworld.listener.InventoryListener;
import me.modify.pocketworld.listener.PlayerListener;
import me.modify.pocketworld.listener.ThemeCreationListener;
import me.modify.pocketworld.listener.WorldListener;
import me.modify.pocketworld.loaders.MongoLoader;
import me.modify.pocketworld.loaders.ThemeLoader;
import me.modify.pocketworld.theme.ThemeRegistry;
import me.modify.pocketworld.util.PocketDebugger;
import me.modify.pocketworld.world.PocketWorldCache;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class PocketWorldPlugin extends JavaPlugin {

    @Getter
    public DataSource dataSource;

    @Getter
    public ConfigFile configFile;

    @Getter
    public SlimeHook slimeHook;

    @Getter
    public PocketDebugger debugger;

    @Getter
    public ThemeRegistry themeRegistry;

    @Getter
    private PocketWorldCache worldCache;

    @Override
    public void onEnable() {
        debugger = new PocketDebugger(this);
        slimeHook = new SlimeHook(this);
        slimeHook.hook();

        configFile = new ConfigFile(this);
        worldCache = new PocketWorldCache(this);

        connectToDataSource();
        registerCommands();
        registerListeners();
        registerSlimeLoaders();

        themeRegistry = new ThemeRegistry(this);
        themeRegistry.load();
    }

    @Override
    public void onDisable() {
        worldCache.shutdown();
        dataSource.shutdown();
    }

    private void registerCommands() {
        CommandUtil.registerCommand(new CommandTest(this));
        CommandUtil.registerCommand(new CommandTheme(this));
        CommandUtil.registerCommand(new CommandPocketWorld(this));
    }

    private void registerListeners() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new PlayerListener(this), this);
        pluginManager.registerEvents(new InventoryListener(this), this);
        pluginManager.registerEvents(new WorldListener(this), this);
        pluginManager.registerEvents(new ThemeCreationListener(this), this);

    }

    private void connectToDataSource() {
        dataSource = new DataSource(this);
        try {
            dataSource.connect();
        } catch (DataSourceConnectionException e) {
            e.printStackTrace();
        }
    }

    private void registerSlimeLoaders() {
        SlimePlugin slime = slimeHook.getAPI();
        slime.registerLoader("theme", new ThemeLoader(this));

        if (dataSource.getConnection() instanceof MongoConnection mongoConnection) {
            slime.registerLoader("mongo", new MongoLoader(this, mongoConnection));
        }
    }

}
