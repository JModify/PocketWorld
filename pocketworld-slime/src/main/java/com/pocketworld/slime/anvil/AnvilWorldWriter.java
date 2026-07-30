package com.pocketworld.slime.anvil;

import net.kyori.adventure.nbt.CompoundBinaryTag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes a set of chunks out as a directory of Anvil region files, grouping them by the region
 * each chunk position falls into.
 */
public final class AnvilWorldWriter {

    private AnvilWorldWriter() {}

    public static void writeAll(Path regionDirectory, Map<ChunkPos, CompoundBinaryTag> chunks) throws IOException {
        Files.createDirectories(regionDirectory);

        Map<Long, List<ChunkPos>> byRegion = new LinkedHashMap<>();
        for (ChunkPos pos : chunks.keySet()) {
            byRegion.computeIfAbsent(regionKey(pos.regionX(), pos.regionZ()), key -> new ArrayList<>()).add(pos);
        }

        for (List<ChunkPos> regionChunks : byRegion.values()) {
            int regionX = regionChunks.get(0).regionX();
            int regionZ = regionChunks.get(0).regionZ();
            Path file = regionDirectory.resolve("r." + regionX + "." + regionZ + ".mca");

            try (RegionFile region = RegionFile.open(file, regionX, regionZ)) {
                for (ChunkPos pos : regionChunks) {
                    region.writeChunk(pos.localX(), pos.localZ(), chunks.get(pos));
                }
            }
        }
    }

    private static long regionKey(int regionX, int regionZ) {
        return (((long) regionX) << 32) ^ (regionZ & 0xFFFFFFFFL);
    }
}
