package com.pocketworld.slime.anvil;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionFileTest {

    @Test
    void missingChunkReadsAsNull(@TempDir Path dir) throws Exception {
        try (RegionFile region = RegionFile.open(dir.resolve("r.0.0.mca"), 0, 0)) {
            assertFalse(region.hasChunk(5, 5));
            assertNull(region.readChunk(5, 5));
        }
    }

    @Test
    void roundTripsASingleChunk(@TempDir Path dir) throws Exception {
        CompoundBinaryTag tag = CompoundBinaryTag.builder().putString("hello", "world").putInt("n", 42).build();
        try (RegionFile region = RegionFile.open(dir.resolve("r.0.0.mca"), 0, 0)) {
            region.writeChunk(3, 7, tag);
        }
        try (RegionFile region = RegionFile.open(dir.resolve("r.0.0.mca"), 0, 0)) {
            assertTrue(region.hasChunk(3, 7));
            assertEquals(tag, region.readChunk(3, 7));
            assertFalse(region.hasChunk(3, 8));
        }
    }

    @Test
    void roundTripsEveryChunkSlotInARegion(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("r.2.-3.mca");
        try (RegionFile region = RegionFile.open(file, 2, -3)) {
            for (int z = 0; z < 32; z++) {
                for (int x = 0; x < 32; x++) {
                    region.writeChunk(x, z, tagFor(x, z));
                }
            }
        }
        try (RegionFile region = RegionFile.open(file, 2, -3)) {
            for (int z = 0; z < 32; z++) {
                for (int x = 0; x < 32; x++) {
                    assertEquals(tagFor(x, z), region.readChunk(x, z));
                }
            }
        }
    }

    @Test
    void reusesSectorsInPlaceWhenNewDataFits(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("r.0.0.mca");
        CompoundBinaryTag big = CompoundBinaryTag.builder().putByteArray("data", randomBytes(50_000)).build();
        CompoundBinaryTag small = CompoundBinaryTag.builder().putString("small", "x").build();

        long sizeAfterFirstWrite;
        try (RegionFile region = RegionFile.open(file, 0, 0)) {
            region.writeChunk(0, 0, big);
        }
        sizeAfterFirstWrite = Files.size(file);

        try (RegionFile region = RegionFile.open(file, 0, 0)) {
            region.writeChunk(0, 0, small);
        }
        // Overwriting with much smaller data must not grow the file (it should reuse the same sectors).
        assertEquals(sizeAfterFirstWrite, Files.size(file));

        try (RegionFile region = RegionFile.open(file, 0, 0)) {
            assertEquals(small, region.readChunk(0, 0));
        }
    }

    @Test
    void promotesOversizedChunksToAnExternalMccFile(@TempDir Path dir) throws Exception {
        // Compressed size must exceed 255 sectors (~1MB); random bytes barely compress, so this reliably
        // forces external-file promotion without needing a truly enormous fixture.
        byte[] incompressible = randomBytes(1_200_000);
        CompoundBinaryTag huge = CompoundBinaryTag.builder().putByteArray("payload", incompressible).build();

        Path file = dir.resolve("r.0.0.mca");
        try (RegionFile region = RegionFile.open(file, 0, 0)) {
            region.writeChunk(10, 10, huge);
        }

        assertTrue(Files.exists(dir.resolve("c.10.10.mcc")), "expected an external .mcc file for an oversized chunk");

        try (RegionFile region = RegionFile.open(file, 0, 0)) {
            assertEquals(huge, region.readChunk(10, 10));
        }
    }

    @Test
    void externalFileIsRemovedWhenChunkShrinksBackInline(@TempDir Path dir) throws Exception {
        byte[] incompressible = randomBytes(1_200_000);
        CompoundBinaryTag huge = CompoundBinaryTag.builder().putByteArray("payload", incompressible).build();
        CompoundBinaryTag small = CompoundBinaryTag.builder().putString("small", "x").build();

        Path file = dir.resolve("r.0.0.mca");
        try (RegionFile region = RegionFile.open(file, 0, 0)) {
            region.writeChunk(1, 1, huge);
        }
        assertTrue(Files.exists(dir.resolve("c.1.1.mcc")));

        try (RegionFile region = RegionFile.open(file, 0, 0)) {
            region.writeChunk(1, 1, small);
        }
        assertFalse(Files.exists(dir.resolve("c.1.1.mcc")));

        try (RegionFile region = RegionFile.open(file, 0, 0)) {
            assertEquals(small, region.readChunk(1, 1));
        }
    }

    @Test
    void rejectsOutOfRangeLocalCoordinates(@TempDir Path dir) throws Exception {
        try (RegionFile region = RegionFile.open(dir.resolve("r.0.0.mca"), 0, 0)) {
            assertThrows(IllegalArgumentException.class, () -> region.hasChunk(32, 0));
            assertThrows(IllegalArgumentException.class, () -> region.hasChunk(0, -1));
        }
    }

    @Test
    void rejectsCorruptedLengthField(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("r.0.0.mca");
        CompoundBinaryTag tag = CompoundBinaryTag.builder().putString("a", "b").build();
        try (RegionFile region = RegionFile.open(file, 0, 0)) {
            region.writeChunk(0, 0, tag);
        }

        // Corrupt the 4-byte length field of the chunk record (immediately after the 8KiB header).
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw")) {
            raf.seek(8192);
            raf.writeInt(Integer.MAX_VALUE); // absurd length, far beyond the sectors actually allocated
        }

        try (RegionFile region = RegionFile.open(file, 0, 0)) {
            assertThrows(CorruptedRegionFileException.class, () -> region.readChunk(0, 0));
        }
    }

    @Test
    void rejectsUnknownCompressionType(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("r.0.0.mca");
        CompoundBinaryTag tag = CompoundBinaryTag.builder().putString("a", "b").build();
        try (RegionFile region = RegionFile.open(file, 0, 0)) {
            region.writeChunk(0, 0, tag);
        }

        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw")) {
            raf.seek(8192 + 4); // the compression-type byte, right after the 4-byte length
            raf.writeByte(9);   // not a real compression id
        }

        try (RegionFile region = RegionFile.open(file, 0, 0)) {
            assertThrows(CorruptedRegionFileException.class, () -> region.readChunk(0, 0));
        }
    }

    private static CompoundBinaryTag tagFor(int x, int z) {
        return CompoundBinaryTag.builder().putInt("x", x).putInt("z", z).build();
    }

    private static byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        new Random(7).nextBytes(bytes);
        return bytes;
    }
}
