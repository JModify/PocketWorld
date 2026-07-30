package com.pocketworld.plugin.data.mysql;

import com.pocketworld.plugin.data.DAO;
import com.pocketworld.plugin.theme.PocketTheme;
import com.pocketworld.plugin.user.PocketUser;
import com.pocketworld.plugin.world.Invitation;
import com.pocketworld.plugin.world.PocketWorld;
import com.pocketworld.plugin.world.WorldRank;
import com.pocketworld.plugin.world.WorldSpawn;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Material;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Plain-JDBC MySQL implementation of {@link DAO}, backed by a HikariCP pool. Replaces the original
 * project's MySQL support, which was an unimplemented stub that silently NPE'd on first use if a
 * server operator ever set {@code mysql.use: true}.
 */
public class MysqlDAO implements DAO {

    private static final String[] SCHEMA = {
            "CREATE TABLE IF NOT EXISTS pocketworld_users ("
                    + "id VARCHAR(36) NOT NULL PRIMARY KEY, "
                    + "name VARCHAR(16) NOT NULL)",
            "CREATE TABLE IF NOT EXISTS pocketworld_user_worlds ("
                    + "user_id VARCHAR(36) NOT NULL, "
                    + "world_id VARCHAR(36) NOT NULL, "
                    + "PRIMARY KEY (user_id, world_id))",
            "CREATE TABLE IF NOT EXISTS pocketworld_user_invitations ("
                    + "user_id VARCHAR(36) NOT NULL, "
                    + "world_id VARCHAR(36) NOT NULL, "
                    + "PRIMARY KEY (user_id, world_id))",
            "CREATE TABLE IF NOT EXISTS pocketworld_worlds ("
                    + "id VARCHAR(36) NOT NULL PRIMARY KEY, "
                    + "name VARCHAR(64) NOT NULL, "
                    + "icon VARCHAR(64) NOT NULL, "
                    + "biome VARCHAR(64) NOT NULL, "
                    + "world_size INT NOT NULL, "
                    + "world_spawn VARCHAR(128) NOT NULL, "
                    + "allow_animals BOOLEAN NOT NULL, "
                    + "allow_monsters BOOLEAN NOT NULL, "
                    + "pvp BOOLEAN NOT NULL)",
            "CREATE TABLE IF NOT EXISTS pocketworld_world_users ("
                    + "world_id VARCHAR(36) NOT NULL, "
                    + "user_id VARCHAR(36) NOT NULL, "
                    + "world_rank VARCHAR(16) NOT NULL, "
                    + "PRIMARY KEY (world_id, user_id))",
            "CREATE TABLE IF NOT EXISTS pocketworld_world_invitations ("
                    + "world_id VARCHAR(36) NOT NULL, "
                    + "recipient_id VARCHAR(36) NOT NULL, "
                    + "sender_id VARCHAR(36) NOT NULL, "
                    + "sent_at BIGINT NOT NULL, "
                    + "PRIMARY KEY (world_id, recipient_id))",
            "CREATE TABLE IF NOT EXISTS pocketworld_themes ("
                    + "id VARCHAR(36) NOT NULL PRIMARY KEY, "
                    + "name VARCHAR(64) NOT NULL, "
                    + "biome VARCHAR(64) NOT NULL, "
                    + "icon VARCHAR(64) NOT NULL, "
                    + "description VARCHAR(512) NOT NULL, "
                    + "spawn_point VARCHAR(128) NOT NULL)",
    };

    private final HikariDataSource dataSource;

    public MysqlDAO(HikariDataSource dataSource) throws IOException {
        this.dataSource = dataSource;
        try (java.sql.Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            for (String sql : SCHEMA) {
                statement.executeUpdate(sql);
            }
        } catch (SQLException e) {
            throw new IOException("Failed to create/verify PocketWorld's MySQL schema", e);
        }
    }

    @Override
    public void registerPocketWorld(PocketWorld world) {
        if (world == null) {
            return;
        }
        try (java.sql.Connection connection = dataSource.getConnection()) {
            insertWorldRow(connection, world);
            replaceWorldUsers(connection, world);
            replaceWorldInvitations(connection, world);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to register pocket world " + world.getId(), e);
        }
    }

    @Override
    public PocketWorld getPocketWorld(UUID worldId) {
        String sql = "SELECT * FROM pocketworld_worlds WHERE id = ?";
        try (java.sql.Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, worldId.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                String worldName = resultSet.getString("name");
                Material icon = Material.valueOf(resultSet.getString("icon"));
                String biome = resultSet.getString("biome");
                int worldSize = resultSet.getInt("world_size");
                WorldSpawn worldSpawn = WorldSpawn.fromString(resultSet.getString("world_spawn"));
                boolean allowAnimals = resultSet.getBoolean("allow_animals");
                boolean allowMonsters = resultSet.getBoolean("allow_monsters");
                boolean pvp = resultSet.getBoolean("pvp");

                Map<UUID, WorldRank> users = readWorldUsers(connection, worldId);
                Map<UUID, Invitation> invitations = readWorldInvitations(connection, worldId);

                return new PocketWorld(worldId, worldName, icon, users, invitations, biome, worldSize, worldSpawn,
                        allowAnimals, allowMonsters, pvp, false);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read pocket world " + worldId, e);
        }
    }

    @Override
    public void updatePocketWorld(PocketWorld world) {
        try (java.sql.Connection connection = dataSource.getConnection()) {
            String sql = "UPDATE pocketworld_worlds SET name = ?, icon = ?, biome = ?, world_size = ?, "
                    + "world_spawn = ?, allow_animals = ?, allow_monsters = ?, pvp = ? WHERE id = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, world.getWorldName());
                statement.setString(2, world.getIcon().name());
                statement.setString(3, world.getBiome());
                statement.setInt(4, world.getWorldSize());
                statement.setString(5, world.getWorldSpawn().toString());
                statement.setBoolean(6, world.isAllowAnimals());
                statement.setBoolean(7, world.isAllowMonsters());
                statement.setBoolean(8, world.isPvp());
                statement.setString(9, world.getId().toString());
                statement.executeUpdate();
            }

            replaceWorldUsers(connection, world);
            replaceWorldInvitations(connection, world);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update pocket world " + world.getId(), e);
        }
    }

    @Override
    public boolean registerPocketUser(UUID userId, String username) {
        String selectSql = "SELECT 1 FROM pocketworld_users WHERE id = ?";
        try (java.sql.Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(selectSql)) {
                statement.setString(1, userId.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return true;
                    }
                }
            }

            String insertSql = "INSERT INTO pocketworld_users (id, name) VALUES (?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
                statement.setString(1, userId.toString());
                statement.setString(2, username);
                statement.executeUpdate();
            }
            return false;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to register pocket user " + userId, e);
        }
    }

    @Override
    public PocketUser getPocketUser(UUID userId) {
        String sql = "SELECT name FROM pocketworld_users WHERE id = ?";
        try (java.sql.Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                String name = resultSet.getString("name");
                Set<UUID> worlds = readIdSet(connection, "pocketworld_user_worlds", "user_id", "world_id", userId);
                Set<UUID> invitations = readIdSet(connection, "pocketworld_user_invitations", "user_id", "world_id", userId);
                return new PocketUser(userId, name, invitations, worlds);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read pocket user " + userId, e);
        }
    }

    @Override
    public void updatePocketUser(PocketUser user) {
        try (java.sql.Connection connection = dataSource.getConnection()) {
            String sql = "UPDATE pocketworld_users SET name = ? WHERE id = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, user.getName());
                statement.setString(2, user.getId().toString());
                statement.executeUpdate();
            }

            replaceIdSet(connection, "pocketworld_user_worlds", "user_id", "world_id", user.getId(), user.getWorlds());
            replaceIdSet(connection, "pocketworld_user_invitations", "user_id", "world_id", user.getId(), user.getInvitations());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update pocket user " + user.getId(), e);
        }
    }

    @Override
    public void registerPocketTheme(PocketTheme theme) {
        String sql = "INSERT INTO pocketworld_themes (id, name, biome, icon, description, spawn_point) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (java.sql.Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, theme.getId().toString());
            statement.setString(2, theme.getName());
            statement.setString(3, theme.getBiome());
            statement.setString(4, theme.getIcon().name());
            statement.setString(5, theme.getDescription());
            statement.setString(6, theme.getSpawnPoint());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to register pocket theme " + theme.getId(), e);
        }
    }

    @Override
    public PocketTheme getPocketTheme(UUID themeId) {
        String sql = "SELECT * FROM pocketworld_themes WHERE id = ?";
        try (java.sql.Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, themeId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? themeFromRow(resultSet) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read pocket theme " + themeId, e);
        }
    }

    @Override
    public Set<PocketTheme> getAllPocketThemes() {
        String sql = "SELECT * FROM pocketworld_themes";
        Set<PocketTheme> themes = new HashSet<>();
        try (java.sql.Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                themes.add(themeFromRow(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read pocket themes", e);
        }
        return themes;
    }

    @Override
    public void deleteTheme(UUID themeId) {
        String sql = "DELETE FROM pocketworld_themes WHERE id = ?";
        try (java.sql.Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, themeId.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete pocket theme " + themeId, e);
        }
    }

    private static PocketTheme themeFromRow(ResultSet resultSet) throws SQLException {
        UUID id = UUID.fromString(resultSet.getString("id"));
        String name = resultSet.getString("name");
        String biome = resultSet.getString("biome");
        Material icon = Material.valueOf(resultSet.getString("icon"));
        String description = resultSet.getString("description");
        String spawnPoint = resultSet.getString("spawn_point");
        return new PocketTheme(id, name, description, spawnPoint, biome, icon);
    }

    private static void insertWorldRow(java.sql.Connection connection, PocketWorld world) throws SQLException {
        String sql = "INSERT INTO pocketworld_worlds (id, name, icon, biome, world_size, world_spawn, "
                + "allow_animals, allow_monsters, pvp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, world.getId().toString());
            statement.setString(2, world.getWorldName());
            statement.setString(3, world.getIcon().name());
            statement.setString(4, world.getBiome());
            statement.setInt(5, world.getWorldSize());
            statement.setString(6, world.getWorldSpawn().toString());
            statement.setBoolean(7, world.isAllowAnimals());
            statement.setBoolean(8, world.isAllowMonsters());
            statement.setBoolean(9, world.isPvp());
            statement.executeUpdate();
        }
    }

    private static Map<UUID, WorldRank> readWorldUsers(java.sql.Connection connection, UUID worldId) throws SQLException {
        String sql = "SELECT user_id, world_rank FROM pocketworld_world_users WHERE world_id = ?";
        Map<UUID, WorldRank> users = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, worldId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    UUID userId = UUID.fromString(resultSet.getString("user_id"));
                    WorldRank rank = WorldRank.valueOf(resultSet.getString("world_rank").toUpperCase());
                    users.put(userId, rank);
                }
            }
        }
        return users;
    }

    private static Map<UUID, Invitation> readWorldInvitations(java.sql.Connection connection, UUID worldId) throws SQLException {
        String sql = "SELECT recipient_id, sender_id, sent_at FROM pocketworld_world_invitations WHERE world_id = ?";
        Map<UUID, Invitation> invitations = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, worldId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    UUID recipient = UUID.fromString(resultSet.getString("recipient_id"));
                    UUID sender = UUID.fromString(resultSet.getString("sender_id"));
                    long sentAt = resultSet.getLong("sent_at");
                    invitations.put(recipient, new Invitation(sender, recipient, sentAt));
                }
            }
        }
        return invitations;
    }

    private static void replaceWorldUsers(java.sql.Connection connection, PocketWorld world) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM pocketworld_world_users WHERE world_id = ?")) {
            delete.setString(1, world.getId().toString());
            delete.executeUpdate();
        }

        String insertSql = "INSERT INTO pocketworld_world_users (world_id, user_id, world_rank) VALUES (?, ?, ?)";
        try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
            for (Map.Entry<UUID, WorldRank> entry : world.getUsers().entrySet()) {
                insert.setString(1, world.getId().toString());
                insert.setString(2, entry.getKey().toString());
                insert.setString(3, entry.getValue().toString());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private static void replaceWorldInvitations(java.sql.Connection connection, PocketWorld world) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM pocketworld_world_invitations WHERE world_id = ?")) {
            delete.setString(1, world.getId().toString());
            delete.executeUpdate();
        }

        String insertSql = "INSERT INTO pocketworld_world_invitations (world_id, recipient_id, sender_id, sent_at) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
            for (Invitation invitation : world.getInvitations().values()) {
                insert.setString(1, world.getId().toString());
                insert.setString(2, invitation.recipient().toString());
                insert.setString(3, invitation.sender().toString());
                insert.setLong(4, invitation.timestamp());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private static Set<UUID> readIdSet(java.sql.Connection connection, String table, String ownerColumn,
                                        String valueColumn, UUID ownerId) throws SQLException {
        String sql = "SELECT " + valueColumn + " FROM " + table + " WHERE " + ownerColumn + " = ?";
        Set<UUID> ids = new HashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ownerId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(UUID.fromString(resultSet.getString(valueColumn)));
                }
            }
        }
        return ids;
    }

    private static void replaceIdSet(java.sql.Connection connection, String table, String ownerColumn,
                                      String valueColumn, UUID ownerId, Set<UUID> values) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM " + table + " WHERE " + ownerColumn + " = ?")) {
            delete.setString(1, ownerId.toString());
            delete.executeUpdate();
        }

        String insertSql = "INSERT INTO " + table + " (" + ownerColumn + ", " + valueColumn + ") VALUES (?, ?)";
        try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
            for (UUID value : values) {
                insert.setString(1, ownerId.toString());
                insert.setString(2, value.toString());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }
}
