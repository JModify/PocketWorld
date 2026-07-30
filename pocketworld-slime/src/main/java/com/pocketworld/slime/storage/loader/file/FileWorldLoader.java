package com.pocketworld.slime.storage.loader.file;

import com.pocketworld.slime.storage.UnknownWorldException;
import com.pocketworld.slime.storage.WorldAlreadyExistsException;
import com.pocketworld.slime.storage.WorldLoader;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Stores each world as a single {@code <worldId>.slime} file in a directory. This is the default,
 * zero-external-dependency backend.
 */
public final class FileWorldLoader implements WorldLoader {

    private static final Pattern SAFE_WORLD_ID = Pattern.compile("[A-Za-z0-9_-]+");
    private static final String EXTENSION = ".slime";

    private final Path directory;

    public FileWorldLoader(Path directory) throws IOException {
        Files.createDirectories(directory);
        this.directory = directory.toAbsolutePath().normalize();
    }

    @Override
    public boolean exists(String worldId) {
        return Files.isRegularFile(fileFor(worldId));
    }

    @Override
    public byte[] read(String worldId) throws IOException {
        Path file = fileFor(worldId);
        if (!Files.isRegularFile(file)) {
            throw new UnknownWorldException(worldId);
        }
        return Files.readAllBytes(file);
    }

    @Override
    public void write(String worldId, byte[] data) throws IOException {
        Path file = fileFor(worldId);
        Path tempFile = directory.resolve(file.getFileName() + "." + System.nanoTime() + ".tmp");
        try {
            Files.write(tempFile, data);
            Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Override
    public void delete(String worldId) throws IOException {
        Path file = fileFor(worldId);
        try {
            Files.delete(file);
        } catch (java.nio.file.NoSuchFileException e) {
            throw new UnknownWorldException(worldId);
        }
    }

    @Override
    public List<String> list() throws IOException {
        List<String> ids = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*" + EXTENSION)) {
            for (Path file : stream) {
                String name = file.getFileName().toString();
                ids.add(name.substring(0, name.length() - EXTENSION.length()));
            }
        }
        return ids;
    }

    @Override
    public void cloneWorld(String worldId, WorldLoader targetLoader, String targetWorldId) throws IOException {
        if (!(targetLoader instanceof FileWorldLoader fileTarget)) {
            WorldLoader.super.cloneWorld(worldId, targetLoader, targetWorldId);
            return;
        }
        Path source = fileFor(worldId);
        if (!Files.isRegularFile(source)) {
            throw new UnknownWorldException(worldId);
        }
        Path target = fileTarget.fileFor(targetWorldId);
        if (Files.isRegularFile(target)) {
            throw new WorldAlreadyExistsException(targetWorldId);
        }
        Files.copy(source, target);
    }

    private Path fileFor(String worldId) {
        if (worldId == null || !SAFE_WORLD_ID.matcher(worldId).matches()) {
            throw new IllegalArgumentException(
                    "World id must be non-empty and contain only letters, digits, '-' or '_': " + worldId);
        }
        return directory.resolve(worldId + EXTENSION);
    }
}
