package me.modify.pocketworld.data.mysql;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.data.Connection;
import me.modify.pocketworld.data.DAO;
import me.modify.pocketworld.exceptions.DataSourceConnectionException;

public class MySQLConnection implements Connection {

    private final PocketWorldPlugin plugin;
    public MySQLConnection(PocketWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void connect() throws DataSourceConnectionException {
    }

    @Override
    public void close() {

    }

    @Override
    public DAO getDAO() {
        return null;
    }


}
