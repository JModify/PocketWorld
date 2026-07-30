package com.pocketworld.slime.format;

import com.pocketworld.slime.compression.SlimeCompression;
import com.pocketworld.slime.model.SlimeChunkData;
import com.pocketworld.slime.model.SlimeChunkSection;
import com.pocketworld.slime.model.SlimeWorldData;
import com.pocketworld.slime.model.SlimeWorldFlag;
import com.pocketworld.slime.nbt.NbtIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads Slime Region Format world data. Only {@link SlimeConstants#CURRENT_VERSION} is decoded;
 * see {@link UnsupportedSlimeVersionException}'s javadoc for why earlier versions aren't yet.
 */
public final class SlimeReader {

    private static final int KNOWN_FLAGS_MASK =
            SlimeWorldFlag.POI_CHUNKS.bit() | SlimeWorldFlag.BLOCK_TICKS.bit() | SlimeWorldFlag.FLUID_TICKS.bit();

    private SlimeReader() {}

    public static SlimeWorldData read(InputStream input) throws IOException {
        DataInputStream in = asDataInputStream(input);

        int magic = in.readUnsignedShort();
        if (magic != SlimeConstants.MAGIC) {
            throw new CorruptedSlimeFileException(
                    "Not a Slime file: expected magic 0x" + Integer.toHexString(SlimeConstants.MAGIC).toUpperCase()
                            + " but found 0x" + Integer.toHexString(magic).toUpperCase());
        }

        int version = in.readUnsignedByte();
        if (version != SlimeConstants.CURRENT_VERSION) {
            throw new UnsupportedSlimeVersionException(version,
                    "Slime file version " + version + " is not supported for reading. Only version "
                            + SlimeConstants.CURRENT_VERSION + " (current) is implemented; legacy read support "
                            + "is a planned follow-up.");
        }

        int dataVersion = in.readInt();
        int flagsByte = in.readUnsignedByte();
        Set<SlimeWorldFlag> flags = decodeKnownFlags(flagsByte);
        int unknownFlagsMask = flagsByte & ~KNOWN_FLAGS_MASK;

        byte[] chunkBytes = readCompressedBlock(in, "chunk data");
        List<SlimeChunkData> chunks = readChunks(chunkBytes, flags, unknownFlagsMask);

        byte[] extraBytes = readCompressedBlock(in, "world extra data");
        CompoundBinaryTag extra = NbtIO.read(asDataInputStream(new ByteArrayInputStream(extraBytes)));

        return new SlimeWorldData(dataVersion, flags, chunks, extra);
    }

    private static DataInputStream asDataInputStream(InputStream input) {
        return input instanceof DataInputStream d ? d : new DataInputStream(input);
    }

    private static byte[] readCompressedBlock(DataInputStream in, String what) throws IOException {
        int compressedSize = in.readInt();
        int uncompressedSize = in.readInt();
        if (compressedSize < 0 || uncompressedSize < 0) {
            throw new CorruptedSlimeFileException(
                    "Negative size reading " + what + ": compressed=" + compressedSize + " uncompressed=" + uncompressedSize);
        }
        byte[] compressed = new byte[compressedSize];
        in.readFully(compressed);
        try {
            return SlimeCompression.decompress(compressed, uncompressedSize);
        } catch (RuntimeException e) {
            throw new CorruptedSlimeFileException("Failed to decompress " + what + ": " + e.getMessage(), e);
        }
    }

    private static Set<SlimeWorldFlag> decodeKnownFlags(int flagsByte) {
        Set<SlimeWorldFlag> flags = EnumSet.noneOf(SlimeWorldFlag.class);
        for (SlimeWorldFlag flag : SlimeWorldFlag.values()) {
            if ((flagsByte & flag.bit()) != 0) {
                flags.add(flag);
            }
        }
        return flags;
    }

    private static List<SlimeChunkData> readChunks(byte[] chunkBytes, Set<SlimeWorldFlag> flags, int unknownFlagsMask)
            throws IOException {
        List<SlimeChunkData> chunks = new ArrayList<>();
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(chunkBytes));
        while (true) {
            int x;
            try {
                x = in.readInt();
            } catch (EOFException endOfChunkArray) {
                break; // consumed exactly the declared uncompressed size; not a corruption
            }
            int z = in.readInt();
            chunks.add(readChunk(in, x, z, flags, unknownFlagsMask));
        }
        return chunks;
    }

    private static SlimeChunkData readChunk(DataInputStream in, int x, int z, Set<SlimeWorldFlag> flags, int unknownFlagsMask)
            throws IOException {
        int sectionCount = in.readInt();
        if (sectionCount < 0 || sectionCount > 64) {
            throw new CorruptedSlimeFileException(
                    "Implausible section count " + sectionCount + " for chunk (" + x + "," + z + ")");
        }
        List<SlimeChunkSection> sections = new ArrayList<>(sectionCount);
        for (int i = 0; i < sectionCount; i++) {
            sections.add(readSection(in));
        }

        CompoundBinaryTag heightmaps = NbtIO.readLengthPrefixed(in);

        CompoundBinaryTag poiChunk = flags.contains(SlimeWorldFlag.POI_CHUNKS) ? NbtIO.readLengthPrefixed(in) : null;
        CompoundBinaryTag blockTicks = flags.contains(SlimeWorldFlag.BLOCK_TICKS) ? NbtIO.readLengthPrefixed(in) : null;
        CompoundBinaryTag fluidTicks = flags.contains(SlimeWorldFlag.FLUID_TICKS) ? NbtIO.readLengthPrefixed(in) : null;

        Map<Integer, byte[]> unknownFlagData = readUnknownFlagData(in, unknownFlagsMask);

        CompoundBinaryTag tileEntities = NbtIO.readLengthPrefixed(in);
        CompoundBinaryTag entities = NbtIO.readLengthPrefixed(in);
        CompoundBinaryTag extra = NbtIO.read(in);

        return new SlimeChunkData(x, z, sections, heightmaps, poiChunk, blockTicks, fluidTicks,
                unknownFlagData, tileEntities, entities, extra);
    }

    private static SlimeChunkSection readSection(DataInputStream in) throws IOException {
        int sectionFlags = in.readUnsignedByte();
        boolean hasBlockLight = (sectionFlags & 0x1) != 0;
        boolean hasSkyLight = (sectionFlags & 0x2) != 0;

        byte[] skyLight = null;
        if (hasSkyLight) {
            skyLight = new byte[SlimeChunkSection.LIGHT_ARRAY_LENGTH];
            in.readFully(skyLight);
        }

        byte[] blockLight = null;
        if (hasBlockLight) {
            blockLight = new byte[SlimeChunkSection.LIGHT_ARRAY_LENGTH];
            in.readFully(blockLight);
        }

        CompoundBinaryTag blockStates = NbtIO.readLengthPrefixed(in);
        CompoundBinaryTag biomes = NbtIO.readLengthPrefixed(in);

        return new SlimeChunkSection(skyLight, blockLight, blockStates, biomes);
    }

    private static Map<Integer, byte[]> readUnknownFlagData(DataInputStream in, int unknownFlagsMask) throws IOException {
        if (unknownFlagsMask == 0) {
            return Map.of();
        }
        Map<Integer, byte[]> data = new LinkedHashMap<>();
        for (int bit = 0; bit < 8; bit++) {
            if ((unknownFlagsMask & (1 << bit)) != 0) {
                int length = in.readInt();
                byte[] bytes = new byte[length];
                in.readFully(bytes);
                data.put(bit, bytes);
            }
        }
        return data;
    }
}
