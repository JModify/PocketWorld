package com.pocketworld.plugin.data.file;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.data.Connection;
import com.pocketworld.plugin.data.DAO;
import com.pocketworld.plugin.exceptions.DataSourceConnectionException;

import java.io.IOException;

/** The default metadata connection: local YAML files, no external database required. */
public class FileConnection implements Connection {

    private final PocketWorldPlugin plugin;
    private DAO dataAccess;

    public FileConnection(PocketWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void connect() throws DataSourceConnectionException {
        try {
            this.dataAccess = new FileDAO(plugin.getDataFolder().toPath().resolve("data"));
        } catch (IOException e) {
            throw new DataSourceConnectionException("Failed to initialize file-based metadata storage: " + e.getMessage());
        }
        plugin.getLogger().info("Using local YAML files for metadata storage (enable mongodb.use or mysql.use to use a database instead).");
    }

    @Override
    public void close() {
        // No external resource to release.
    }

    @Override
    public DAO getDAO() {
        return dataAccess;
    }
}
