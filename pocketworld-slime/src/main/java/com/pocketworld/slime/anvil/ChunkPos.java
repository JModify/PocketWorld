package com.pocketworld.slime.anvil;

/** Absolute chunk coordinates, plus the region-file grouping and in-region slot they fall into. */
public record ChunkPos(int x, int z) {

    public int regionX() {
        return x >> 5;
    }

    public int regionZ() {
        return z >> 5;
    }

    public int localX() {
        return x & 31;
    }

    public int localZ() {
        return z & 31;
    }
}
