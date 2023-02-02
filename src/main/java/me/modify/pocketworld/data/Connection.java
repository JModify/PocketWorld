package me.modify.pocketworld.data;

import me.modify.pocketworld.exceptions.DataSourceConnectionException;

public interface Connection {

    void connect() throws DataSourceConnectionException;

    void close();

    DAO getDAO();
}
