package me.modify.pocketworld.data;

import me.modify.pocketworld.exceptions.DataSourceConnectionException;

/**
 * Represents a connection to a data source.
 */
public interface Connection {

    /**
     * Connect to the data source
     * @throws DataSourceConnectionException if the connection failed.
     */
    void connect() throws DataSourceConnectionException;

    /**
     * Closes this connection.
     */
    void close();

    /**
     * Returns a reference to the data access object.
     * @return data access object
     */
    DAO getDAO();
}
