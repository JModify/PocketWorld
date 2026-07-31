package com.pocketworld.plugin.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatInputRegistryTest {

    @Test
    void handleReturnsFalseWhenNothingPending() {
        ChatInputRegistry registry = new ChatInputRegistry();
        assertFalse(registry.handle(UUID.randomUUID(), "hello"));
    }

    @Test
    void handleConsumesAcceptedInput() {
        ChatInputRegistry registry = new ChatInputRegistry();
        UUID playerId = UUID.randomUUID();

        registry.await(playerId, message -> message.equals("correct"));

        assertTrue(registry.isPending(playerId));
        assertTrue(registry.handle(playerId, "correct"));
        assertFalse(registry.isPending(playerId));
    }

    @Test
    void handleKeepsWaitingWhenHandlerRejectsInput() {
        ChatInputRegistry registry = new ChatInputRegistry();
        UUID playerId = UUID.randomUUID();

        registry.await(playerId, message -> false);

        assertTrue(registry.handle(playerId, "wrong"));
        assertTrue(registry.isPending(playerId), "a rejected message should not cancel the pending input");
    }

    @Test
    void cancelStopsAwaitingInput() {
        ChatInputRegistry registry = new ChatInputRegistry();
        UUID playerId = UUID.randomUUID();

        registry.await(playerId, message -> true);
        registry.cancel(playerId);

        assertFalse(registry.isPending(playerId));
        assertFalse(registry.handle(playerId, "anything"));
    }

    @Test
    void awaitOverwritesAnyExistingHandlerForTheSamePlayer() {
        ChatInputRegistry registry = new ChatInputRegistry();
        UUID playerId = UUID.randomUUID();

        Function<String, Boolean> firstHandler = message -> {
            throw new AssertionError("stale handler should have been replaced");
        };
        registry.await(playerId, firstHandler);
        registry.await(playerId, message -> true);

        assertTrue(registry.handle(playerId, "anything"));
    }
}
