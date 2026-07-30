package com.pocketworld.slime.storage.loader.mysql;

import com.pocketworld.slime.storage.UnknownWorldException;
import com.pocketworld.slime.storage.WorldLoader;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Stores each world as a row (a {@code LONGBLOB}) in a single MySQL table, via a HikariCP pool. */
public final class MysqlWorldLoader implements WorldLoader, AutoCloseable {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private final HikariDataSource dataSource;
    private final String tableName;

    public MysqlWorldLoader(String jdbcUrl, String username, String password, String tableName) throws IOException {
        if (!SAFE_IDENTIFIER.matcher(tableName).matches()) {
            throw new IllegalArgumentException("Unsafe table name: " + tableName);
        }
        this.tableName = tableName;

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setPoolName("pocketworld-mysql");
        config.setMaximumPoolSize(8);
        this.dataSource = new HikariDataSource(config);

        try {
            createTableIfMissing();
        } catch (IOException e) {
            dataSource.close();
            throw e;
        }
    }

    private void createTableIfMissing() throws IOException {
        String sql = "CREATE TABLE IF NOT EXISTS `" + tableName + "` ("
                + "world_id VARCHAR(255) NOT NULL PRIMARY KEY, "
                + "data LONGBLOB NOT NULL, "
                + "updated_at BIGINT NOT NULL)";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            throw new IOException("Failed to create/verify table `" + tableName + "`", e);
        }
    }

    @Override
    public boolean exists(String worldId) throws IOException {
        String sql = "SELECT 1 FROM `" + tableName + "` WHERE world_id = ? LIMIT 1";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, worldId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new IOException("Failed to check existence of world \"" + worldId + "\"", e);
        }
    }

    @Override
    public byte[] read(String worldId) throws IOException {
        String sql = "SELECT data FROM `" + tableName + "` WHERE world_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, worldId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new UnknownWorldException(worldId);
                }
                return resultSet.getBytes("data");
            }
        } catch (SQLException e) {
            throw new IOException("Failed to read world \"" + worldId + "\"", e);
        }
    }

    @Override
    public void write(String worldId, byte[] data) throws IOException {
        String sql = "INSERT INTO `" + tableName + "` (world_id, data, updated_at) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE data = VALUES(data), updated_at = VALUES(updated_at)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, worldId);
            statement.setBytes(2, data);
            statement.setLong(3, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IOException("Failed to write world \"" + worldId + "\"", e);
        }
    }

    @Override
    public void delete(String worldId) throws IOException {
        String sql = "DELETE FROM `" + tableName + "` WHERE world_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, worldId);
            int updated = statement.executeUpdate();
            if (updated == 0) {
                throw new UnknownWorldException(worldId);
            }
        } catch (SQLException e) {
            throw new IOException("Failed to delete world \"" + worldId + "\"", e);
        }
    }

    @Override
    public List<String> list() throws IOException {
        String sql = "SELECT world_id FROM `" + tableName + "`";
        List<String> ids = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                ids.add(resultSet.getString("world_id"));
            }
        } catch (SQLException e) {
            throw new IOException("Failed to list worlds", e);
        }
        return ids;
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
