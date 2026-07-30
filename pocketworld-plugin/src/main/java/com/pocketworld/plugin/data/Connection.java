package com.pocketworld.plugin.data;

import com.pocketworld.plugin.exceptions.DataSourceConnectionException;

/** A connection to the metadata data source (users/worlds/themes) - not the Slime world storage. */
public interface Connection {

    /** @throws DataSourceConnectionException if the connection failed. */
    void connect() throws DataSourceConnectionException;

    void close();

    DAO getDAO();
}
