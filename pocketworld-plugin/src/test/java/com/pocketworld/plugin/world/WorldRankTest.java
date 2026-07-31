package com.pocketworld.plugin.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorldRankTest {

    @Test
    void toStringIsLowercase() {
        for (WorldRank rank : WorldRank.values()) {
            assertEquals(rank.name().toLowerCase(), rank.toString());
        }
    }
}
