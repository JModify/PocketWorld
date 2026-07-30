package com.pocketworld.slime.model;

import net.kyori.adventure.nbt.CompoundBinaryTag;

import java.util.Arrays;
import java.util.Objects;

/**
 * One Y-slice of a chunk: optional sky/block light nibble arrays, plus block-state and biome NBT
 * compounds. Light arrays are 2048 bytes (4096 nibbles) each, matching vanilla's packed format.
 */
public final class SlimeChunkSection {

    public static final int LIGHT_ARRAY_LENGTH = 2048;

    private final byte[] skyLight;
    private final byte[] blockLight;
    private final CompoundBinaryTag blockStates;
    private final CompoundBinaryTag biomes;

    public SlimeChunkSection(byte[] skyLight, byte[] blockLight, CompoundBinaryTag blockStates, CompoundBinaryTag biomes) {
        this.skyLight = validateLight(skyLight, "skyLight");
        this.blockLight = validateLight(blockLight, "blockLight");
        this.blockStates = Objects.requireNonNull(blockStates, "blockStates");
        this.biomes = Objects.requireNonNull(biomes, "biomes");
    }

    private static byte[] validateLight(byte[] light, String name) {
        if (light == null) {
            return null;
        }
        if (light.length != LIGHT_ARRAY_LENGTH) {
            throw new IllegalArgumentException(name + " must be exactly " + LIGHT_ARRAY_LENGTH + " bytes, got " + light.length);
        }
        return light.clone();
    }

    public boolean hasSkyLight() {
        return skyLight != null;
    }

    public boolean hasBlockLight() {
        return blockLight != null;
    }

    /** Returns a defensive copy, or null if this section has no sky light stored. */
    public byte[] skyLight() {
        return skyLight == null ? null : skyLight.clone();
    }

    /** Returns a defensive copy, or null if this section has no block light stored. */
    public byte[] blockLight() {
        return blockLight == null ? null : blockLight.clone();
    }

    public CompoundBinaryTag blockStates() {
        return blockStates;
    }

    public CompoundBinaryTag biomes() {
        return biomes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SlimeChunkSection other)) {
            return false;
        }
        return Arrays.equals(skyLight, other.skyLight)
                && Arrays.equals(blockLight, other.blockLight)
                && blockStates.equals(other.blockStates)
                && biomes.equals(other.biomes);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(skyLight);
        result = 31 * result + Arrays.hashCode(blockLight);
        result = 31 * result + blockStates.hashCode();
        result = 31 * result + biomes.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SlimeChunkSection{hasSkyLight=" + hasSkyLight() + ", hasBlockLight=" + hasBlockLight() + '}';
    }
}
