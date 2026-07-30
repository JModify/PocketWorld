package com.pocketworld.slime.format;

import com.pocketworld.slime.compression.SlimeCompression;
import com.pocketworld.slime.model.SlimeChunkData;
import com.pocketworld.slime.model.SlimeChunkSection;
import com.pocketworld.slime.model.SlimeWorldData;
import com.pocketworld.slime.model.SlimeWorldFlag;
import com.pocketworld.slime.nbt.NbtIO;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Writes {@link SlimeWorldData} back out using the current format version's exact layout. */
public final class SlimeWriter {

    private static final byte[] EMPTY = new byte[0];

    private SlimeWriter() {}

    public static void write(SlimeWorldData world, OutputStream output) throws IOException {
        DataOutputStream out = output instanceof DataOutputStream d ? d : new DataOutputStream(output);

        out.writeShort(SlimeConstants.MAGIC);
        out.writeByte(SlimeConstants.CURRENT_VERSION);
        out.writeInt(world.dataVersion());

        Set<Integer> unknownBits = collectUnknownFlagBits(world.chunks());
        out.writeByte(encodeFlagsByte(world.flags(), unknownBits));

        writeCompressedBlock(out, writeChunksToBytes(world.chunks(), unknownBits));
        writeCompressedBlock(out, writeCompoundToBytes(world.extra()));

        out.flush();
    }

    private static Set<Integer> collectUnknownFlagBits(List<SlimeChunkData> chunks) {
        Set<Integer> bits = new TreeSet<>();
        for (SlimeChunkData chunk : chunks) {
            bits.addAll(chunk.unknownFlagData().keySet());
        }
        return bits;
    }

    private static int encodeFlagsByte(Set<SlimeWorldFlag> flags, Set<Integer> unknownBits) {
        int value = 0;
        for (SlimeWorldFlag flag : flags) {
            value |= flag.bit();
        }
        for (int bit : unknownBits) {
            value |= (1 << bit);
        }
        return value;
    }

    private static void writeCompressedBlock(DataOutputStream out, byte[] uncompressed) throws IOException {
        byte[] compressed = SlimeCompression.compress(uncompressed);
        out.writeInt(compressed.length);
        out.writeInt(uncompressed.length);
        out.write(compressed);
    }

    private static byte[] writeCompoundToBytes(net.kyori.adventure.nbt.CompoundBinaryTag tag) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        NbtIO.write(new DataOutputStream(buffer), tag);
        return buffer.toByteArray();
    }

    private static byte[] writeChunksToBytes(List<SlimeChunkData> chunks, Set<Integer> unknownBits) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(buffer);
        for (SlimeChunkData chunk : chunks) {
            writeChunk(out, chunk, unknownBits);
        }
        return buffer.toByteArray();
    }

    private static void writeChunk(DataOutputStream out, SlimeChunkData chunk, Set<Integer> unknownBits) throws IOException {
        out.writeInt(chunk.x());
        out.writeInt(chunk.z());
        out.writeInt(chunk.sections().size());
        for (SlimeChunkSection section : chunk.sections()) {
            writeSection(out, section);
        }

        NbtIO.writeLengthPrefixed(out, chunk.heightmaps());

        if (chunk.poiChunk() != null) {
            NbtIO.writeLengthPrefixed(out, chunk.poiChunk());
        }
        if (chunk.blockTicks() != null) {
            NbtIO.writeLengthPrefixed(out, chunk.blockTicks());
        }
        if (chunk.fluidTicks() != null) {
            NbtIO.writeLengthPrefixed(out, chunk.fluidTicks());
        }

        for (int bit : unknownBits) {
            byte[] data = chunk.unknownFlagData().getOrDefault(bit, EMPTY);
            out.writeInt(data.length);
            out.write(data);
        }

        NbtIO.writeLengthPrefixed(out, chunk.tileEntities());
        NbtIO.writeLengthPrefixed(out, chunk.entities());
        NbtIO.write(out, chunk.extra());
    }

    private static void writeSection(DataOutputStream out, SlimeChunkSection section) throws IOException {
        int flags = 0;
        if (section.hasBlockLight()) {
            flags |= 0x1;
        }
        if (section.hasSkyLight()) {
            flags |= 0x2;
        }
        out.writeByte(flags);

        if (section.hasSkyLight()) {
            out.write(section.skyLight());
        }
        if (section.hasBlockLight()) {
            out.write(section.blockLight());
        }

        NbtIO.writeLengthPrefixed(out, section.blockStates());
        NbtIO.writeLengthPrefixed(out, section.biomes());
    }
}
