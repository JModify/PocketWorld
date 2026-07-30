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
 * <p>
 * Methods are explicitly documented as either pure I/O (safe to call from an async task) or
 * main-thread-only (touch live Bukkit world state). Callers are expected to do their own
 * async-then-sync scheduling around these - {@code PocketWorldRuntime} itself doesn't own a
 * scheduler, so it stays easy to use from contexts that already manage their own threading (a
 * domain object mid-way through its own async load sequence, a command handler, a test).
 */
public final class PocketWorldRuntime {

    private final WorldLoader storage;
    private final WorldRuntimeBridge bridge;

    public PocketWorldRuntime(WorldLoader storage, WorldRuntimeBridge bridge) {
        this.storage = storage;
        this.bridge = bridge;
    }

    /** Pure I/O - safe off the main thread. */
    public void cloneWorld(String sourceWorldId, String targetWorldId) throws IOException {
        storage.cloneWorld(sourceWorldId, storage, targetWorldId);
    }

    /**
     * Clones a world out of THIS runtime's storage into a different runtime's storage under a new
     * id - e.g. cloning a theme's stored world into the world runtime's storage when a player
     * creates a new PocketWorld from that theme. Pure I/O - safe off the main thread.
     */
    public void cloneInto(String sourceWorldId, PocketWorldRuntime targetRuntime, String targetWorldId) throws IOException {
        storage.cloneWorld(sourceWorldId, targetRuntime.storage, targetWorldId);
    }

    /** Deletes a world's stored bytes outright, without touching any live Bukkit world. Pure I/O. */
    public void deleteStored(String worldId) throws IOException {
        storage.delete(worldId);
    }

    public boolean exists(String worldId) throws IOException {
        return storage.exists(worldId);
    }

    /**
     * Reads and decodes a stored world, then writes whatever on-disk state the runtime bridge
     * needs to bring it live. Pure I/O - safe off the main thread. Call {@link #activate} next.
     *
     * @return the decoded world's Minecraft data version, needed by {@link #activate}.
     */
    public int prepareLoad(String worldId) throws IOException {
        SlimeWorldData data = decode(worldId);
        bridge.prepare(data, worldId);
        return data.dataVersion();
    }

    /** Must run on the main thread. {@link #prepareLoad} must have already completed for this id. */
    public World activate(String worldId, int dataVersion, WorldProperties properties) throws IOException {
        return bridge.activate(worldId, dataVersion, properties);
    }

    /**
     * Extracts the world's live state (if {@code save}), unloads it, and cleans up the bridge's
     * on-disk state. Must run on the main thread. Does NOT write the extracted data to storage -
     * pass the result to {@link #persist} to do that off the main thread.
     */
    public SlimeWorldData unloadSync(World world, String worldId, boolean save) throws IOException {
        SlimeWorldData data = save ? bridge.extract(world) : null;
        Path worldFolder = world.getWorldFolder().toPath();
        Bukkit.unloadWorld(world, false);
        bridge.discard(worldId, worldFolder);
        return data;
    }

    /** Pure I/O - safe off the main thread. */
    public void persist(String worldId, SlimeWorldData data) throws IOException {
        storage.write(worldId, encode(data));
    }

    /** Imports a real, on-disk vanilla-format world folder (a {@code region/}+{@code entities/} pair) into storage. Pure I/O. */
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

    /** Exports a stored world as a real, on-disk vanilla-format world folder. Pure I/O. */
    public void exportWorld(String worldId, Path targetFolder) throws IOException {
        SlimeWorldData data = decode(worldId);

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

    private SlimeWorldData decode(String worldId) throws IOException {
        return SlimeReader.read(new ByteArrayInputStream(storage.read(worldId)));
    }

    private static byte[] encode(SlimeWorldData data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SlimeWriter.write(data, out);
        return out.toByteArray();
    }
}
