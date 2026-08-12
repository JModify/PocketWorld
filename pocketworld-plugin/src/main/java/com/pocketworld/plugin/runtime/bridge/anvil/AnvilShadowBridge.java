package com.pocketworld.plugin.runtime.bridge.anvil;

import com.pocketworld.plugin.runtime.WorldProperties;
import com.pocketworld.plugin.runtime.bridge.ChunkBounds;
import com.pocketworld.plugin.runtime.bridge.WorldRuntimeBridge;
import com.pocketworld.plugin.util.ChunkPrewarmer;
import com.pocketworld.plugin.util.VoidGenerator;
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
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * The always-available runtime bridge: materializes Slime data as a real, temporary vanilla-format
 * world folder and lets Paper's own world loader handle everything from there - no NMS, no
 * reflection, just {@link WorldCreator} and public API. This is what the plugin falls back to on
 * any Paper version, and the only bridge on versions without a dedicated NMS adapter.
 * <p>
 * <b>Warm cache</b>: a world's on-disk folder is NOT deleted after a normal (saving) unload -
 * it's kept as a warm cache (see {@link #afterUnload}), so a world that gets revisited soon after
 * (the common case for a small, frequently-used pocket world) can skip decode-and-rewrite entirely
 * ({@link #cachedDataVersion}) and go straight to {@link #activate}. The cache is purely an
 * in-session optimization: the tracking map is in memory only and never trusted across a plugin
 * restart, an entry expires after {@link #CACHE_TTL_MILLIS} of disuse, and {@link #cachedDataVersion}
 * double-checks the expected folder is still actually present on disk before trusting its own record -
 * so a stale/missing/externally-modified folder always safely falls back to a full rebuild rather
 * than risking serving out-of-date data. The authoritative copy of every world is always the Slime
 * bytes in storage; this cache only ever mirrors it, never diverges from it.
 * <p>
 * <b>On-disk layout</b>: which path a chunk is written to/read from depends on the platform,
 * confirmed empirically (not documented anywhere available at design time) rather than assumed to
 * be uniform - see {@link #regionFolder}/{@link #entitiesFolder}. Paper relocates a classic
 * {@code <world>/region}, {@code <world>/entities} layout into its own consolidated folder
 * structure during {@code Bukkit.createWorld()} (confirmed by actually running it - the data ends
 * up directly inside the relocated folder, no further nesting), so writing/reading that classic
 * layout is correct there. Plain Spigot does neither: nothing gets relocated, and - confirmed by
 * inspecting a world Spigot generated natively, with no involvement from this bridge - a
 * NORMAL-environment world's own data instead lives at the vanilla-native
 * {@code dimensions/minecraft/overworld} path from the start. Writing to the classic path on
 * Spigot leaves it orphaned and silently ignored: {@code Bukkit.createWorld()} finds nothing at
 * the path it actually reads from and generates fresh (empty) content there instead - a real,
 * live-server-observed bug (an admin's built theme content silently not making it into any pocket
 * world created from it), not a hypothetical. Reading a *live* world back out uses
 * {@link World#getWorldFolder()} directly, which reliably points at wherever the world's own root
 * actually is regardless of platform - Bukkit already abstracts this away once the world exists;
 * {@link #regionFolder}/{@link #entitiesFolder} still need applying to that root, the same as when
 * writing, since the platform split is about where *within* that root the data lives.
 * <p>
 * This also means the warm-cache check below - which uses {@link #regionFolder} against the
 * classic pre-migration root - safely and automatically declines to trust the cache on a version/
 * platform combination where migration relocates the data out from under it, rather than needing
 * an explicit per-version branch.
 * <p>
 * <b>Spawn pre-seeding</b>: {@link #activate} writes {@code level.dat} with a known spawn via
 * {@link LevelDatWriter} before ever calling {@link Bukkit#createWorld}, so vanilla's own "find a
 * valid spawn" search never runs at all - confirmed empirically to be the dominant cost of creating
 * a brand-new world (several seconds, since that search behaves pathologically against a fully void
 * generator with no solid ground anywhere), collapsing it to well under a second. See
 * docs/ARCHITECTURE.md §20 and {@link com.pocketworld.plugin.util.ChunkPrewarmer}, which shaves the
 * small remaining first-chunk-touch cost further on Paper.
 */
public final class AnvilShadowBridge implements WorldRuntimeBridge {

    private static final long CACHE_TTL_MILLIS = 30L * 60L * 1000L; // 30 minutes of disuse

    private record CacheEntry(int dataVersion, long markedFreshAtMillis) {}

    private final Map<String, CacheEntry> warmCache = new ConcurrentHashMap<>();

    @Override
    public String name() {
        return "anvil-shadow";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public OptionalInt cachedDataVersion(String worldName) {
        pruneExpired();

        CacheEntry entry = warmCache.get(worldName);
        if (entry == null) {
            return OptionalInt.empty();
        }

        // Trust nothing blindly: if the folder this entry claims to describe isn't actually there
        // in the shape prepare()/activate() left it in, treat it as a miss and let the normal path
        // rebuild it - covers a version whose world-creation migration relocated the data elsewhere,
        // a folder removed externally, or any other state we didn't cause ourselves.
        Path regionFolder = regionFolder(Bukkit.getWorldContainer().toPath().resolve(worldName));
        if (!Files.isDirectory(regionFolder)) {
            warmCache.remove(worldName);
            return OptionalInt.empty();
        }

        return OptionalInt.of(entry.dataVersion());
    }

    @Override
    public void prepare(SlimeWorldData data, String worldName) throws IOException {
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

        AnvilWorldWriter.writeAll(regionFolder(worldFolder), regionChunks);
        AnvilWorldWriter.writeAll(entitiesFolder(worldFolder), entityChunks);
    }

    /** See the class doc's "On-disk layout" section - Paper wants the classic path (which it then
     *  relocates itself), plain Spigot wants the vanilla-native nested path directly. Reuses
     *  {@link ChunkPrewarmer}'s existing, already-proven Paper-presence check rather than adding a
     *  second, separate detection mechanism. */
    private static Path regionFolder(Path worldFolder) {
        return ChunkPrewarmer.isAsyncAvailable()
                ? worldFolder.resolve("region")
                : worldFolder.resolve("dimensions").resolve("minecraft").resolve("overworld").resolve("region");
    }

    private static Path entitiesFolder(Path worldFolder) {
        return ChunkPrewarmer.isAsyncAvailable()
                ? worldFolder.resolve("entities")
                : worldFolder.resolve("dimensions").resolve("minecraft").resolve("overworld").resolve("entities");
    }

    @Override
    public World activate(String worldName, int dataVersion, WorldProperties properties) throws IOException {
        Path worldFolder = Bukkit.getWorldContainer().toPath().resolve(worldName);
        LevelDatWriter.write(worldFolder, worldName, dataVersion, properties);

        WorldCreator creator = new WorldCreator(worldName)
                .environment(World.Environment.NORMAL)
                .generateStructures(false)
                // A world border alone does not stop chunk generation, only entities crossing it -
                // anything that touches a chunk outside what prepare() actually wrote (the vanilla
                // spawn-safety search in particular, which can probe outward independently of the
                // border) would otherwise fall through to the default overworld generator and paste
                // in real, random vanilla terrain having nothing to do with the stored world.
                // Confirmed by a real in-game report of exactly that. VoidGenerator makes any chunk
                // not already on disk come out as plain empty air instead.
                .generator(new VoidGenerator());
        World world = Bukkit.createWorld(creator);
        if (world == null) {
            throw new IOException("Bukkit refused to create/load world \"" + worldName + "\"");
        }

        // Pocket worlds are small and bounded by their own world border; forcing vanilla's ~11x11
        // chunk spawn-keep-alive area regardless of that size would mean most (or all) of a small
        // world's chunks stay permanently loaded and ticking even with no one nearby. Loading lazily
        // as players actually walk in is the right shape for a small instanced world, not a
        // main-world-style always-on spawn. setKeepSpawnInMemory is plain Bukkit/Spigot World API
        // (unlike WorldCreator#keepSpawnLoaded, which is Paper-only), so this has to be set after
        // creation rather than as part of the WorldCreator chain.
        world.setKeepSpawnInMemory(false);
        world.setSpawnLocation((int) Math.floor(properties.spawnX()), (int) Math.floor(properties.spawnY()),
                (int) Math.floor(properties.spawnZ()), properties.spawnYaw());
        world.setDifficulty(parseDifficulty(properties.difficulty()));
        world.setPVP(properties.pvp());

        return world;
    }

    @Override
    public SlimeWorldData extractUnloaded(Path worldFolder, ChunkBounds bounds) throws IOException {
        Map<ChunkPos, CompoundBinaryTag> regionChunks = AnvilWorldReader.readAll(regionFolder(worldFolder));
        Map<ChunkPos, CompoundBinaryTag> entityChunks = AnvilWorldReader.readAll(entitiesFolder(worldFolder));

        List<SlimeChunkData> chunks = new ArrayList<>();
        for (Map.Entry<ChunkPos, CompoundBinaryTag> entry : regionChunks.entrySet()) {
            ChunkPos pos = entry.getKey();
            // Paper's own chunk-generation pipeline can touch (and persist) real chunks outside the
            // border on a brand-new world's very first chunk load, independent of anything this
            // bridge does - confirmed empirically. The border already stops players from ever
            // reaching those chunks, so they're dead weight; drop them here rather than paying
            // decode/encode cost for them on every future load. See ChunkBounds.
            if (!bounds.contains(pos.x(), pos.z())) {
                continue;
            }
            CompoundBinaryTag entities = entityChunks.get(pos);
            chunks.add(AnvilChunkConverter.toSlimeChunk(pos.x(), pos.z(), entry.getValue(), entities));
        }

        int dataVersion = Bukkit.getUnsafe().getDataVersion();
        return new SlimeWorldData(dataVersion, EnumSet.of(SlimeWorldFlag.BLOCK_TICKS, SlimeWorldFlag.FLUID_TICKS),
                chunks, CompoundBinaryTag.empty());
    }

    @Override
    public void afterUnload(String worldName, Path worldFolder, boolean retain, int dataVersion) throws IOException {
        Path classicFolder = Bukkit.getWorldContainer().toPath().resolve(worldName);
        boolean migratedElsewhere = !classicFolder.equals(worldFolder);

        // The warm-cache check only ever looks at the classic pre-migration path, so retaining is
        // only meaningful (and safe) when the live world's data actually still lives there - i.e.
        // this server/version didn't relocate it on activate(). If it did (confirmed empirically:
        // Paper 26.2's LegacyCraftBukkitWorldMigration moves it into the primary world's own
        // dimensions folder), keeping that migrated copy around would do nothing useful (our own
        // freshness check would never trust it) AND actively breaks the next load: prepare() would
        // write fresh classic-layout files again, and activate()'s migration would then fail because
        // its destination already exists from last time. So on a version where migration happened,
        // always fully clean up regardless of retain - there is nothing safe to keep.
        if (retain && !migratedElsewhere) {
            warmCache.put(worldName, new CacheEntry(dataVersion, System.currentTimeMillis()));
            return;
        }

        warmCache.remove(worldName);
        deleteRecursively(worldFolder);
        if (migratedElsewhere) {
            deleteRecursively(classicFolder);
        }
    }

    @Override
    public void evictCache(String worldName) throws IOException {
        warmCache.remove(worldName);
        deleteRecursively(Bukkit.getWorldContainer().toPath().resolve(worldName));
    }

    /** Opportunistic cleanup piggybacked on cache checks - no dedicated scheduled task needed. */
    private void pruneExpired() {
        long cutoff = System.currentTimeMillis() - CACHE_TTL_MILLIS;
        for (Map.Entry<String, CacheEntry> entry : warmCache.entrySet()) {
            if (entry.getValue().markedFreshAtMillis() < cutoff) {
                warmCache.remove(entry.getKey());
                try {
                    deleteRecursively(Bukkit.getWorldContainer().toPath().resolve(entry.getKey()));
                } catch (IOException ignored) {
                    // Best-effort - it'll be retried (or just left, harmlessly, for manual cleanup)
                    // next time this world's cache is looked at or another entry expires.
                }
            }
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
