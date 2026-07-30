package com.pocketworld.slime.compression;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdException;

/**
 * Zstd compression, the codec the Slime format specifies for its two compressed blocks (chunk
 * data and world-level "extra" data). Both directions operate on whole in-memory buffers, matching
 * how the format stores an explicit compressed/uncompressed size pair rather than a stream.
 */
public final class SlimeCompression {

    private SlimeCompression() {}

    public static byte[] compress(byte[] data) {
        return Zstd.compress(data);
    }

    /**
     * @param uncompressedSize the exact decompressed size, as recorded in the Slime file header.
     */
    public static byte[] decompress(byte[] compressed, int uncompressedSize) {
        if (uncompressedSize < 0) {
            throw new IllegalArgumentException("uncompressedSize must be >= 0, got " + uncompressedSize);
        }
        if (uncompressedSize == 0) {
            return new byte[0];
        }
        byte[] result = Zstd.decompress(compressed, uncompressedSize);
        long actualSize = Zstd.getFrameContentSize(compressed);
        if (actualSize >= 0 && actualSize != uncompressedSize) {
            throw new ZstdException(0,
                    "Declared uncompressed size (" + uncompressedSize + ") does not match the frame's actual "
                            + "content size (" + actualSize + ")");
        }
        return result;
    }
}
