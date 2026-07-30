package com.pocketworld.slime.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Shared behavioural contract every {@link WorldLoader} implementation must satisfy, run against
 * each concrete backend so they can't silently drift from each other. Deliberately named without a
 * "Test"/"Tests" suffix so Surefire's default include patterns don't try to run this abstract class
 * directly - only the concrete per-backend subclasses should execute it.
 */
public abstract class WorldLoaderContract {

    protected WorldLoader loader;

    protected abstract WorldLoader createLoader() throws Exception;

    @BeforeEach
    void setUp() throws Exception {
        loader = createLoader();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (loader instanceof AutoCloseable closeable) {
            closeable.close();
        }
    }

    @Test
    void unknownWorldDoesNotExist() throws IOException {
        assertFalse(loader.exists("does-not-exist"));
    }

    @Test
    void readingAnUnknownWorldThrows() {
        assertThrows(UnknownWorldException.class, () -> loader.read("does-not-exist"));
    }

    @Test
    void deletingAnUnknownWorldThrows() {
        assertThrows(UnknownWorldException.class, () -> loader.delete("does-not-exist"));
    }

    @Test
    void writtenWorldExistsAndReadsBackIdentically() throws IOException {
        byte[] data = "hello slime world".getBytes(StandardCharsets.UTF_8);
        loader.write("world-a", data);

        assertTrue(loader.exists("world-a"));
        assertArrayEquals(data, loader.read("world-a"));
    }

    @Test
    void writingTwiceOverwritesRatherThanDuplicating() throws IOException {
        loader.write("world-a", "first".getBytes(StandardCharsets.UTF_8));
        loader.write("world-a", "second".getBytes(StandardCharsets.UTF_8));

        assertArrayEquals("second".getBytes(StandardCharsets.UTF_8), loader.read("world-a"));
        assertEquals(1, loader.list().stream().filter("world-a"::equals).count());
    }

    @Test
    void deletedWorldNoLongerExists() throws IOException {
        loader.write("world-a", "data".getBytes(StandardCharsets.UTF_8));
        loader.delete("world-a");

        assertFalse(loader.exists("world-a"));
        assertThrows(UnknownWorldException.class, () -> loader.read("world-a"));
    }

    @Test
    void listReflectsWrittenAndDeletedWorlds() throws IOException {
        assertTrue(loader.list().isEmpty());

        loader.write("alpha", "a".getBytes(StandardCharsets.UTF_8));
        loader.write("beta", "b".getBytes(StandardCharsets.UTF_8));

        List<String> ids = loader.list();
        assertEquals(2, ids.size());
        assertTrue(ids.contains("alpha"));
        assertTrue(ids.contains("beta"));

        loader.delete("alpha");
        assertEquals(List.of("beta"), loader.list());
    }

    @Test
    void cloneWorldCopiesDataUnderNewIdWithoutTouchingTheSource() throws IOException {
        byte[] data = "clone me".getBytes(StandardCharsets.UTF_8);
        loader.write("source", data);

        loader.cloneWorld("source", loader, "target");

        assertTrue(loader.exists("target"));
        assertArrayEquals(data, loader.read("target"));
        assertArrayEquals(data, loader.read("source"));
    }

    @Test
    void cloneWorldRefusesToOverwriteAnExistingTarget() throws IOException {
        loader.write("source", "a".getBytes(StandardCharsets.UTF_8));
        loader.write("target", "b".getBytes(StandardCharsets.UTF_8));

        assertThrows(WorldAlreadyExistsException.class, () -> loader.cloneWorld("source", loader, "target"));
    }

    @Test
    void handlesABinaryPayloadThatIsNotValidText() throws IOException {
        byte[] data = new byte[512];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        loader.write("binary", data);
        assertArrayEquals(data, loader.read("binary"));
    }
}
