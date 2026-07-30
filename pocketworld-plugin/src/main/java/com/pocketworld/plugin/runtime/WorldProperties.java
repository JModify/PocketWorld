package com.pocketworld.plugin.runtime;

/**
 * World-instantiation parameters that live outside the Slime binary format itself (spawn point,
 * difficulty, PVP) - supplied by the caller at materialize time, the same way SlimeWorldManager's
 * SlimePropertyMap was never part of the on-disk .slime file.
 */
public record WorldProperties(double spawnX, double spawnY, double spawnZ, float spawnYaw, float spawnPitch,
                               String difficulty, boolean pvp) {

    public static WorldProperties defaults() {
        return new WorldProperties(0.5, 64.0, 0.5, 0f, 0f, "normal", true);
    }
}
