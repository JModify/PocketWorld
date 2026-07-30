package com.pocketworld.slime.model;

import net.kyori.adventure.nbt.CompoundBinaryTag;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A fully decoded Slime-format world: the world's Minecraft data version, its flags, every chunk,
 * and the world-level "extra" custom/PDC compound. This is the in-memory representation
 * {@link com.pocketworld.slime.format.SlimeReader}/{@code SlimeWriter} read and write; it carries
 * no notion of storage location or runtime world-creation properties (spawn point, difficulty,
 * etc.) since those aren't part of the on-disk binary format itself.
 */
public final class SlimeWorldData {

    private final int dataVersion;
    private final Set<SlimeWorldFlag> flags;
    private final List<SlimeChunkData> chunks;
    private final CompoundBinaryTag extra;

    public SlimeWorldData(int dataVersion, Set<SlimeWorldFlag> flags, List<SlimeChunkData> chunks, CompoundBinaryTag extra) {
        this.dataVersion = dataVersion;
        this.flags = flags.isEmpty() ? EnumSet.noneOf(SlimeWorldFlag.class) : EnumSet.copyOf(flags);
        this.chunks = List.copyOf(Objects.requireNonNull(chunks, "chunks"));
        this.extra = Objects.requireNonNull(extra, "extra");
        validateChunkFlagConsistency();
    }

    private void validateChunkFlagConsistency() {
        boolean poi = flags.contains(SlimeWorldFlag.POI_CHUNKS);
        boolean blockTicks = flags.contains(SlimeWorldFlag.BLOCK_TICKS);
        boolean fluidTicks = flags.contains(SlimeWorldFlag.FLUID_TICKS);

        for (SlimeChunkData chunk : chunks) {
            requireConsistent(poi, chunk.poiChunk() != null, "POI_CHUNKS", chunk);
            requireConsistent(blockTicks, chunk.blockTicks() != null, "BLOCK_TICKS", chunk);
            requireConsistent(fluidTicks, chunk.fluidTicks() != null, "FLUID_TICKS", chunk);
        }
    }

    private static void requireConsistent(boolean flagSet, boolean fieldPresent, String flagName, SlimeChunkData chunk) {
        if (flagSet != fieldPresent) {
            String problem = flagSet
                    ? "flag is set but chunk has no " + flagName + " data"
                    : "chunk has " + flagName + " data but the world flag isn't set";
            throw new IllegalArgumentException(
                    flagName + " inconsistency for chunk (" + chunk.x() + "," + chunk.z() + "): " + problem);
        }
    }

    public int dataVersion() {
        return dataVersion;
    }

    public Set<SlimeWorldFlag> flags() {
        return flags;
    }

    public List<SlimeChunkData> chunks() {
        return chunks;
    }

    /** World-level custom/PDC data. Never null; empty if there's nothing stored. */
    public CompoundBinaryTag extra() {
        return extra;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SlimeWorldData other)) {
            return false;
        }
        return dataVersion == other.dataVersion
                && flags.equals(other.flags)
                && chunks.equals(other.chunks)
                && extra.equals(other.extra);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataVersion, flags, chunks, extra);
    }

    @Override
    public String toString() {
        return "SlimeWorldData{dataVersion=" + dataVersion + ", flags=" + flags + ", chunks=" + chunks.size() + '}';
    }
}
