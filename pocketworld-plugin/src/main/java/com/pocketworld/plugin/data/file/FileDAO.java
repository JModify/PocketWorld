package com.pocketworld.plugin.data.file;

import com.pocketworld.plugin.data.DAO;
import com.pocketworld.plugin.theme.PocketTheme;
import com.pocketworld.plugin.user.PocketUser;
import com.pocketworld.plugin.world.Invitation;
import com.pocketworld.plugin.world.PocketWorld;
import com.pocketworld.plugin.world.WorldRank;
import com.pocketworld.plugin.world.WorldSpawn;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Zero-external-dependency metadata storage: each world/user/theme is a single YAML file. The
 * default {@link DAO} implementation when neither {@code mongodb.use} nor {@code mysql.use} is
 * enabled - the plugin's world data is already file-based by default (see
 * {@link com.pocketworld.slime.storage.loader.file.FileWorldLoader}), so this closes the gap that
 * previously meant the plugin couldn't start at all without an external database, even though it
 * only ever needed one for these small metadata records.
 */
public final class FileDAO implements DAO {

    private static final String EXTENSION = ".yml";

    private final Path worldsDir;
    private final Path usersDir;
    private final Path themesDir;

    public FileDAO(Path baseDirectory) throws IOException {
        this.worldsDir = baseDirectory.resolve("worlds");
        this.usersDir = baseDirectory.resolve("users");
        this.themesDir = baseDirectory.resolve("themes");
        Files.createDirectories(worldsDir);
        Files.createDirectories(usersDir);
        Files.createDirectories(themesDir);
    }

    @Override
    public void registerPocketWorld(PocketWorld world) {
        if (world == null) {
            return;
        }
        writeWorld(world);
    }

    @Override
    public PocketWorld getPocketWorld(UUID worldId) {
        Path file = fileFor(worldsDir, worldId);
        return Files.isRegularFile(file) ? readWorld(worldId, file) : null;
    }

    @Override
    public void updatePocketWorld(PocketWorld world) {
        writeWorld(world);
    }

    @Override
    public boolean registerPocketUser(UUID userId, String username) {
        Path file = fileFor(usersDir, userId);
        if (Files.isRegularFile(file)) {
            return true;
        }
        writeUser(new PocketUser(userId, username, new HashSet<>(), new HashSet<>()));
        return false;
    }

    @Override
    public PocketUser getPocketUser(UUID userId) {
        Path file = fileFor(usersDir, userId);
        return Files.isRegularFile(file) ? readUser(userId, file) : null;
    }

    @Override
    public void updatePocketUser(PocketUser user) {
        writeUser(user);
    }

    @Override
    public void registerPocketTheme(PocketTheme theme) {
        writeTheme(theme);
    }

    @Override
    public PocketTheme getPocketTheme(UUID themeId) {
        Path file = fileFor(themesDir, themeId);
        return Files.isRegularFile(file) ? readTheme(themeId, file) : null;
    }

    @Override
    public Set<PocketTheme> getAllPocketThemes() {
        Set<PocketTheme> themes = new HashSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(themesDir, "*" + EXTENSION)) {
            for (Path file : stream) {
                themes.add(readTheme(idFromFileName(file), file));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to list themes", e);
        }
        return themes;
    }

    @Override
    public void deleteTheme(UUID themeId) {
        try {
            Files.deleteIfExists(fileFor(themesDir, themeId));
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete theme " + themeId, e);
        }
    }

    private void writeWorld(PocketWorld world) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("name", world.getWorldName());
        yaml.set("icon", world.getIcon().name());
        yaml.set("biome", world.getBiome());
        yaml.set("world-size", world.getWorldSize());
        yaml.set("world-spawn", world.getWorldSpawn().toString());
        yaml.set("allow-animals", world.isAllowAnimals());
        yaml.set("allow-monsters", world.isAllowMonsters());
        yaml.set("pvp", world.isPvp());

        ConfigurationSection usersSection = yaml.createSection("users");
        for (Map.Entry<UUID, WorldRank> entry : world.getUsers().entrySet()) {
            usersSection.set(entry.getKey().toString(), entry.getValue().name());
        }

        ConfigurationSection invitationsSection = yaml.createSection("invitations");
        for (Invitation invitation : world.getInvitations().values()) {
            ConfigurationSection entry = invitationsSection.createSection(invitation.recipient().toString());
            entry.set("sender", invitation.sender().toString());
            entry.set("sent-at", invitation.timestamp());
        }

        save(yaml, fileFor(worldsDir, world.getId()));
    }

    private PocketWorld readWorld(UUID id, Path file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());

        String name = yaml.getString("name");
        Material icon = Material.valueOf(yaml.getString("icon"));
        String biome = yaml.getString("biome");
        int worldSize = yaml.getInt("world-size");
        WorldSpawn worldSpawn = WorldSpawn.fromString(yaml.getString("world-spawn"));
        boolean allowAnimals = yaml.getBoolean("allow-animals");
        boolean allowMonsters = yaml.getBoolean("allow-monsters");
        boolean pvp = yaml.getBoolean("pvp");

        Map<UUID, WorldRank> users = new HashMap<>();
        ConfigurationSection usersSection = yaml.getConfigurationSection("users");
        if (usersSection != null) {
            for (String key : usersSection.getKeys(false)) {
                users.put(UUID.fromString(key), WorldRank.valueOf(usersSection.getString(key).toUpperCase()));
            }
        }

        Map<UUID, Invitation> invitations = new HashMap<>();
        ConfigurationSection invitationsSection = yaml.getConfigurationSection("invitations");
        if (invitationsSection != null) {
            for (String key : invitationsSection.getKeys(false)) {
                ConfigurationSection entry = invitationsSection.getConfigurationSection(key);
                UUID recipient = UUID.fromString(key);
                UUID sender = UUID.fromString(entry.getString("sender"));
                long sentAt = entry.getLong("sent-at");
                invitations.put(recipient, new Invitation(sender, recipient, sentAt));
            }
        }

        return new PocketWorld(id, name, icon, users, invitations, biome, worldSize, worldSpawn,
                allowAnimals, allowMonsters, pvp, false);
    }

    private void writeUser(PocketUser user) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("name", user.getName());
        yaml.set("worlds", user.getWorlds().stream().map(UUID::toString).toList());
        yaml.set("invitations", user.getInvitations().stream().map(UUID::toString).toList());
        save(yaml, fileFor(usersDir, user.getId()));
    }

    private PocketUser readUser(UUID id, Path file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());
        String name = yaml.getString("name");
        Set<UUID> worlds = yaml.getStringList("worlds").stream()
                .map(UUID::fromString).collect(Collectors.toCollection(HashSet::new));
        Set<UUID> invitations = yaml.getStringList("invitations").stream()
                .map(UUID::fromString).collect(Collectors.toCollection(HashSet::new));
        return new PocketUser(id, name, invitations, worlds);
    }

    private void writeTheme(PocketTheme theme) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("name", theme.getName());
        yaml.set("biome", theme.getBiome());
        yaml.set("icon", theme.getIcon().name());
        yaml.set("description", theme.getDescription());
        yaml.set("spawn-point", theme.getSpawnPoint());
        save(yaml, fileFor(themesDir, theme.getId()));
    }

    private PocketTheme readTheme(UUID id, Path file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());
        String name = yaml.getString("name");
        String biome = yaml.getString("biome");
        Material icon = Material.valueOf(yaml.getString("icon"));
        String description = yaml.getString("description");
        String spawnPoint = yaml.getString("spawn-point");
        return new PocketTheme(id, name, description, spawnPoint, biome, icon);
    }

    private static Path fileFor(Path dir, UUID id) {
        return dir.resolve(id + EXTENSION);
    }

    private static UUID idFromFileName(Path file) {
        String name = file.getFileName().toString();
        return UUID.fromString(name.substring(0, name.length() - EXTENSION.length()));
    }

    /** Writes to a temp file and atomically moves it into place, so a crash mid-write can't leave a torn file. */
    private static void save(YamlConfiguration yaml, Path file) {
        Path tempFile = file.resolveSibling(file.getFileName() + "." + System.nanoTime() + ".tmp");
        try {
            yaml.save(tempFile.toFile());
            Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save " + file, e);
        } finally {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {
                // best-effort cleanup - the move above already succeeded or failed definitively
            }
        }
    }
}
