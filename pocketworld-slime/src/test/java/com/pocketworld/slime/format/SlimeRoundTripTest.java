package com.pocketworld.slime.format;

import com.pocketworld.slime.model.SlimeChunkData;
import com.pocketworld.slime.model.SlimeWorldData;
import com.pocketworld.slime.model.SlimeWorldFlag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlimeRoundTripTest {

    private static Stream<Set<SlimeWorldFlag>> flagCombinations() {
        return Stream.of(
                Set.of(),
                EnumSet.of(SlimeWorldFlag.POI_CHUNKS),
                EnumSet.of(SlimeWorldFlag.BLOCK_TICKS),
                EnumSet.of(SlimeWorldFlag.FLUID_TICKS),
                EnumSet.allOf(SlimeWorldFlag.class));
    }

    @ParameterizedTest
    @MethodSource("flagCombinations")
    void roundTripsWorldsWithEveryFlagCombination(Set<SlimeWorldFlag> flags) throws Exception {
        SlimeWorldData original = SlimeTestFixtures.world(flags, 5, 3);

        SlimeWorldData decoded = SlimeReader.read(new ByteArrayInputStream(encode(original)));

        assertEquals(original, decoded);
    }

    @Test
    void roundTripsWorldWithNoChunksAndEmptyExtra() throws Exception {
        SlimeWorldData original = new SlimeWorldData(4189, Set.of(), List.of(), CompoundBinaryTag.empty());

        SlimeWorldData decoded = SlimeReader.read(new ByteArrayInputStream(encode(original)));

        assertEquals(original, decoded);
    }

    @Test
    void roundTripsChunkWithZeroSections() throws Exception {
        SlimeWorldData original = SlimeTestFixtures.world(Set.of(), 2, 0);

        SlimeWorldData decoded = SlimeReader.read(new ByteArrayInputStream(encode(original)));

        assertEquals(original, decoded);
        assertTrue(decoded.chunks().get(0).sections().isEmpty());
    }

    @Test
    void roundTripsUnknownFlagDataForForwardCompatibility() throws Exception {
        SlimeChunkData chunkWithUnknownFlag = new SlimeChunkData(
                0, 0, List.of(),
                SlimeTestFixtures.compound("heightmaps"),
                null, null, null,
                Map.of(3, new byte[] {1, 2, 3, 4}),
                SlimeTestFixtures.compound("tileEntities"),
                SlimeTestFixtures.compound("entities"),
                SlimeTestFixtures.compound("extra"));
        SlimeWorldData original = new SlimeWorldData(
                4189, Set.of(), List.of(chunkWithUnknownFlag), CompoundBinaryTag.empty());

        SlimeWorldData decoded = SlimeReader.read(new ByteArrayInputStream(encode(original)));

        assertEquals(original, decoded);
        assertTrue(Arrays.equals(new byte[] {1, 2, 3, 4}, decoded.chunks().get(0).unknownFlagData().get(3)));
    }

    @Test
    void roundTripsLargerWorld() throws Exception {
        SlimeWorldData original = SlimeTestFixtures.world(EnumSet.allOf(SlimeWorldFlag.class), 40, 8);

        SlimeWorldData decoded = SlimeReader.read(new ByteArrayInputStream(encode(original)));

        assertEquals(original, decoded);
        assertEquals(40, decoded.chunks().size());
    }

    static byte[] encode(SlimeWorldData world) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SlimeWriter.write(world, out);
        return out.toByteArray();
    }
}
