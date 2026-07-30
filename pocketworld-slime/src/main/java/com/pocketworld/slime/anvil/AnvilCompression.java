package com.pocketworld.slime.anvil;

/** The three chunk-payload compression schemes defined by the Anvil region file format. */
public enum AnvilCompression {

    GZIP(1),
    ZLIB(2),
    NONE(3);

    private final int id;

    AnvilCompression(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static AnvilCompression fromId(int id) {
        for (AnvilCompression compression : values()) {
            if (compression.id == id) {
                return compression;
            }
        }
        throw new IllegalArgumentException("Unknown Anvil compression type id: " + id);
    }
}
