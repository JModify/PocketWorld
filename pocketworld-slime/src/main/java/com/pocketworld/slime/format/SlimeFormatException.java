package com.pocketworld.slime.format;

import java.io.IOException;

/** Base type for Slime-format-specific read/write failures. */
public class SlimeFormatException extends IOException {

    public SlimeFormatException(String message) {
        super(message);
    }

    public SlimeFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
