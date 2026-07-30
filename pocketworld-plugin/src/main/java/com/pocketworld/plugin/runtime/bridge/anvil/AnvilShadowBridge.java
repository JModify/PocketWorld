package com.pocketworld.plugin.runtime.bridge.anvil;

import com.pocketworld.plugin.runtime.WorldProperties;
import com.pocketworld.plugin.runtime.bridge.WorldRuntimeBridge;
import com.pocketworld.slime.anvil.AnvilChunkConverter;
import com.pocketworld.slime.anvil.AnvilWorldReader;
import com.pocketworld.slime.anvil.AnvilWorldWriter;
import com.pocketworld.slime.anvil.ChunkPos;
import com.pocketworld.slime.model.SlimeChunkData;
import com.pocketworld.slime.model.SlimeWorldData;
import com.pocketworld.slime.model.SlimeWorldFlag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The always-available runtime bridge: materializes Slime data as a real, temporary vanilla-format
 * world folder and lets Paper's own world loader handle everything from there - no NMS, no
 * reflection, just {@link WorldCreator} and public API. This is what the plugin falls back to on
 * any Paper version, and the only bridge on versions without a dedicated NMS adapter.
 * <p>
 * <b>On-disk layout</b>: chunks are always written using the classic, long-established
 * {@code <world>/region}, {@code <world>/entities} layout. Minecraft 26.2 restructured how it
 * actually stores per-dimension data on disk (discovered empirically, not documented anywhere
 * available at design time - see docs/ARCHITECTURE.md); rather than reverse-engineer and hardcode
 * that internal layout, this bridge deliberately writes the classic layout and lets Paper's own
 * {@code LegacyCraftBukkitWorldMigration} relocate it on world creation, which was confirmed (by
 * actually running it) to fully relocate the data with no leftover. Reading a *live* world back out
 * uses {@link World#getWorldFolder()} directly, which reliably points at wherever the data actually
 * ended up regardless of internal layout - Bukkit already abstracts this away once the world exists.
 */
public final class AnvilShadowBridge implements WorldRuntimeBridge {

    @Override
    public String name() {
        return "anvil-shadow";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public World materialize(SlimeWorldData data, String worldName, WorldProperties properties) throws IOException {
        Path worldFolder = Bukkit.getWorldContainer().toPath().resolve(worldName);
        Files.createDirectories(worldFolder);

        Map<ChunkPos, CompoundBinaryTag> regionChunks = new LinkedHashMap<>();
        Map<ChunkPos, CompoundBinaryTag> entityChunks = new LinkedHashMap<>();
        for (SlimeChunkData chunk : data.chunks()) {
            AnvilChunkConverter.VanillaChunk vanilla = AnvilChunkConverter.toVanillaChunk(data.dataVersion(), chunk);
            ChunkPos pos = new ChunkPos(chunk.x(), chunk.z());
            regionChunks.put(pos, vanilla.region());
            entityChunks.put(pos, vanilla.entities());
        }

        AnvilWorldWriter.writeAll(worldFolder.resolve("region"), regionChunks);
        AnvilWorldWriter.writeAll(worldFolder.resolve("entities"), entityChunks);

        LevelDatWriter.write(worldFolder, worldName, data.dataVersion(), properties);

        WorldCreator creator = new WorldCreator(worldName)
                .environment(World.Environment.NORMAL)
                .generateStructures(false);
        World world = Bukkit.createWorld(creator);
        if (world == null) {
            throw new IOException("Bukkit refused to create/load world \"" + worldName + "\"");
        }

        world.setSpawnLocation((int) Math.floor(properties.spawnX()), (int) Math.floor(properties.spawnY()),
                (int) Math.floor(properties.spawnZ()), properties.spawnYaw());
        world.setDifficulty(parseDifficulty(properties.difficulty()));
        world.setPVP(properties.pvp());

        return world;
    }

    @Override
    public SlimeWorldData extract(World world) throws IOException {
        world.save();

        Path dimensionRoot = world.getWorldFolder().toPath();
        Map<ChunkPos, CompoundBinaryTag> regionChunks = AnvilWorldReader.readAll(dimensionRoot.resolve("region"));
        Map<ChunkPos, CompoundBinaryTag> entityChunks = AnvilWorldReader.readAll(dimensionRoot.resolve("entities"));

        List<SlimeChunkData> chunks = new ArrayList<>();
        for (Map.Entry<ChunkPos, CompoundBinaryTag> entry : regionChunks.entrySet()) {
            CompoundBinaryTag entities = entityChunks.get(entry.getKey());
            chunks.add(AnvilChunkConverter.toSlimeChunk(entry.getKey().x(), entry.getKey().z(), entry.getValue(), entities));
        }

        int dataVersion = Bukkit.getUnsafe().getDataVersion();
        return new SlimeWorldData(dataVersion, EnumSet.of(SlimeWorldFlag.BLOCK_TICKS, SlimeWorldFlag.FLUID_TICKS),
                chunks, CompoundBinaryTag.empty());
    }

    @Override
    public void discard(String worldName, Path worldFolder) throws IOException {
        deleteRecursively(worldFolder);
        // Belt-and-braces: if this particular server/version's migration path ever leaves the
        // classic pre-migration folder behind (it didn't, when this was verified), clean it up too.
        Path legacyFolder = Bukkit.getWorldContainer().toPath().resolve(worldName);
        if (!legacyFolder.equals(worldFolder)) {
            deleteRecursively(legacyFolder);
        }
    }

    private static void deleteRecursively(Path folder) throws IOException {
        if (!Files.isDirectory(folder)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(folder)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private static Difficulty parseDifficulty(String name) {
        return switch (name.toLowerCase(java.util.Locale.ROOT)) {
            case "peaceful" -> Difficulty.PEACEFUL;
            case "easy" -> Difficulty.EASY;
            case "hard" -> Difficulty.HARD;
            default -> Difficulty.NORMAL;
        };
    }
}
