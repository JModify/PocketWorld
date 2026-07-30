package com.pocketworld.slime.anvil;

import net.kyori.adventure.nbt.CompoundBinaryTag;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads every populated chunk out of a directory of Anvil region files (a world's {@code region/},
 * {@code entities/}, or {@code poi/} folder - the container format is identical for all three).
 */
public final class AnvilWorldReader {

    private static final Pattern REGION_FILE_NAME = Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mca");

    private AnvilWorldReader() {}

    public static Map<ChunkPos, CompoundBinaryTag> readAll(Path regionDirectory) throws IOException {
        Map<ChunkPos, CompoundBinaryTag> chunks = new LinkedHashMap<>();
        if (!Files.isDirectory(regionDirectory)) {
            return chunks;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(regionDirectory, "r.*.*.mca")) {
            for (Path file : stream) {
                int[] regionCoords = parseRegionFileName(file.getFileName().toString());
                if (regionCoords == null) {
                    continue;
                }
                readRegionInto(chunks, file, regionCoords[0], regionCoords[1]);
            }
        }
        return chunks;
    }

    private static void readRegionInto(Map<ChunkPos, CompoundBinaryTag> chunks, Path file, int regionX, int regionZ)
            throws IOException {
        try (RegionFile region = RegionFile.open(file, regionX, regionZ)) {
            for (int localZ = 0; localZ < 32; localZ++) {
                for (int localX = 0; localX < 32; localX++) {
                    if (!region.hasChunk(localX, localZ)) {
                        continue;
                    }
                    CompoundBinaryTag tag = region.readChunk(localX, localZ);
                    int x = (regionX << 5) | localX;
                    int z = (regionZ << 5) | localZ;
                    chunks.put(new ChunkPos(x, z), tag);
                }
            }
        }
    }

    static int[] parseRegionFileName(String name) {
        Matcher matcher = REGION_FILE_NAME.matcher(name);
        if (!matcher.matches()) {
            return null;
        }
        return new int[] {Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))};
    }
}
