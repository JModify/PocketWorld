package com.pocketworld.slime.format;

import com.pocketworld.slime.model.SlimeChunkData;
import com.pocketworld.slime.model.SlimeChunkSection;
import com.pocketworld.slime.model.SlimeWorldData;
import com.pocketworld.slime.model.SlimeWorldFlag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Builders for small, synthetic {@link SlimeWorldData} instances used by the round-trip and
 * corruption tests. Values are deliberately arbitrary (not real vanilla block-state/biome schema)
 * since Stage 1 tests the container/wire format, not Minecraft chunk semantics - that's Stage 2's
 * concern (the Anvil layer) and beyond.
 */
final class SlimeTestFixtures {

    private SlimeTestFixtures() {}

    static byte[] lightArray(long seed) {
        byte[] array = new byte[SlimeChunkSection.LIGHT_ARRAY_LENGTH];
        new Random(seed).nextBytes(array);
        return array;
    }

    static CompoundBinaryTag compound(String label) {
        return CompoundBinaryTag.builder()
                .putString("label", label)
                .putInt("value", label.hashCode())
                .build();
    }

    static CompoundBinaryTag listCompound(String listName, String... entries) {
        List<net.kyori.adventure.nbt.BinaryTag> entryTags = new ArrayList<>();
        for (String entry : entries) {
            entryTags.add(StringBinaryTag.stringBinaryTag(entry));
        }
        return CompoundBinaryTag.builder()
                .put(listName, ListBinaryTag.from(entryTags))
                .build();
    }

    static SlimeChunkSection section(boolean skyLight, boolean blockLight, int seed) {
        return new SlimeChunkSection(
                skyLight ? lightArray(seed) : null,
                blockLight ? lightArray(seed + 1) : null,
                compound("blockStates-" + seed),
                compound("biomes-" + seed));
    }

    static SlimeChunkData chunk(int x, int z, Set<SlimeWorldFlag> flags, int sectionCount) {
        List<SlimeChunkSection> sections = new ArrayList<>();
        for (int i = 0; i < sectionCount; i++) {
            // exercise all four light-presence combinations across sections
            boolean sky = (i % 2) == 0;
            boolean block = (i % 4) < 2;
            sections.add(section(sky, block, x * 1000 + z * 10 + i));
        }

        CompoundBinaryTag poi = flags.contains(SlimeWorldFlag.POI_CHUNKS) ? compound("poi-" + x + "-" + z) : null;
        CompoundBinaryTag blockTicks = flags.contains(SlimeWorldFlag.BLOCK_TICKS)
                ? listCompound("block_ticks", "minecraft:test_block_tick") : null;
        CompoundBinaryTag fluidTicks = flags.contains(SlimeWorldFlag.FLUID_TICKS)
                ? listCompound("fluid_ticks", "minecraft:test_fluid_tick") : null;

        return new SlimeChunkData(
                x, z, sections,
                compound("heightmaps-" + x + "-" + z),
                poi, blockTicks, fluidTicks,
                Map.of(),
                listCompound("tileEntities", "minecraft:test_tile_entity"),
                listCompound("entities", "minecraft:test_entity"),
                compound("chunkExtra-" + x + "-" + z));
    }

    static SlimeWorldData world(Set<SlimeWorldFlag> flags, int chunkCount, int sectionsPerChunk) {
        List<SlimeChunkData> chunks = new ArrayList<>();
        for (int i = 0; i < chunkCount; i++) {
            chunks.add(chunk(i, -i, flags, sectionsPerChunk));
        }
        return new SlimeWorldData(4189 /* an arbitrary Minecraft data version */, flags, chunks, compound("worldExtra"));
    }
}
