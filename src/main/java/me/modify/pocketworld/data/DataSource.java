package me.modify.pocketworld.data;

import lombok.Getter;
import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.data.mongo.MongoConnection;
import me.modify.pocketworld.data.mysql.MySQLConnection;
import me.modify.pocketworld.exceptions.DataSourceConnectionException;

/**
 * Represents the data source used in this plugin.
 */
public class DataSource {
    private PocketWorldPlugin plugin;

    @Getter
    private Connection connection;
    public DataSource(PocketWorldPlugin plugin) {
        this.plugin = plugin;
    }

    public void connect() throws DataSourceConnectionException {
        boolean useMongoDB = plugin.getConfigFile().getYaml().getBoolean("mongodb.use", false);
        boolean useMySQL = plugin.getConfigFile().getYaml().getBoolean("mysql.use", false);

        if (useMongoDB && useMySQL) {
            throw new DataSourceConnectionException("Too many databases in use!");
        }

        if (!useMongoDB && !useMySQL) {
            throw new DataSourceConnectionException("No databases in use.");
        }

        connection = useMySQL ? new MySQLConnection(plugin) : new MongoConnection(plugin);
        connection.connect();
    }

    public void shutdown() {
        connection.close();
    }
}
