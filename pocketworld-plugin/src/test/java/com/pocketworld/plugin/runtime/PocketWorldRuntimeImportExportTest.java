package com.pocketworld.plugin.runtime;

import com.pocketworld.plugin.runtime.bridge.ChunkBounds;
import com.pocketworld.plugin.runtime.bridge.WorldRuntimeBridge;
import com.pocketworld.slime.anvil.AnvilWorldReader;
import com.pocketworld.slime.anvil.AnvilWorldWriter;
import com.pocketworld.slime.anvil.ChunkPos;
import com.pocketworld.slime.model.SlimeWorldData;
import com.pocketworld.slime.storage.loader.file.FileWorldLoader;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.World;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link PocketWorldRuntime#importWorld}/{@link PocketWorldRuntime#exportWorld} are pure I/O
 * orchestration around already-tested pieces (AnvilWorldReader/Writer, AnvilChunkConverter,
 * SlimeWriter/Reader) - this checks the wiring itself (folder <-> storage key, dataVersion) via a
 * real import-then-export round trip, not the conversion logic those pieces already cover.
 */
class PocketWorldRuntimeImportExportTest {

    private static final int DATA_VERSION = 4189;

    @Test
    void importThenExportRoundTripsTheSameChunks(@TempDir Path dir) throws IOException {
        Path sourceFolder = dir.resolve("source-anvil-world");
        Map<ChunkPos, CompoundBinaryTag> chunks = new LinkedHashMap<>();
        chunks.put(new ChunkPos(0, 0), CompoundBinaryTag.empty());
        chunks.put(new ChunkPos(-1, 3), CompoundBinaryTag.empty());
        AnvilWorldWriter.writeAll(sourceFolder.resolve("region"), chunks);

        PocketWorldRuntime runtime = new PocketWorldRuntime(new FileWorldLoader(dir.resolve("storage")), throwingBridge());
        runtime.importWorld(sourceFolder, "imported-world", DATA_VERSION);

        assertTrue(runtime.exists("imported-world"));
        runtime.validate("imported-world"); // must decode cleanly - throws on any structural problem

        Path exportedFolder = dir.resolve("exported-anvil-world");
        runtime.exportWorld("imported-world", exportedFolder);

        Map<ChunkPos, CompoundBinaryTag> exported = AnvilWorldReader.readAll(exportedFolder.resolve("region"));
        assertEquals(chunks.keySet(), exported.keySet());
    }

    private static WorldRuntimeBridge throwingBridge() {
        return new WorldRuntimeBridge() {
            @Override
            public String name() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isAvailable() {
                throw new UnsupportedOperationException();
            }

            @Override
            public OptionalInt cachedDataVersion(String worldName) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void prepare(SlimeWorldData data, String worldName) {
                throw new UnsupportedOperationException();
            }

            @Override
            public World activate(String worldName, int dataVersion, WorldProperties properties) {
                throw new UnsupportedOperationException();
            }

            @Override
            public SlimeWorldData extractUnloaded(Path worldFolder, ChunkBounds bounds) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void afterUnload(String worldName, Path worldFolder, boolean retain, int dataVersion) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void evictCache(String worldName) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
