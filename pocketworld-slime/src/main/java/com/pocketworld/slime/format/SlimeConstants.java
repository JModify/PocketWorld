package com.pocketworld.slime.format;

public final class SlimeConstants {

    /** 0xB10B, the two-byte magic every Slime file starts with. */
    public static final int MAGIC = 0xB10B;

    /**
     * The only format version {@link SlimeReader}/{@link SlimeWriter} currently implement.
     * Historical versions 1-12 are recognised (a file claiming one of them is reported via
     * {@link UnsupportedSlimeVersionException}, not silently misread) but not yet decoded.
     */
    public static final int CURRENT_VERSION = 13;

    public static final int MIN_KNOWN_VERSION = 1;

    private SlimeConstants() {}
}
