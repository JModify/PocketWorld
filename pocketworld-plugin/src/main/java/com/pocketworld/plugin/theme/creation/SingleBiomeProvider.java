package com.pocketworld.plugin.theme.creation;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;

import java.util.List;

/** Forces an entire generated world to a single biome - used for theme editor worlds. */
final class SingleBiomeProvider extends BiomeProvider {

    private final Biome biome;

    SingleBiomeProvider(String biomeName) {
        Biome resolved = Registry.BIOME.get(NamespacedKey.minecraft(biomeName.toLowerCase(java.util.Locale.ROOT)));
        this.biome = resolved != null ? resolved : Biome.PLAINS;
    }

    @Override
    public Biome getBiome(WorldInfo worldInfo, int x, int y, int z) {
        return biome;
    }

    @Override
    public List<Biome> getBiomes(WorldInfo worldInfo) {
        return List.of(biome);
    }
}
