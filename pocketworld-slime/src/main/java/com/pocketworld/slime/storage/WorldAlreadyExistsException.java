package com.pocketworld.slime.storage;

import java.io.IOException;

/** A clone (or other create-only operation) targeted an id that's already in use. */
public final class WorldAlreadyExistsException extends IOException {

    private final String worldId;

    public WorldAlreadyExistsException(String worldId) {
        super("A world already exists under id \"" + worldId + "\"");
        this.worldId = worldId;
    }

    public String worldId() {
        return worldId;
    }
}
