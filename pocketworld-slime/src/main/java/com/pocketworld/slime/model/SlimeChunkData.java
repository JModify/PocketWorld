package com.pocketworld.slime.model;

import net.kyori.adventure.nbt.CompoundBinaryTag;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * One chunk's worth of Slime-format data. {@code poiChunk}/{@code blockTicks}/{@code fluidTicks}
 * are only present when the owning {@link SlimeWorldData}'s flags say so; {@link SlimeWorldData}'s
 * constructor is responsible for enforcing that consistency across all of its chunks.
 * <p>
 * {@code unknownFlagData} preserves any per-chunk blobs for world-flag bits this reader doesn't
 * recognise (bit index 0-7, excluding the three known {@link SlimeWorldFlag} bits), so that
 * reading and re-writing a file that used a future/unknown flag doesn't silently drop data.
 */
public final class SlimeChunkData {

    private final int x;
    private final int z;
    private final List<SlimeChunkSection> sections;
    private final CompoundBinaryTag heightmaps;
    private final CompoundBinaryTag poiChunk;
    private final CompoundBinaryTag blockTicks;
    private final CompoundBinaryTag fluidTicks;
    private final Map<Integer, byte[]> unknownFlagData;
    private final CompoundBinaryTag tileEntities;
    private final CompoundBinaryTag entities;
    private final CompoundBinaryTag extra;

    public SlimeChunkData(int x, int z, List<SlimeChunkSection> sections, CompoundBinaryTag heightmaps,
                           CompoundBinaryTag poiChunk, CompoundBinaryTag blockTicks, CompoundBinaryTag fluidTicks,
                           Map<Integer, byte[]> unknownFlagData, CompoundBinaryTag tileEntities,
                           CompoundBinaryTag entities, CompoundBinaryTag extra) {
        this.x = x;
        this.z = z;
        this.sections = List.copyOf(Objects.requireNonNull(sections, "sections"));
        this.heightmaps = Objects.requireNonNull(heightmaps, "heightmaps");
        this.poiChunk = poiChunk;
        this.blockTicks = blockTicks;
        this.fluidTicks = fluidTicks;
        this.unknownFlagData = copyUnknownFlagData(unknownFlagData);
        this.tileEntities = Objects.requireNonNull(tileEntities, "tileEntities");
        this.entities = Objects.requireNonNull(entities, "entities");
        this.extra = Objects.requireNonNull(extra, "extra");
    }

    private static Map<Integer, byte[]> copyUnknownFlagData(Map<Integer, byte[]> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<Integer, byte[]> copy = new LinkedHashMap<>(source.size());
        for (Map.Entry<Integer, byte[]> entry : source.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().clone());
        }
        return Map.copyOf(copy);
    }

    public int x() {
        return x;
    }

    public int z() {
        return z;
    }

    public List<SlimeChunkSection> sections() {
        return sections;
    }

    public CompoundBinaryTag heightmaps() {
        return heightmaps;
    }

    /** Null if this world's flags don't include {@link SlimeWorldFlag#POI_CHUNKS}. */
    public CompoundBinaryTag poiChunk() {
        return poiChunk;
    }

    /** Null if this world's flags don't include {@link SlimeWorldFlag#BLOCK_TICKS}. */
    public CompoundBinaryTag blockTicks() {
        return blockTicks;
    }

    /** Null if this world's flags don't include {@link SlimeWorldFlag#FLUID_TICKS}. */
    public CompoundBinaryTag fluidTicks() {
        return fluidTicks;
    }

    public Map<Integer, byte[]> unknownFlagData() {
        return unknownFlagData;
    }

    public CompoundBinaryTag tileEntities() {
        return tileEntities;
    }

    public CompoundBinaryTag entities() {
        return entities;
    }

    /** Chunk-level custom/PDC data (v12+). Never null; empty if there's nothing stored. */
    public CompoundBinaryTag extra() {
        return extra;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SlimeChunkData other)) {
            return false;
        }
        return x == other.x
                && z == other.z
                && sections.equals(other.sections)
                && heightmaps.equals(other.heightmaps)
                && Objects.equals(poiChunk, other.poiChunk)
                && Objects.equals(blockTicks, other.blockTicks)
                && Objects.equals(fluidTicks, other.fluidTicks)
                && unknownFlagDataEquals(unknownFlagData, other.unknownFlagData)
                && tileEntities.equals(other.tileEntities)
                && entities.equals(other.entities)
                && extra.equals(other.extra);
    }

    private static boolean unknownFlagDataEquals(Map<Integer, byte[]> a, Map<Integer, byte[]> b) {
        if (!a.keySet().equals(b.keySet())) {
            return false;
        }
        for (Map.Entry<Integer, byte[]> entry : a.entrySet()) {
            if (!Arrays.equals(entry.getValue(), b.get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z, sections, heightmaps, poiChunk, blockTicks, fluidTicks, tileEntities, entities, extra);
    }

    @Override
    public String toString() {
        return "SlimeChunkData{x=" + x + ", z=" + z + ", sections=" + sections.size() + '}';
    }
}
