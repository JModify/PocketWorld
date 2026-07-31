package com.pocketworld.plugin.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorldSpawnTest {

    @Test
    void toStringAndFromStringRoundTrip() {
        WorldSpawn original = new WorldSpawn(1.5, 64.0, -12.25, 90.0f, -45.0f);

        WorldSpawn parsed = WorldSpawn.fromString(original.toString());

        assertEquals(original.x(), parsed.x());
        assertEquals(original.y(), parsed.y());
        assertEquals(original.z(), parsed.z());
        assertEquals(original.yaw(), parsed.yaw());
        assertEquals(original.pitch(), parsed.pitch());
    }

    @Test
    void settersMutateInPlace() {
        WorldSpawn spawn = new WorldSpawn(0, 0, 0, 0, 0);

        spawn.setX(5);
        spawn.setY(6);
        spawn.setZ(7);
        spawn.setYaw(8);
        spawn.setPitch(9);

        assertEquals(5, spawn.x());
        assertEquals(6, spawn.y());
        assertEquals(7, spawn.z());
        assertEquals(8, spawn.yaw());
        assertEquals(9, spawn.pitch());
    }

    @Test
    void fromStringRejectsMalformedInput() {
        assertThrows(NumberFormatException.class, () -> WorldSpawn.fromString("not:a:valid:spawn:string"));
    }
}
