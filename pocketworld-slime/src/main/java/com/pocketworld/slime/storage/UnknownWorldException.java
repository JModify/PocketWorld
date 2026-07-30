package com.pocketworld.slime.storage;

import java.io.IOException;

/** No world is stored under the given id in this loader. */
public final class UnknownWorldException extends IOException {

    private final String worldId;

    public UnknownWorldException(String worldId) {
        super("No world stored under id \"" + worldId + "\"");
        this.worldId = worldId;
    }

    public String worldId() {
        return worldId;
    }
}
