package com.pocketworld.plugin.runtime.bridge;

import org.bukkit.Location;
import org.bukkit.WorldBorder;

/**
 * The chunk-coordinate range a world's border actually covers, inclusive on both ends. Used to
 * strip out-of-border chunks when extracting a world's on-disk state back to Slime data - a world
 * border stops players from ever reaching those chunks, but does NOT stop Paper/vanilla's own chunk
 * generation pipeline from occasionally touching (and permanently persisting) real chunks well
 * outside it anyway. Confirmed empirically: even a single, otherwise-inert chunk load in a
 * brand-new world's previously-untouched territory can pull in a wide neighborhood of border/void
 * chunks - independent of the operation that triggered the load (read, write, or an explicit
 * chunk-load call all reproduced it identically) - almost certainly Paper's own multi-stage chunk
 * generation requiring a neighbor radius for structure-reference checks, not anything this plugin's
 * bridge code does. Filtering by the actual border on save is simpler and more robust than trying to
 * prevent whatever vanilla mechanism causes it.
 */
public record ChunkBounds(int minChunkX, int maxChunkX, int minChunkZ, int maxChunkZ) {

    public boolean contains(int chunkX, int chunkZ) {
        return chunkX >= minChunkX && chunkX <= maxChunkX && chunkZ >= minChunkZ && chunkZ <= maxChunkZ;
    }

    /** Inclusive of any chunk the border merely overlaps, not just chunks fully inside it - a
     *  legitimately-built chunk straddling the border edge must never be clipped. */
    public static ChunkBounds fromWorldBorder(WorldBorder border) {
        double half = border.getSize() / 2.0;
        Location center = border.getCenter();

        int minChunkX = Math.floorDiv((int) Math.floor(center.getX() - half), 16);
        int maxChunkX = Math.floorDiv((int) Math.floor(center.getX() + half), 16);
        int minChunkZ = Math.floorDiv((int) Math.floor(center.getZ() - half), 16);
        int maxChunkZ = Math.floorDiv((int) Math.floor(center.getZ() + half), 16);

        return new ChunkBounds(minChunkX, maxChunkX, minChunkZ, maxChunkZ);
    }
}
