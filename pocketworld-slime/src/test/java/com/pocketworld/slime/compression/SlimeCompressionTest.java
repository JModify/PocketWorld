package com.pocketworld.slime.compression;

import com.github.luben.zstd.ZstdException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SlimeCompressionTest {

    @Test
    void roundTripsEmptyInput() {
        byte[] compressed = SlimeCompression.compress(new byte[0]);
        assertArrayEquals(new byte[0], SlimeCompression.decompress(compressed, 0));
    }

    @Test
    void roundTripsSmallInput() {
        byte[] data = "hello slime world".getBytes(StandardCharsets.UTF_8);
        byte[] compressed = SlimeCompression.compress(data);
        assertArrayEquals(data, SlimeCompression.decompress(compressed, data.length));
    }

    @Test
    void roundTripsLargeRandomInput() {
        byte[] data = new byte[256 * 1024];
        new Random(42).nextBytes(data);
        byte[] compressed = SlimeCompression.compress(data);
        assertArrayEquals(data, SlimeCompression.decompress(compressed, data.length));
    }

    @Test
    void rejectsMismatchedDeclaredSize() {
        byte[] data = "hello slime world".getBytes(StandardCharsets.UTF_8);
        byte[] compressed = SlimeCompression.compress(data);

        assertThrows(ZstdException.class, () -> SlimeCompression.decompress(compressed, data.length + 5));
    }
}
