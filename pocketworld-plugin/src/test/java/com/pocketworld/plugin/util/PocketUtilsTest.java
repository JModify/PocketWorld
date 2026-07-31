package com.pocketworld.plugin.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PocketUtilsTest {

    @Test
    void acceptsARandomUuidString() {
        assertTrue(PocketUtils.isUUID(UUID.randomUUID().toString()));
    }

    @Test
    void rejectsPlainWorldNames() {
        assertFalse(PocketUtils.isUUID("world"));
        assertFalse(PocketUtils.isUUID("world_nether"));
    }

    @Test
    void rejectsNearMissStrings() {
        assertFalse(PocketUtils.isUUID(UUID.randomUUID().toString().replace("-", "")));
        assertFalse(PocketUtils.isUUID(""));
    }
}
