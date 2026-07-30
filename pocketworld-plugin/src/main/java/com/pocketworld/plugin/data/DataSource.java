package com.pocketworld.plugin.data;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.data.mongo.MongoConnection;
import com.pocketworld.plugin.data.mysql.MysqlConnection;
import com.pocketworld.plugin.exceptions.DataSourceConnectionException;
import org.bukkit.Bukkit;

/**
 * The metadata data source used by this plugin (users/worlds/themes). Independent of the Slime
 * world-blob storage layer ({@link com.pocketworld.slime.storage.WorldLoader}) - a server can, for
 * instance, keep world files on disk while storing metadata in MySQL, or vice versa.
 */
public class DataSource {

    private final PocketWorldPlugin plugin;
    private Connection connection;

    public DataSource(PocketWorldPlugin plugin) {
        this.plugin = plugin;
    }

    public Connection getConnection() {
        return connection;
    }

    /** @throws DataSourceConnectionException if configuration is invalid or the connection failed. */
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

        connection = useMySQL ? new MysqlConnection(plugin) : new MongoConnection(plugin);
        connection.connect();
    }

    public void shutdown() {
        connection.close();
    }
}
