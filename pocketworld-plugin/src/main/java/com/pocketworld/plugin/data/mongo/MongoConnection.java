package com.pocketworld.plugin.data.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.data.Connection;
import com.pocketworld.plugin.data.DAO;
import com.pocketworld.plugin.exceptions.DataSourceConnectionException;
import org.bukkit.Bukkit;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MongoConnection implements Connection {

    private final PocketWorldPlugin plugin;
    private MongoClient mongoClient;
    private MongoDatabase mongoDatabase;
    private DAO dataAccess;

    public MongoConnection(PocketWorldPlugin plugin) {
        this.plugin = plugin;
    }

    public MongoDatabase getMongoDatabase() {
        return mongoDatabase;
    }

    @Override
    public void connect() throws DataSourceConnectionException {
        String uri = plugin.getConfigFile().getYaml().getString("mongodb.uri", null);
        String database = plugin.getConfigFile().getYaml().getString("mongodb.database", null);

        if (uri == null || database == null) {
            Bukkit.getServer().getPluginManager().disablePlugin(plugin);
            throw new DataSourceConnectionException("Failed to connect to MongoDB. Corrupted configuration.");
        }

        try {
            this.mongoClient = MongoClients.create(uri);
            this.mongoDatabase = mongoClient.getDatabase(database);
            this.dataAccess = new MongoDAO(this);
        } catch (IllegalArgumentException e) {
            throw new DataSourceConnectionException("Failed to make connection to database. Invalid credentials.");
        }

        Logger.getLogger("org.mongodb.driver").setLevel(Level.SEVERE);
        plugin.getLogger().info("Successfully connected to MongoDB.");
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
