package com.pocketworld.slime.nbt;

import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Reads and writes NBT compounds the way the Slime format embeds them: full tag encoding (type
 * byte + name + payload, name conventionally empty), matching "same format as mc" fields exactly.
 * <p>
 * Some fields are self-terminating (no length prefix, relying on NBT's own structure to know where
 * it ends) and some are prefixed with a 4-byte big-endian length so they can be skipped without
 * parsing; both forms appear in the Slime format, so both are provided here.
 */
public final class NbtIO {

    private NbtIO() {}

    public static CompoundBinaryTag read(DataInput in) throws IOException {
        // BinaryTagIO.reader() caps compound size (~128KB) - too small for real chunk/entity NBT,
        // which is legitimately larger. This is trusted local disk data, not untrusted network input,
        // so there's no reason to keep that cap.
        return BinaryTagIO.unlimitedReader().read(in);
    }

    public static void write(DataOutput out, CompoundBinaryTag tag) throws IOException {
        BinaryTagIO.writer().write(tag, out);
    }

    public static CompoundBinaryTag readLengthPrefixed(DataInput in) throws IOException {
        int length = in.readInt();
        if (length < 0) {
            throw new IOException("Negative NBT compound length: " + length);
        }
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return read(new DataInputStream(new ByteArrayInputStream(bytes)));
    }

    public static void writeLengthPrefixed(DataOutput out, CompoundBinaryTag tag) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        write(new DataOutputStream(buffer), tag);
        byte[] bytes = buffer.toByteArray();
        out.writeInt(bytes.length);
        out.write(bytes);
    }
}
