package com.pocketworld.slime.anvil;

import com.pocketworld.slime.nbt.NbtIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipException;

/**
 * One vanilla Anvil region file (.mca): a 32x32 grid of chunks, each independently compressed and
 * stored in whole 4096-byte sectors after an 8KiB header (a chunk-location table and a
 * last-modified timestamp table). Chunks whose compressed size would need more than 255 sectors
 * (~1MB) are promoted to a sibling {@code c.<x>.<z>.mcc} file, per the format's external-chunk
 * mechanism.
 * <p>
 * This class only moves opaque NBT compounds in and out of the container - it has no notion of
 * what a chunk's fields mean, so it works unchanged for {@code region/}, {@code entities/}, and
 * {@code poi/} directories alike.
 * <p>
 * Space is reused in place when a rewritten chunk still fits its previous allocation; otherwise
 * new sectors are appended at the end of the file. Sectors freed by a chunk shrinking are not
 * reclaimed for other chunks - this matches vanilla's own well-documented tendency for region
 * files to accumulate unused space over time, which is one of the reasons the Slime format exists.
 */
public final class RegionFile implements AutoCloseable {

    private static final int SECTOR_SIZE = 4096;
    private static final int HEADER_SECTORS = 2;
    private static final int MAX_INLINE_SECTORS = 0xFF;

    private final FileChannel channel;
    private final Path directory;
    private final int regionX;
    private final int regionZ;
    private final int[] sectorOffset = new int[1024];
    private final int[] sectorCount = new int[1024];
    private final int[] timestamp = new int[1024];
    private int fileSectorCount;

    private RegionFile(FileChannel channel, Path directory, int regionX, int regionZ) {
        this.channel = channel;
        this.directory = directory;
        this.regionX = regionX;
        this.regionZ = regionZ;
    }

    public static RegionFile open(Path file, int regionX, int regionZ) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        FileChannel channel = FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
        RegionFile region = new RegionFile(channel, file.toAbsolutePath().getParent(), regionX, regionZ);
        region.loadHeader();
        return region;
    }

    private void loadHeader() throws IOException {
        long size = channel.size();
        fileSectorCount = Math.max(HEADER_SECTORS, ceilDiv(size, SECTOR_SIZE));
        if (size < (long) HEADER_SECTORS * SECTOR_SIZE) {
            return; // fresh/empty file; header will be written on first writeChunk
        }

        ByteBuffer header = ByteBuffer.allocate(HEADER_SECTORS * SECTOR_SIZE);
        readFully(header, 0);
        for (int i = 0; i < 1024; i++) {
            int entry = header.getInt(i * 4);
            sectorOffset[i] = entry >>> 8;
            sectorCount[i] = entry & 0xFF;
        }
        for (int i = 0; i < 1024; i++) {
            timestamp[i] = header.getInt(SECTOR_SIZE + i * 4);
        }
    }

    public boolean hasChunk(int localX, int localZ) {
        return sectorCount[index(localX, localZ)] > 0;
    }

    public int timestamp(int localX, int localZ) {
        return timestamp[index(localX, localZ)];
    }

    /** Returns null if this chunk slot is empty. */
    public CompoundBinaryTag readChunk(int localX, int localZ) throws IOException {
        int index = index(localX, localZ);
        int offset = sectorOffset[index];
        int sectors = sectorCount[index];
        if (sectors == 0) {
            return null;
        }
        if (offset + sectors > fileSectorCount) {
            throw new CorruptedRegionFileException(
                    "Chunk (" + localX + "," + localZ + ") claims sectors [" + offset + "," + (offset + sectors)
                            + ") beyond the file's own allocated range (" + fileSectorCount + " sectors)");
        }

        ByteBuffer buffer = ByteBuffer.allocate(sectors * SECTOR_SIZE);
        readFully(buffer, offset * (long) SECTOR_SIZE);
        buffer.flip();

        try {
            int length = buffer.getInt();
            int typeByte = buffer.get() & 0xFF;
            boolean external = (typeByte & 0x80) != 0;
            AnvilCompression compression = AnvilCompression.fromId(typeByte & 0x7F);

            byte[] payload;
            if (external) {
                payload = Files.readAllBytes(externalFile(absoluteX(localX), absoluteZ(localZ)));
            } else {
                if (length < 1 || length - 1 > buffer.remaining()) {
                    throw new CorruptedRegionFileException(
                            "Chunk (" + localX + "," + localZ + ") declares length " + length
                                    + " but only " + (buffer.remaining() + 1) + " bytes are available in its sectors");
                }
                payload = new byte[length - 1];
                buffer.get(payload);
            }
            return decompressAndRead(payload, compression);
        } catch (BufferUnderflowException | IllegalArgumentException | ZipException e) {
            throw new CorruptedRegionFileException(
                    "Failed to read chunk (" + localX + "," + localZ + "): " + e.getMessage(), e);
        }
    }

    public void writeChunk(int localX, int localZ, CompoundBinaryTag tag) throws IOException {
        writeChunk(localX, localZ, tag, AnvilCompression.ZLIB);
    }

    public void writeChunk(int localX, int localZ, CompoundBinaryTag tag, AnvilCompression compression) throws IOException {
        byte[] compressed = compress(tag, compression);
        int index = index(localX, localZ);
        Path externalFile = externalFile(absoluteX(localX), absoluteZ(localZ));

        int inlineSectorsNeeded = ceilDiv(1L + compressed.length, SECTOR_SIZE); // +1 for the type byte
        ByteBuffer record;
        int sectorsToAllocate;

        if (inlineSectorsNeeded > MAX_INLINE_SECTORS) {
            Files.write(externalFile, compressed);
            record = ByteBuffer.allocate(SECTOR_SIZE);
            record.putInt(1);
            record.put((byte) (compression.id() | 0x80));
            sectorsToAllocate = 1;
        } else {
            Files.deleteIfExists(externalFile); // no longer external, if it used to be
            sectorsToAllocate = inlineSectorsNeeded;
            record = ByteBuffer.allocate(sectorsToAllocate * SECTOR_SIZE);
            record.putInt(1 + compressed.length);
            record.put((byte) compression.id());
            record.put(compressed);
        }
        record.clear(); // write the whole sector-padded buffer, including the zero-filled tail

        int offset = allocateSectors(index, sectorsToAllocate);
        writeFully(record, offset * (long) SECTOR_SIZE);

        sectorOffset[index] = offset;
        sectorCount[index] = sectorsToAllocate;
        timestamp[index] = (int) (System.currentTimeMillis() / 1000L);

        flushHeader();
    }

    private int allocateSectors(int index, int sectorsNeeded) {
        int existingCount = sectorCount[index];
        if (existingCount > 0 && sectorsNeeded <= existingCount) {
            return sectorOffset[index];
        }
        int offset = fileSectorCount;
        fileSectorCount += sectorsNeeded;
        return offset;
    }

    private void flushHeader() throws IOException {
        ByteBuffer header = ByteBuffer.allocate(HEADER_SECTORS * SECTOR_SIZE);
        for (int i = 0; i < 1024; i++) {
            header.putInt(i * 4, (sectorOffset[i] << 8) | (sectorCount[i] & 0xFF));
        }
        for (int i = 0; i < 1024; i++) {
            header.putInt(SECTOR_SIZE + i * 4, timestamp[i]);
        }
        writeFully(header, 0);
    }

    private Path externalFile(int absoluteX, int absoluteZ) {
        return directory.resolve("c." + absoluteX + "." + absoluteZ + ".mcc");
    }

    private int absoluteX(int localX) {
        return (regionX << 5) | localX;
    }

    private int absoluteZ(int localZ) {
        return (regionZ << 5) | localZ;
    }

    private static int index(int localX, int localZ) {
        if (localX < 0 || localX > 31 || localZ < 0 || localZ > 31) {
            throw new IllegalArgumentException("Local chunk coordinates must be 0-31, got (" + localX + "," + localZ + ")");
        }
        return localX + localZ * 32;
    }

    private static byte[] compress(CompoundBinaryTag tag, AnvilCompression compression) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        switch (compression) {
            case ZLIB -> {
                Deflater deflater = new Deflater();
                try (DeflaterOutputStream out = new DeflaterOutputStream(buffer, deflater)) {
                    NbtIO.write(new DataOutputStream(out), tag);
                } finally {
                    deflater.end();
                }
            }
            case GZIP -> {
                try (GZIPOutputStream out = new GZIPOutputStream(buffer)) {
                    NbtIO.write(new DataOutputStream(out), tag);
                }
            }
            case NONE -> NbtIO.write(new DataOutputStream(buffer), tag);
        }
        return buffer.toByteArray();
    }

    private static CompoundBinaryTag decompressAndRead(byte[] data, AnvilCompression compression) throws IOException {
        return switch (compression) {
            case ZLIB -> {
                Inflater inflater = new Inflater();
                try (InflaterInputStream in = new InflaterInputStream(new ByteArrayInputStream(data), inflater)) {
                    yield NbtIO.read(new DataInputStream(in));
                } finally {
                    inflater.end();
                }
            }
            case GZIP -> {
                try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(data))) {
                    yield NbtIO.read(new DataInputStream(in));
                }
            }
            case NONE -> NbtIO.read(new DataInputStream(new ByteArrayInputStream(data)));
        };
    }

    /** Fills the buffer from {@code position}, leaving it ready for absolute gets (position/limit untouched). */
    private void readFully(ByteBuffer buffer, long position) throws IOException {
        long pos = position;
        while (buffer.hasRemaining()) {
            int read = channel.read(buffer, pos);
            if (read < 0) {
                break; // short file; unread remainder is treated as zero (matches a freshly grown region)
            }
            pos += read;
        }
    }

    private void writeFully(ByteBuffer buffer, long position) throws IOException {
        long pos = position;
        while (buffer.hasRemaining()) {
            pos += channel.write(buffer, pos);
        }
    }

    private static int ceilDiv(long value, int divisor) {
        return (int) ((value + divisor - 1) / divisor);
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}
