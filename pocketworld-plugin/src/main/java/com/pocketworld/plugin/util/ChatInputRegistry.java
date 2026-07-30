package com.pocketworld.plugin.util;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * A single, reusable "type a line in chat to answer a prompt" mechanism, used wherever the UI
 * needs a short piece of free-form text from a player (a world/theme name, an invite target's
 * username) - deliberately not an anvil-inventory text-entry GUI, since chat input is simpler,
 * needs no extra dependency, and is the same mechanism the theme-creation flow's description step
 * already used.
 * <p>
 * The handler returns whether the input was accepted: {@code true} consumes the prompt, {@code
 * false} leaves it in place so the same handler fires again on the player's next message (for
 * "invalid, try again" validation failures).
 */
public final class ChatInputRegistry {

    private final ConcurrentHashMap<UUID, Function<String, Boolean>> pending = new ConcurrentHashMap<>();

    public void await(UUID playerId, Function<String, Boolean> handler) {
        pending.put(playerId, handler);
    }

    public void cancel(UUID playerId) {
        pending.remove(playerId);
    }

    public boolean isPending(UUID playerId) {
        return pending.containsKey(playerId);
    }

    /** @return true if a pending prompt handled (and consumed, or asked to retry) this message. */
    public boolean handle(UUID playerId, String message) {
        Function<String, Boolean> handler = pending.get(playerId);
        if (handler == null) {
            return false;
        }
        if (handler.apply(message)) {
            pending.remove(playerId);
        }
        return true;
    }
}
