package com.pocketworld.slime.anvil;

import com.pocketworld.slime.model.SlimeChunkData;
import com.pocketworld.slime.model.SlimeChunkSection;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Converts between vanilla Anvil chunk NBT (the schema Minecraft/Paper itself reads and writes,
 * split across the region/entities region-file sets) and {@link SlimeChunkData} (the Slime
 * format's own per-chunk layout). This is a structural reshaping of the same underlying data, not
 * a reinterpretation of it - block state palettes, biome data, and entity NBT all pass through
 * completely opaque and untouched.
 * <p>
 * Verified empirically against a real Paper 26.2 (DataVersion 4903) world rather than assumed:
 * each vanilla section carries an explicit "Y" byte tag alongside "block_states"/"biomes" (the
 * documented Slime format doesn't show a per-section Y field at all, which only makes sense if
 * it's folded into the "block states" compound); entities live in a separate per-region file set
 * keyed by chunk (Position/DataVersion/Entities), not embedded in the main chunk.
 * <p>
 * Deliberately not preserved: chunk-generation bookkeeping the Slime format itself has no slot for
 * (Status, structures, PostProcessing, InhabitedTime, LastUpdate) and Paper's starlight lighting
 * cache. A materialized chunk simply looks freshly-loaded-and-unlit to those systems - a safe,
 * well-supported fallback path, not a correctness bug - exactly as the original format never
 * captured them either.
 * <p>
 * POI (points of interest - villager job sites, bells, etc.) is deliberately out of scope for now:
 * of the three optional Slime world flags this is the one with the least empirical grounding here
 * (no POI data existed in the reference world used to verify this converter), and Minecraft already
 * rebuilds POI data lazily by scanning a chunk's blocks when it's missing, so omitting it is safe,
 * not silently lossy.
 */
public final class AnvilChunkConverter {

    private AnvilChunkConverter() {}

    public static SlimeChunkData toSlimeChunk(int x, int z, CompoundBinaryTag regionChunk, CompoundBinaryTag entitiesChunk) {
        List<SlimeChunkSection> sections = new ArrayList<>();
        for (BinaryTag tag : regionChunk.getList("sections")) {
            sections.add(toSlimeSection((CompoundBinaryTag) tag));
        }

        CompoundBinaryTag heightmaps = regionChunk.getCompound("Heightmaps");

        CompoundBinaryTag blockTicks = CompoundBinaryTag.builder()
                .put("block_ticks", regionChunk.getList("block_ticks"))
                .build();
        CompoundBinaryTag fluidTicks = CompoundBinaryTag.builder()
                .put("fluid_ticks", regionChunk.getList("fluid_ticks"))
                .build();

        CompoundBinaryTag tileEntities = CompoundBinaryTag.builder()
                .put("tileEntities", regionChunk.getList("block_entities"))
                .build();

        ListBinaryTag entitiesList = entitiesChunk != null ? entitiesChunk.getList("Entities") : ListBinaryTag.empty();
        CompoundBinaryTag entities = CompoundBinaryTag.builder().put("entities", entitiesList).build();

        return new SlimeChunkData(x, z, sections, heightmaps, null, blockTicks, fluidTicks,
                Map.of(), tileEntities, entities, CompoundBinaryTag.empty());
    }

    private static SlimeChunkSection toSlimeSection(CompoundBinaryTag section) {
        byte[] skyLight = section.keySet().contains("SkyLight") ? section.getByteArray("SkyLight") : null;
        byte[] blockLight = section.keySet().contains("BlockLight") ? section.getByteArray("BlockLight") : null;

        CompoundBinaryTag.Builder blockStates = CompoundBinaryTag.builder().putByte("Y", section.getByte("Y", (byte) 0));
        BinaryTag blockStatesTag = section.get("block_states");
        if (blockStatesTag != null) {
            blockStates.put("block_states", blockStatesTag);
        }

        CompoundBinaryTag.Builder biomes = CompoundBinaryTag.builder();
        BinaryTag biomesTag = section.get("biomes");
        if (biomesTag != null) {
            biomes.put("biomes", biomesTag);
        }

        return new SlimeChunkSection(skyLight, blockLight, blockStates.build(), biomes.build());
    }

    /** The vanilla NBT for one chunk's region-file entry and its (always-present) entities-file entry. */
    public record VanillaChunk(CompoundBinaryTag region, CompoundBinaryTag entities) {}

    public static VanillaChunk toVanillaChunk(int dataVersion, SlimeChunkData chunk) {
        List<BinaryTag> sectionTags = new ArrayList<>();
        for (SlimeChunkSection section : chunk.sections()) {
            sectionTags.add(toVanillaSection(section));
        }

        CompoundBinaryTag.Builder region = CompoundBinaryTag.builder()
                .putInt("DataVersion", dataVersion)
                .putInt("xPos", chunk.x())
                .putInt("zPos", chunk.z())
                .putInt("yPos", minSectionY(chunk))
                .putString("Status", "minecraft:full")
                .put("sections", ListBinaryTag.from(sectionTags))
                .put("Heightmaps", chunk.heightmaps())
                .put("block_entities", chunk.tileEntities().getList("tileEntities"))
                .put("block_ticks", chunk.blockTicks() != null ? chunk.blockTicks().getList("block_ticks") : ListBinaryTag.empty())
                .put("fluid_ticks", chunk.fluidTicks() != null ? chunk.fluidTicks().getList("fluid_ticks") : ListBinaryTag.empty());

        CompoundBinaryTag entities = CompoundBinaryTag.builder()
                .putIntArray("Position", new int[] {chunk.x(), chunk.z()})
                .putInt("DataVersion", dataVersion)
                .put("Entities", chunk.entities().getList("entities"))
                .build();

        return new VanillaChunk(region.build(), entities);
    }

    private static int minSectionY(SlimeChunkData chunk) {
        if (chunk.sections().isEmpty()) {
            return 0;
        }
        // Sections are preserved bottom-to-top through both the Anvil and Slime readers/writers,
        // so the first one is always the lowest.
        return chunk.sections().get(0).blockStates().getByte("Y", (byte) 0);
    }

    private static CompoundBinaryTag toVanillaSection(SlimeChunkSection section) {
        CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder()
                .putByte("Y", section.blockStates().getByte("Y", (byte) 0));

        BinaryTag blockStatesTag = section.blockStates().get("block_states");
        if (blockStatesTag != null) {
            builder.put("block_states", blockStatesTag);
        }
        BinaryTag biomesTag = section.biomes().get("biomes");
        if (biomesTag != null) {
            builder.put("biomes", biomesTag);
        }
        if (section.hasSkyLight()) {
            builder.putByteArray("SkyLight", section.skyLight());
        }
        if (section.hasBlockLight()) {
            builder.putByteArray("BlockLight", section.blockLight());
        }
        return builder.build();
    }
}
