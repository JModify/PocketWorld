package me.modify.pocketworld;

import com.grinderwolf.swm.api.SlimePlugin;
import lombok.Getter;
import me.modify.pocketworld.cache.UserCache;
import me.modify.pocketworld.cache.WorldCache;
import me.modify.pocketworld.command.*;
import me.modify.pocketworld.data.DataSource;
import me.modify.pocketworld.data.config.ConfigFile;
import me.modify.pocketworld.data.config.MessageFile;
import me.modify.pocketworld.data.mongo.MongoConnection;
import me.modify.pocketworld.data.mysql.MySQLConnection;
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
import me.modify.pocketworld.util.MessageReader;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class PocketWorldPlugin extends JavaPlugin {

    @Getter private DataSource dataSource;

    /** File containing all configurations for the plugin */
    @Getter private ConfigFile configFile;

    /** File containing all messages sent to user from the plugin */
    @Getter private MessageFile messageFile;

    @Getter private MessageReader messageReader;

    @Getter private SlimeHook slimeHook;

    @Getter private PocketDebugger debugger;

    @Getter private ThemeRegistry themeRegistry;

    @Getter private WorldCache worldCache;
    @Getter private UserCache userCache;

    @Override
    public void onEnable() {
        debugger = new PocketDebugger(this);
        debugger.setDebugMode(true);

        slimeHook = new SlimeHook(this);
        slimeHook.hook();

        configFile = new ConfigFile(this);
        messageFile = new MessageFile(this);
        messageReader = new MessageReader(this, messageFile);

        userCache = new UserCache(this);
        worldCache = new WorldCache(this);

        connectToDataSource();
        registerCommands();
        registerListeners();
        registerSlimeLoaders();

        themeRegistry = new ThemeRegistry(this);
        themeRegistry.load();
    }

    @Override
    public void onDisable() {
        worldCache.flush();
        userCache.flush();
        dataSource.shutdown();
    }

    private void registerCommands() {
        CommandUtil.registerCommand(new CommandTest(this));
        CommandUtil.registerCommand(new CommandTheme(this));
        CommandUtil.registerCommand(new CommandPocketWorld(this));
        CommandUtil.registerCommand(new CommandPocketWorldAdmin(this));
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
        } else if (dataSource.getConnection() instanceof MySQLConnection mySQLConnection) {
            //TODO: Implement SQL data source
            //slime.registerLoader("sql", new SQLLoader(this, mySQLlConnection));
        }
    }

/*    private void disableSlime() {
        PluginManager pluginManager = getServer().getPluginManager();
        Plugin plugin = pluginManager.getPlugin("SlimeWorldManager");

        if (plugin == null) {
            plugin.getLogger().severe("Failed to disable slime. Plugin is null?");
            return;
        }

        pluginManager.disablePlugin(plugin);
    }*/

}
