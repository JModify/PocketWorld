package com.pocketworld.plugin.world;

import java.util.UUID;

/**
 * A pending invitation to join a {@link PocketWorld}. Replaces the original {@code Map<UUID, UUID>}
 * representation, whose key/value order ("key = recipient, value = sender", per the original
 * PocketWorld's own field comment) was never actually enforced by the type system - the Mongo
 * deserialization code got it backwards (built the map as {@code sender -> recipient} instead),
 * silently breaking invitation lookups after every cache-miss reload. An explicit type with named
 * accessors makes that class of mistake a compile error instead of a runtime data-corruption bug.
 */
public record Invitation(UUID sender, UUID recipient, long timestamp) {

    public Invitation(UUID sender, UUID recipient) {
        this(sender, recipient, System.currentTimeMillis());
    }
}
