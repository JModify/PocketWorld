package me.modify.pocketworld.data;

import lombok.Getter;
import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.data.mongo.MongoConnection;
import me.modify.pocketworld.data.mysql.MySQLConnection;
import me.modify.pocketworld.exceptions.DataSourceConnectionException;
import org.bukkit.Bukkit;

/**
 * Represents the data source used in this plugin.
 */
public class DataSource {

    /** Instance of this plugin */
    private PocketWorldPlugin plugin;

    /**
     * Represents the connection to this data source.
     */
    @Getter private Connection connection;

    /**
     * Constructs a new data source.
     * @param plugin the plugin instance.
     */
    public DataSource(PocketWorldPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Attempt connection to this data source.
     * @throws DataSourceConnectionException if configurations are not correctly set up OR the connection failed.
     */
    public void connect() throws DataSourceConnectionException {
        boolean useMongoDB = plugin.getConfigFile().getYaml().getBoolean("mongodb.use", false);
        boolean useMySQL = plugin.getConfigFile().getYaml().getBoolean("mysql.use", false);

        if (useMongoDB && useMySQL) {
            Bukkit.getServer().getPluginManager().disablePlugin(plugin);
            throw new DataSourceConnectionException("Too many databases in use!");
        }

        if (!useMongoDB && !useMySQL) {
            Bukkit.getServer().getPluginManager().disablePlugin(plugin);
            throw new DataSourceConnectionException("No databases in use.");
        }

        connection = useMySQL ? new MySQLConnection(plugin) : new MongoConnection(plugin);
        connection.connect();
    }

    /**
     * Shuts down this data source.
     */
    public void shutdown() {
        connection.close();
    }

    public String getSlimeLoaderName() {
        return connection instanceof MongoConnection ? "mongo" : "sql";
    }
}
