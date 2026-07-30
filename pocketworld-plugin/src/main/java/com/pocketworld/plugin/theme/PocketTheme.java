package com.pocketworld.plugin.theme;

import com.pocketworld.plugin.PocketWorldPlugin;
import org.bukkit.Material;

import java.io.IOException;
import java.util.UUID;

/** A reusable world template: a named, described "seed" world players pick from when creating a PocketWorld. */
public class PocketTheme {

    private final UUID id;
    private final String name;
    private final String biome;
    private final Material icon;
    private final String description;
    /** Default spawn point for this theme, in {@code x:y:z:yaw:pitch} format. */
    private final String spawnPoint;

    public PocketTheme(UUID id, String name, String description, String spawnPoint, String biome, Material icon) {
        this.id = id;
        this.name = name;
        this.biome = biome;
        this.icon = icon;
        this.description = description;
        this.spawnPoint = spawnPoint;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBiome() {
        return biome;
    }

    public Material getIcon() {
        return icon;
    }

    public String getDescription() {
        return description;
    }

    public String getSpawnPoint() {
        return spawnPoint;
    }

    public void delete(PocketWorldPlugin plugin) {
        plugin.getDataSource().getConnection().getDAO().deleteTheme(id);
        plugin.getThemeRegistry().delete(id);
        try {
            plugin.getThemeRuntime().deleteStored(id.toString());
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to delete stored world for theme " + id + ": " + e);
        }
    }
}
