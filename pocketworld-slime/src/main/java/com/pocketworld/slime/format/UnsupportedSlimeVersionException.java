package com.pocketworld.slime.format;

/**
 * The file's version byte is recognised as a real historical Slime format version, but reading it
 * isn't implemented yet. Only {@link SlimeConstants#CURRENT_VERSION} is currently supported; legacy
 * versions 1-12 are a planned follow-up (see docs/ARCHITECTURE.md), not something this exception
 * silently papers over.
 */
public final class UnsupportedSlimeVersionException extends SlimeFormatException {

    private final int version;

    public UnsupportedSlimeVersionException(int version, String message) {
        super(message);
        this.version = version;
    }

    public int version() {
        return version;
    }
}
