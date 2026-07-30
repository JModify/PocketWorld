package com.pocketworld.slime.format;

/** The file isn't a valid Slime file, or its declared structure doesn't match its actual contents. */
public final class CorruptedSlimeFileException extends SlimeFormatException {

    public CorruptedSlimeFileException(String message) {
        super(message);
    }

    public CorruptedSlimeFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
