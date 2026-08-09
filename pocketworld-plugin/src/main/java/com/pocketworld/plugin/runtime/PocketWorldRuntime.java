package com.pocketworld.plugin.runtime;

import com.pocketworld.plugin.runtime.bridge.ChunkBounds;
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
import java.util.OptionalInt;

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

    /**
     * Deletes a world's stored bytes outright, without touching any live Bukkit world - also evicts
     * any bridge-side cache left over from an earlier session, since a world can be deleted while
     * already unloaded. Pure I/O.
     */
    public void deleteStored(String worldId) throws IOException {
        storage.delete(worldId);
        bridge.evictCache(worldId);
    }

    public boolean exists(String worldId) throws IOException {
        return storage.exists(worldId);
    }

    /** All stored world ids. Pure I/O - safe off the main thread. */
    public List<String> list() throws IOException {
        return storage.list();
    }

    /**
     * Fully decodes a stored world's bytes without touching the runtime bridge or any live Bukkit
     * state - the same decode step {@link #prepareLoad} already runs on every load/create, exposed
     * standalone so a stored world can be checked for corruption proactively (e.g. by an admin
     * command) rather than only ever surfacing as a caught-and-logged exception the first time a
     * player happens to load it. Returns normally if the data is structurally valid; throws
     * (typically a {@code SlimeFormatException}) otherwise. Pure I/O - safe off the main thread.
     */
    public void validate(String worldId) throws IOException {
        decode(worldId);
    }

    /**
     * Prepares {@code worldId} to be {@link #activate}d. If the bridge already has a valid warm
     * cache for this world (nothing's changed since it was last unloaded), the expensive
     * decode-and-rewrite is skipped entirely and that cache is reused as-is; otherwise this reads
     * and decodes the stored world and writes whatever on-disk state the bridge needs. Pure I/O -
     * safe off the main thread.
     *
     * @return the world's Minecraft data version, needed by {@link #activate}.
     */
    public int prepareLoad(String worldId) throws IOException {
        OptionalInt cached = bridge.cachedDataVersion(worldId);
        if (cached.isPresent()) {
            return cached.getAsInt();
        }

        SlimeWorldData data = decode(worldId);
        bridge.prepare(data, worldId);
        return data.dataVersion();
    }

    /** Must run on the main thread. {@link #prepareLoad} must have already completed for this id. */
    public World activate(String worldId, int dataVersion, WorldProperties properties) throws IOException {
        return bridge.activate(worldId, dataVersion, properties);
    }

    /**
     * Unloads the world (which must already have no players left in it - see {@link World#getPlayers()}
     * on {@code world} before calling this, or {@code Bukkit.unloadWorld} will simply refuse and return
     * false), then, if {@code save}, reads its now-final on-disk state back out as Slime data. Must
     * run on the main thread. Does NOT write the extracted data to storage - pass the result to
     * {@link #persist} to do that off the main thread.
     * <p>
     * Deliberately unloads (and lets {@code Bukkit.unloadWorld(world, save)} do its own save) before
     * reading anything back, rather than reading while the world is still live: Paper's own async
     * chunk-saving I/O does not necessarily finish writing every chunk to disk synchronously within a
     * plain {@code World.save()} call, which {@code Bukkit.unloadWorld}'s own save path is trusted
     * ecosystem-wide not to have this problem with.
     */
    public SlimeWorldData unloadSync(World world, String worldId, boolean save) throws IOException {
        Path worldFolder = world.getWorldFolder().toPath();
        // Must capture the border now - it's unrecoverable once the world is unloaded, and
        // extractUnloaded() needs it to drop any chunk Paper's own generation pipeline touched
        // outside it (see ChunkBounds).
        ChunkBounds bounds = ChunkBounds.fromWorldBorder(world.getWorldBorder());

        if (!Bukkit.unloadWorld(world, save)) {
            throw new IOException("Bukkit refused to unload world \"" + worldId + "\" - a player may still be in it");
        }

        SlimeWorldData data = save ? bridge.extractUnloaded(worldFolder, bounds) : null;
        bridge.afterUnload(worldId, worldFolder, data != null, data != null ? data.dataVersion() : -1);
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
