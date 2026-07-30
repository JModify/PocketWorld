package com.pocketworld.plugin.data.mysql;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.data.Connection;
import com.pocketworld.plugin.data.DAO;
import com.pocketworld.plugin.exceptions.DataSourceConnectionException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;

public class MysqlConnection implements Connection {

    private final PocketWorldPlugin plugin;
    private HikariDataSource dataSource;
    private DAO dataAccess;

    public MysqlConnection(PocketWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void connect() throws DataSourceConnectionException {
        String host = plugin.getConfigFile().getYaml().getString("mysql.host", null);
        String database = plugin.getConfigFile().getYaml().getString("mysql.database", null);
        String username = plugin.getConfigFile().getYaml().getString("mysql.username", null);
        String password = plugin.getConfigFile().getYaml().getString("mysql.password", null);
        int port = plugin.getConfigFile().getYaml().getInt("mysql.port", 3306);

        if (host == null || database == null || username == null || password == null) {
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            throw new DataSourceConnectionException("Failed to connect to MySQL. Corrupted configuration.");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&serverTimezone=UTC");
        config.setUsername(username);
        config.setPassword(password);
        config.setPoolName("pocketworld-mysql-metadata");
        config.setMaximumPoolSize(8);

        try {
            this.dataSource = new HikariDataSource(config);
            this.dataAccess = new MysqlDAO(dataSource);
        } catch (RuntimeException | IOException e) {
            if (dataSource != null) {
                dataSource.close();
            }
            throw new DataSourceConnectionException("Failed to make connection to database: " + e.getMessage());
        }

        plugin.getLogger().info("Successfully connected to MySQL.");
    }

    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Override
    public DAO getDAO() {
        return dataAccess;
    }
}
