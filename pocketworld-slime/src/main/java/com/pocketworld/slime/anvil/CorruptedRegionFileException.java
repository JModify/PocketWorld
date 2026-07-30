package com.pocketworld.slime.anvil;

import java.io.IOException;

/** A region (or external .mcc) file's declared structure doesn't match its actual contents. */
public final class CorruptedRegionFileException extends IOException {

    public CorruptedRegionFileException(String message) {
        super(message);
    }

    public CorruptedRegionFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
