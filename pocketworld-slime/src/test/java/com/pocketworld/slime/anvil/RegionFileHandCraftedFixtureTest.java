package com.pocketworld.slime.anvil;

import com.pocketworld.slime.nbt.NbtIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Builds a region file byte-for-byte by hand, straight from the documented format layout, rather
 * than via {@link RegionFile}'s own writer - this catches bugs the round-trip tests in
 * {@link RegionFileTest} structurally can't, since those exercise the reader and writer together
 * and would happily agree on a shared misunderstanding of the spec.
 */
class RegionFileHandCraftedFixtureTest {

    @Test
    void readsAChunkFromAManuallyAssembledRegionFile(@TempDir Path dir) throws Exception {
        CompoundBinaryTag tag = CompoundBinaryTag.builder().putString("greeting", "hand-crafted").putInt("answer", 42).build();

        // Compress the payload independently of RegionFile, using the same primitives (Deflater + our
        // NbtIO) but assembling the container format entirely by hand below.
        ByteArrayOutputStream compressedBuffer = new ByteArrayOutputStream();
        Deflater deflater = new Deflater();
        try (DeflaterOutputStream out = new DeflaterOutputStream(compressedBuffer, deflater)) {
            NbtIO.write(new DataOutputStream(out), tag);
        } finally {
            deflater.end();
        }
        byte[] compressed = compressedBuffer.toByteArray();

        // Chunk lives at local (1, 0) => header index 1, one sector at file offset 2 (i.e. sector 2,
        // right after the two 4096-byte header sectors).
        int chunkIndex = 1;
        int sectorOffset = 2;
        int sectorCount = 1; // 4 (length) + 1 (type) + compressed.length must fit in one 4096-byte sector

        ByteBuffer file = ByteBuffer.allocate((sectorOffset + sectorCount) * 4096);
        file.putInt(chunkIndex * 4, (sectorOffset << 8) | sectorCount); // location table entry
        file.putInt(4096 + chunkIndex * 4, 1_700_000_000);              // timestamp table entry (arbitrary)

        file.position(sectorOffset * 4096);
        file.putInt(1 + compressed.length); // length field: type byte + payload
        file.put((byte) AnvilCompression.ZLIB.id());
        file.put(compressed);

        Path regionFile = dir.resolve("r.0.0.mca");
        Files.write(regionFile, file.array());

        try (RegionFile region = RegionFile.open(regionFile, 0, 0)) {
            assertEquals(true, region.hasChunk(1, 0));
            assertEquals(tag, region.readChunk(1, 0));
        }
    }
}
