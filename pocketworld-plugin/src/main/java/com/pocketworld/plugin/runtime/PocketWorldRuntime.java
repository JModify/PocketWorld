package com.pocketworld.plugin.runtime;

import com.pocketworld.plugin.runtime.bridge.WorldRuntimeBridge;
import com.pocketworld.slime.anvil.AnvilChunkConverter;
import com.pocketworld.slime.anvil.AnvilWorldReader;
import com.pocketworld.slime.anvil.AnvilWorldWriter;
import com.pocketworld.slime.anvil.ChunkPos;
import com.pocketworld.slime.format.SlimeReader;
import com.pocketworld.slime.format.SlimeWriter;
import com.pocketworld.slime.model.SlimeChunkData;
import com.pocketworld.slime.model.SlimeWorldData;
import com.pocketworld.slime.model.SlimeWorldFlag;
import com.pocketworld.slime.storage.WorldLoader;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates the storage layer ({@link WorldLoader}) and the active {@link WorldRuntimeBridge}
 * for the whole world lifecycle. This is the one class the rest of the plugin (commands, menus,
 * listeners) should ever need to call to create, load, unload, clone, import, or export a world -
 * everything about the format and the runtime bridge is an implementation detail behind it.
 */
public final class PocketWorldRuntime {

    private final WorldLoader storage;
    private final WorldRuntimeBridge bridge;

    public PocketWorldRuntime(WorldLoader storage, WorldRuntimeBridge bridge) {
        this.storage = storage;
        this.bridge = bridge;
    }

    /** Clones {@code templateWorldId} to {@code newWorldId} in storage, then loads the copy. */
    public World create(String templateWorldId, String newWorldId, WorldProperties properties) throws IOException {
        storage.cloneWorld(templateWorldId, storage, newWorldId);
        return load(newWorldId, properties);
    }

    public World load(String worldId, WorldProperties properties) throws IOException {
        SlimeWorldData data = SlimeReader.read(new ByteArrayInputStream(storage.read(worldId)));
        return bridge.materialize(data, worldId, properties);
    }

    public void unload(World world, String worldId, boolean save) throws IOException {
        Path worldFolder = world.getWorldFolder().toPath(); // capture before unloading - see WorldRuntimeBridge#discard
        if (save) {
            SlimeWorldData data = bridge.extract(world);
            storage.write(worldId, encode(data));
        }
        Bukkit.unloadWorld(world, false);
        bridge.discard(worldId, worldFolder);
    }

    public void cloneWorld(String sourceWorldId, String targetWorldId) throws IOException {
        storage.cloneWorld(sourceWorldId, storage, targetWorldId);
    }

    /** Imports a real, on-disk vanilla-format world folder (a {@code region/}+{@code entities/} pair) into storage. */
    public void importWorld(Path anvilWorldFolder, String worldId, int dataVersion) throws IOException {
        Map<ChunkPos, CompoundBinaryTag> region = AnvilWorldReader.readAll(anvilWorldFolder.resolve("region"));
        Map<ChunkPos, CompoundBinaryTag> entities = AnvilWorldReader.readAll(anvilWorldFolder.resolve("entities"));

        List<SlimeChunkData> chunks = new ArrayList<>();
        for (Map.Entry<ChunkPos, CompoundBinaryTag> entry : region.entrySet()) {
            chunks.add(AnvilChunkConverter.toSlimeChunk(entry.getKey().x(), entry.getKey().z(), entry.getValue(),
                    entities.get(entry.getKey())));
        }

        SlimeWorldData data = new SlimeWorldData(dataVersion,
                EnumSet.of(SlimeWorldFlag.BLOCK_TICKS, SlimeWorldFlag.FLUID_TICKS), chunks, CompoundBinaryTag.empty());
        storage.write(worldId, encode(data));
    }

    /** Exports a stored world as a real, on-disk vanilla-format world folder. */
    public void exportWorld(String worldId, Path targetFolder) throws IOException {
        SlimeWorldData data = SlimeReader.read(new ByteArrayInputStream(storage.read(worldId)));

        Map<ChunkPos, CompoundBinaryTag> region = new LinkedHashMap<>();
        Map<ChunkPos, CompoundBinaryTag> entities = new LinkedHashMap<>();
        for (SlimeChunkData chunk : data.chunks()) {
            AnvilChunkConverter.VanillaChunk vanilla = AnvilChunkConverter.toVanillaChunk(data.dataVersion(), chunk);
            ChunkPos pos = new ChunkPos(chunk.x(), chunk.z());
            region.put(pos, vanilla.region());
            entities.put(pos, vanilla.entities());
        }

        AnvilWorldWriter.writeAll(targetFolder.resolve("region"), region);
        AnvilWorldWriter.writeAll(targetFolder.resolve("entities"), entities);
    }

    private static byte[] encode(SlimeWorldData data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SlimeWriter.write(data, out);
        return out.toByteArray();
    }
}
