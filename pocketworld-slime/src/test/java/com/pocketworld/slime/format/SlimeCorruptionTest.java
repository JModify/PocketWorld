package com.pocketworld.slime.format;

import com.pocketworld.slime.model.SlimeWorldFlag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Slime files are read from disk and can be truncated, bit-flipped, or otherwise damaged. All of
 * these must fail with a clear, typed exception rather than crash unpredictably or silently
 * misinterpret the data.
 */
class SlimeCorruptionTest {

    private static final int HEADER_SIZE_BEFORE_COMPRESSED_CHUNKS = 2 + 1 + 4 + 1 + 4 + 4; // magic+version+dataVersion+flags+sizes

    private static byte[] validFile() throws Exception {
        return SlimeRoundTripTest.encode(SlimeTestFixtures.world(EnumSet.allOf(SlimeWorldFlag.class), 3, 2));
    }

    @Test
    void rejectsBadMagic() throws Exception {
        byte[] bytes = validFile();
        bytes[0] = 0x00;
        bytes[1] = 0x00;

        CorruptedSlimeFileException e = assertThrows(CorruptedSlimeFileException.class,
                () -> SlimeReader.read(new ByteArrayInputStream(bytes)));
        assertEquals(true, e.getMessage().contains("magic"));
    }

    @Test
    void rejectsUnsupportedVersion() throws Exception {
        byte[] bytes = validFile();
        bytes[2] = 7; // a real historical Slime version, just not one we implement reading for

        UnsupportedSlimeVersionException e = assertThrows(UnsupportedSlimeVersionException.class,
                () -> SlimeReader.read(new ByteArrayInputStream(bytes)));
        assertEquals(7, e.version());
    }

    @Test
    void rejectsTruncatedHeader() throws Exception {
        byte[] bytes = validFile();
        byte[] truncated = new byte[5]; // cuts off mid-header, well before any compressed data
        System.arraycopy(bytes, 0, truncated, 0, truncated.length);

        assertThrows(EOFException.class, () -> SlimeReader.read(new ByteArrayInputStream(truncated)));
    }

    @Test
    void rejectsTruncatedChunkBlock() throws Exception {
        byte[] bytes = validFile();
        byte[] truncated = new byte[HEADER_SIZE_BEFORE_COMPRESSED_CHUNKS + 3]; // header intact, compressed block cut short
        System.arraycopy(bytes, 0, truncated, 0, truncated.length);

        assertThrows(EOFException.class, () -> SlimeReader.read(new ByteArrayInputStream(truncated)));
    }

    @Test
    void rejectsCorruptedCompressedChunkData() throws Exception {
        byte[] bytes = validFile();
        // Flip a byte inside the zstd frame itself (just past the compressed/uncompressed size ints),
        // which should break Zstd's frame checksum/structure rather than merely produce different
        // (but validly-decodable) chunk bytes.
        int offset = HEADER_SIZE_BEFORE_COMPRESSED_CHUNKS + 4;
        bytes[offset] = (byte) ~bytes[offset];

        assertThrows(CorruptedSlimeFileException.class, () -> SlimeReader.read(new ByteArrayInputStream(bytes)));
    }

    @Test
    void rejectsNegativeDeclaredSize() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(buffer);
        out.writeShort(SlimeConstants.MAGIC);
        out.writeByte(SlimeConstants.CURRENT_VERSION);
        out.writeInt(4189);
        out.writeByte(0);
        out.writeInt(-1); // compressed size
        out.writeInt(0);  // uncompressed size

        IOException e = assertThrows(IOException.class,
                () -> SlimeReader.read(new ByteArrayInputStream(buffer.toByteArray())));
        assertEquals(CorruptedSlimeFileException.class, e.getClass());
    }

    @Test
    void constructorRejectsInconsistentFlags() {
        // A chunk that carries POI data but whose world doesn't declare the POI_CHUNKS flag is an
        // invalid model, not just an invalid file - this should be caught before it's ever written.
        var poiChunk = new com.pocketworld.slime.model.SlimeChunkData(
                0, 0, java.util.List.of(),
                SlimeTestFixtures.compound("heightmaps"),
                SlimeTestFixtures.compound("poi"), null, null,
                java.util.Map.of(),
                SlimeTestFixtures.compound("tileEntities"),
                SlimeTestFixtures.compound("entities"),
                CompoundBinaryTag.empty());

        assertThrows(IllegalArgumentException.class, () -> new com.pocketworld.slime.model.SlimeWorldData(
                4189, java.util.Set.of(), java.util.List.of(poiChunk), CompoundBinaryTag.empty()));
    }
}
