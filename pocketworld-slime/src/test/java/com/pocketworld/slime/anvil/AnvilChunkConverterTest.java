package com.pocketworld.slime.anvil;

import com.pocketworld.slime.model.SlimeChunkData;
import com.pocketworld.slime.model.SlimeChunkSection;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The synthetic vanilla chunk NBT built here mirrors the real schema confirmed empirically against
 * a running Paper 26.2 (DataVersion 4903) server: explicit per-section "Y", top-level "block_ticks"/
 * "fluid_ticks" lists, "block_entities" (not "TileEntities"), and entities living in a separate
 * region-file-shaped compound (Position/DataVersion/Entities) rather than inside the chunk itself.
 */
class AnvilChunkConverterTest {

    private static final int DATA_VERSION = 4903;

    @Test
    void roundTripsBlockStatesBiomesAndSectionY() {
        CompoundBinaryTag vanillaChunk = vanillaChunk(sectionsOnly());

        SlimeChunkData slime = AnvilChunkConverter.toSlimeChunk(3, -2, vanillaChunk, null);
        AnvilChunkConverter.VanillaChunk back = AnvilChunkConverter.toVanillaChunk(DATA_VERSION, slime);

        assertEquals(2, slime.sections().size());
        SlimeChunkSection sectionMinus1 = slime.sections().get(0);
        assertEquals((byte) -1, sectionMinus1.blockStates().getByte("Y"));
        assertEquals(vanillaTag("block_states", -1), sectionMinus1.blockStates().get("block_states"));
        assertEquals(vanillaTag("biomes", -1), sectionMinus1.biomes().get("biomes"));

        assertEquals(vanillaChunk.getList("sections"), back.region().getList("sections"));
        assertEquals(3, back.region().getInt("xPos"));
        assertEquals(-2, back.region().getInt("zPos"));
        assertEquals(-1, back.region().getInt("yPos")); // lowest section
    }

    @Test
    void preservesLightArraysWhenPresentAndOmitsThemWhenAbsent() {
        CompoundBinaryTag sectionWithLight = CompoundBinaryTag.builder()
                .putByte("Y", (byte) 0)
                .put("block_states", vanillaTag("block_states", 0))
                .put("biomes", vanillaTag("biomes", 0))
                .putByteArray("SkyLight", lightArray((byte) 1))
                .putByteArray("BlockLight", lightArray((byte) 2))
                .build();
        CompoundBinaryTag sectionWithoutLight = CompoundBinaryTag.builder()
                .putByte("Y", (byte) 1)
                .put("block_states", vanillaTag("block_states", 1))
                .put("biomes", vanillaTag("biomes", 1))
                .build();
        CompoundBinaryTag vanillaChunk = vanillaChunk(ListBinaryTag.from(java.util.List.of(sectionWithLight, sectionWithoutLight)));

        SlimeChunkData slime = AnvilChunkConverter.toSlimeChunk(0, 0, vanillaChunk, null);

        SlimeChunkSection s0 = slime.sections().get(0);
        assertTrue(s0.hasSkyLight());
        assertTrue(s0.hasBlockLight());
        assertArrayEquals(lightArray((byte) 1), s0.skyLight());
        assertArrayEquals(lightArray((byte) 2), s0.blockLight());

        SlimeChunkSection s1 = slime.sections().get(1);
        assertFalse(s1.hasSkyLight());
        assertFalse(s1.hasBlockLight());

        AnvilChunkConverter.VanillaChunk back = AnvilChunkConverter.toVanillaChunk(DATA_VERSION, slime);
        assertEquals(vanillaChunk.getList("sections"), back.region().getList("sections"));
    }

    @Test
    void roundTripsHeightmapsBlockEntitiesAndTicksVerbatim() {
        CompoundBinaryTag heightmaps = CompoundBinaryTag.builder()
                .putLongArray("WORLD_SURFACE", new long[] {1L, 2L, 3L})
                .build();
        ListBinaryTag blockEntities = ListBinaryTag.from(java.util.List.of(
                CompoundBinaryTag.builder().putString("id", "minecraft:chest").putInt("x", 1).build()));
        ListBinaryTag blockTicks = ListBinaryTag.from(java.util.List.of(
                CompoundBinaryTag.builder().putString("i", "minecraft:redstone_wire").build()));
        ListBinaryTag fluidTicks = ListBinaryTag.from(java.util.List.of(
                CompoundBinaryTag.builder().putString("i", "minecraft:water").build()));

        CompoundBinaryTag vanillaChunk = CompoundBinaryTag.builder()
                .putInt("DataVersion", DATA_VERSION)
                .putInt("xPos", 0)
                .putInt("zPos", 0)
                .putInt("yPos", -4)
                .putString("Status", "minecraft:full")
                .put("sections", sectionsOnly())
                .put("Heightmaps", heightmaps)
                .put("block_entities", blockEntities)
                .put("block_ticks", blockTicks)
                .put("fluid_ticks", fluidTicks)
                // fields the Slime format has no slot for - must not break conversion
                .putLong("LastUpdate", 123L)
                .putLong("InhabitedTime", 456L)
                .put("structures", CompoundBinaryTag.empty())
                .build();

        SlimeChunkData slime = AnvilChunkConverter.toSlimeChunk(0, 0, vanillaChunk, null);

        assertEquals(heightmaps, slime.heightmaps());
        assertEquals(blockEntities, slime.tileEntities().getList("tileEntities"));
        assertEquals(blockTicks, slime.blockTicks().getList("block_ticks"));
        assertEquals(fluidTicks, slime.fluidTicks().getList("fluid_ticks"));

        AnvilChunkConverter.VanillaChunk back = AnvilChunkConverter.toVanillaChunk(DATA_VERSION, slime);
        assertEquals(heightmaps, back.region().getCompound("Heightmaps"));
        assertEquals(blockEntities, back.region().getList("block_entities"));
        assertEquals(blockTicks, back.region().getList("block_ticks"));
        assertEquals(fluidTicks, back.region().getList("fluid_ticks"));
        // deliberately not preserved
        assertEquals(0L, back.region().getLong("LastUpdate", 0L));
        assertFalse(back.region().keySet().contains("structures"));
    }

    @Test
    void roundTripsEntitiesFromTheSeparateEntitiesRegionFile() {
        ListBinaryTag entityList = ListBinaryTag.from(java.util.List.of(
                CompoundBinaryTag.builder().putString("id", "minecraft:pig").build()));
        CompoundBinaryTag entitiesChunk = CompoundBinaryTag.builder()
                .putIntArray("Position", new int[] {5, 7})
                .putInt("DataVersion", DATA_VERSION)
                .put("Entities", entityList)
                .build();

        SlimeChunkData slime = AnvilChunkConverter.toSlimeChunk(5, 7, vanillaChunk(sectionsOnly()), entitiesChunk);
        assertEquals(entityList, slime.entities().getList("entities"));

        AnvilChunkConverter.VanillaChunk back = AnvilChunkConverter.toVanillaChunk(DATA_VERSION, slime);
        assertEquals(entityList, back.entities().getList("Entities"));
        assertArrayEquals(new int[] {5, 7}, back.entities().getIntArray("Position"));
    }

    @Test
    void missingEntitiesChunkRoundTripsAsEmpty() {
        SlimeChunkData slime = AnvilChunkConverter.toSlimeChunk(0, 0, vanillaChunk(sectionsOnly()), null);
        assertTrue(slime.entities().getList("entities").isEmpty());

        AnvilChunkConverter.VanillaChunk back = AnvilChunkConverter.toVanillaChunk(DATA_VERSION, slime);
        assertTrue(back.entities().getList("Entities").isEmpty());
    }

    @Test
    void poiIsDeliberatelyNotPopulated() {
        SlimeChunkData slime = AnvilChunkConverter.toSlimeChunk(0, 0, vanillaChunk(sectionsOnly()), null);
        assertNull(slime.poiChunk());
    }

    private static ListBinaryTag sectionsOnly() {
        CompoundBinaryTag sectionMinus1 = CompoundBinaryTag.builder()
                .putByte("Y", (byte) -1)
                .put("block_states", vanillaTag("block_states", -1))
                .put("biomes", vanillaTag("biomes", -1))
                .build();
        CompoundBinaryTag section0 = CompoundBinaryTag.builder()
                .putByte("Y", (byte) 0)
                .put("block_states", vanillaTag("block_states", 0))
                .put("biomes", vanillaTag("biomes", 0))
                .build();
        return ListBinaryTag.from(java.util.List.of(sectionMinus1, section0));
    }

    private static CompoundBinaryTag vanillaChunk(ListBinaryTag sections) {
        return CompoundBinaryTag.builder()
                .putInt("DataVersion", DATA_VERSION)
                .putInt("xPos", 3)
                .putInt("zPos", -2)
                .putInt("yPos", -1)
                .putString("Status", "minecraft:full")
                .put("sections", sections)
                .put("Heightmaps", CompoundBinaryTag.empty())
                .put("block_entities", ListBinaryTag.empty())
                .put("block_ticks", ListBinaryTag.empty())
                .put("fluid_ticks", ListBinaryTag.empty())
                .build();
    }

    /** A stand-in for the real (opaque, palette-based) block_states/biomes sub-tag content. */
    private static CompoundBinaryTag vanillaTag(String label, int y) {
        return CompoundBinaryTag.builder()
                .put("palette", ListBinaryTag.from(java.util.List.of(StringBinaryTag.stringBinaryTag(label + "-" + y))))
                .build();
    }

    private static byte[] lightArray(byte fill) {
        byte[] array = new byte[SlimeChunkSection.LIGHT_ARRAY_LENGTH];
        java.util.Arrays.fill(array, fill);
        return array;
    }
}
