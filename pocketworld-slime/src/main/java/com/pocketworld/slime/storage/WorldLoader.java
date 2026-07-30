package com.pocketworld.slime.storage;

import java.io.IOException;
import java.util.List;

/**
 * Storage-agnostic byte-blob persistence for serialized Slime worlds. Implementations know nothing
 * about the Slime format itself - they just get and put opaque bytes under a world id - so the
 * runtime layer can point at a filesystem, a database, or anything else without caring which.
 * <p>
 * World-loading concerns that depend on more than one loader talking to each other (cross-server
 * lock coordination, for instance) are deliberately not part of this contract yet; they belong to
 * whatever concrete concurrency model the runtime layer ends up needing, once that's a real
 * question rather than a guess.
 */
public interface WorldLoader {

    boolean exists(String worldId) throws IOException;

    /** @throws UnknownWorldException if no world is stored under this id. */
    byte[] read(String worldId) throws IOException;

    /** Creates or overwrites the world under this id. */
    void write(String worldId, byte[] data) throws IOException;

    /** @throws UnknownWorldException if no world is stored under this id. */
    void delete(String worldId) throws IOException;

    List<String> list() throws IOException;

    /**
     * Copies a world to (possibly) a different loader under a new id. The default implementation
     * reads the whole world into memory and writes it back out; implementations may override this
     * to avoid that round trip when they can recognise the target as their own concrete type (e.g.
     * a plain filesystem copy instead of a read+write).
     *
     * @throws WorldAlreadyExistsException if a world already exists under {@code targetWorldId}.
     */
    default void cloneWorld(String worldId, WorldLoader targetLoader, String targetWorldId) throws IOException {
        if (targetLoader.exists(targetWorldId)) {
            throw new WorldAlreadyExistsException(targetWorldId);
        }
        targetLoader.write(targetWorldId, read(worldId));
    }
}
