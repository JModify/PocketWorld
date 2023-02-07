package me.modify.pocketworld.data.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.data.Connection;
import me.modify.pocketworld.data.DAO;
import me.modify.pocketworld.exceptions.DataSourceConnectionException;
import org.bson.diagnostics.Loggers;
import org.bukkit.Bukkit;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the connection to MongoDB if MongoDB is used as a data source.
 */
public class MongoConnection implements Connection {

    @Getter
    private MongoClient mongoClient;

    @Getter
    private MongoDatabase mongoDatabase;

    @Getter
    private DAO dataAccess;

    private final PocketWorldPlugin plugin;
    public MongoConnection(PocketWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void connect() throws DataSourceConnectionException {
        String uri = plugin.getConfigFile().getYaml().getString("mongodb.uri", null);
        String database = plugin.getConfigFile().getYaml().getString("mongodb.database", null);

        if (uri == null || database == null) {
            Bukkit.getServer().getPluginManager().disablePlugin(plugin);
            throw new DataSourceConnectionException("Failed to connect to MongoDB. Corrupted configuration.");
        }

        // TODO: Hide spam, doesn't work for some reason, why you doing this to me Mongo :(
        System.setProperty("DEBUG.GO", "true");
        System.setProperty("DB.TRACE", "true");
        Logger logger = Logger.getLogger("org.mongodb.driver");
        logger.setFilter(ignored -> false);
        logger.setLevel(Level.OFF);

        //TODO: Catch error if connection was not able to be made
        this.mongoClient = MongoClients.create(uri);

        this.mongoDatabase = mongoClient.getDatabase(database);
        this.dataAccess = new MongoDAO(this);

        plugin.getLogger().info("Successfully connected to mongodb");
    }

    @Override
    public void close() {
        mongoClient.close();
    }

    @Override
    public DAO getDAO() {
        return dataAccess;
    }
}
