package com.pocketworld.plugin.util;

import org.bukkit.generator.ChunkGenerator;

/**
 * Generates nothing at all: every vanilla generation step is disabled, so any chunk Bukkit needs
 * that isn't already present on disk comes out as empty air instead of falling through to the
 * default overworld generator.
 * <p>
 * Used both by {@link com.pocketworld.plugin.runtime.bridge.anvil.AnvilShadowBridge} (a world
 * border alone does not stop chunk generation, only entities crossing it - anything that touches a
 * chunk outside a pocket world's stored footprint, such as the vanilla spawn-safety search during
 * world creation, would otherwise paste in a real, random slice of vanilla terrain having nothing
 * to do with the stored world - confirmed by a real in-game report of exactly that) and by the
 * theme-creation editor world (which has no stored data to protect yet, but should look and behave
 * the same way a materialized pocket world eventually will - an intentionally empty canvas, not a
 * fully terrain-generated world that happens to be biome-locked).
 */
public final class VoidGenerator extends ChunkGenerator {

    @Override
    public boolean shouldGenerateNoise() {
        return false;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return false;
    }

    @Override
    public boolean shouldGenerateBedrock() {
        return false;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return false;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return false;
    }
}
