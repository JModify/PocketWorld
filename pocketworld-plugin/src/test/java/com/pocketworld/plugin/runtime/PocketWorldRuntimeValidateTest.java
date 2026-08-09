package com.pocketworld.plugin.runtime;

import com.pocketworld.plugin.runtime.bridge.ChunkBounds;
import com.pocketworld.plugin.runtime.bridge.WorldRuntimeBridge;
import com.pocketworld.slime.format.SlimeFormatException;
import com.pocketworld.slime.model.SlimeWorldData;
import com.pocketworld.slime.model.SlimeWorldFlag;
import com.pocketworld.slime.storage.WorldLoader;
import com.pocketworld.slime.storage.loader.file.FileWorldLoader;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.World;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link PocketWorldRuntime#validate} and {@link PocketWorldRuntime#list} are pure storage-layer
 * operations - they never touch the runtime bridge or any live Bukkit state - so they're testable
 * with a plain {@link FileWorldLoader} and a bridge stub that's never actually invoked.
 */
class PocketWorldRuntimeValidateTest {

    @Test
    void validateSucceedsForAWellFormedStoredWorld(@TempDir Path dir) throws IOException {
        PocketWorldRuntime runtime = runtime(dir);
        runtime.persist("valid-world", emptyWorldData());

        assertDoesNotThrowIOException(() -> runtime.validate("valid-world"));
    }

    @Test
    void validateThrowsSlimeFormatExceptionForCorruptedBytes(@TempDir Path dir) throws IOException {
        WorldLoader storage = new FileWorldLoader(dir);
        storage.write("corrupt-world", new byte[]{0x00, 0x00, 0x00, 0x01, 0x02});
        PocketWorldRuntime runtime = new PocketWorldRuntime(storage, throwingBridge());

        assertThrows(SlimeFormatException.class, () -> runtime.validate("corrupt-world"));
    }

    @Test
    void listReturnsAllStoredWorldIds(@TempDir Path dir) throws IOException {
        PocketWorldRuntime runtime = runtime(dir);
        runtime.persist("world-a", emptyWorldData());
        runtime.persist("world-b", emptyWorldData());

        List<String> ids = runtime.list();

        assertEquals(2, ids.size());
        assertTrue(ids.contains("world-a"));
        assertTrue(ids.contains("world-b"));
    }

    private static PocketWorldRuntime runtime(Path dir) throws IOException {
        return new PocketWorldRuntime(new FileWorldLoader(dir), throwingBridge());
    }

    private static SlimeWorldData emptyWorldData() {
        return new SlimeWorldData(4189, EnumSet.noneOf(SlimeWorldFlag.class), List.of(), CompoundBinaryTag.empty());
    }

    private interface ThrowingIO {
        void run() throws IOException;
    }

    private static void assertDoesNotThrowIOException(ThrowingIO action) {
        try {
            action.run();
        } catch (IOException e) {
            throw new AssertionError("Expected no exception, but got: " + e, e);
        }
    }

    /** Never actually invoked by validate()/list() - every method throws if that assumption breaks. */
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
