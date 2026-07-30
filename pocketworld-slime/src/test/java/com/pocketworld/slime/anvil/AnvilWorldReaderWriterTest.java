package com.pocketworld.slime.anvil;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnvilWorldReaderWriterTest {

    @Test
    void writesAndReadsChunksAcrossMultipleRegions(@TempDir Path dir) throws Exception {
        Map<ChunkPos, CompoundBinaryTag> chunks = new LinkedHashMap<>();
        // Spans four different regions, including negative coordinates.
        for (int x = -40; x < 40; x += 17) {
            for (int z = -40; z < 40; z += 19) {
                chunks.put(new ChunkPos(x, z), CompoundBinaryTag.builder().putInt("x", x).putInt("z", z).build());
            }
        }

        AnvilWorldWriter.writeAll(dir, chunks);

        // x in [-40,28] step 17 spans regionX -2..0; z in [-40,36] step 19 spans regionZ -2..1.
        assertTrue(Files.exists(dir.resolve("r.-2.-2.mca")));
        assertTrue(Files.exists(dir.resolve("r.0.1.mca")));

        Map<ChunkPos, CompoundBinaryTag> read = AnvilWorldReader.readAll(dir);

        assertEquals(chunks, read);
    }

    @Test
    void readingAMissingDirectoryReturnsEmpty(@TempDir Path dir) throws Exception {
        Map<ChunkPos, CompoundBinaryTag> read = AnvilWorldReader.readAll(dir.resolve("does-not-exist"));
        assertTrue(read.isEmpty());
    }

    @Test
    void ignoresFilesThatArentRegionFiles(@TempDir Path dir) throws Exception {
        Files.createFile(dir.resolve("session.lock"));
        Files.createFile(dir.resolve("r.not-a-number.0.mca"));

        Map<ChunkPos, CompoundBinaryTag> read = AnvilWorldReader.readAll(dir);

        assertTrue(read.isEmpty());
    }
}
