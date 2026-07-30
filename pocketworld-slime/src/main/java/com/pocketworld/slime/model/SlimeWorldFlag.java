package com.pocketworld.slime.model;

/**
 * The three "additional world flags" defined by Slime format v13, each controlling whether an
 * extra per-chunk section is present in the stream. Bit values match the documented format
 * exactly; they are not reassignable.
 */
public enum SlimeWorldFlag {

    POI_CHUNKS(1),
    FLUID_TICKS(2),
    BLOCK_TICKS(4);

    private final int bit;

    SlimeWorldFlag(int bit) {
        this.bit = bit;
    }

    public int bit() {
        return bit;
    }
}
