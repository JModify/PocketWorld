package me.modify.pocketworld.theme;

import lombok.Getter;
import org.bukkit.Material;

import java.util.UUID;

public class PocketTheme {

    /** UUID of this theme */
    @Getter private UUID id;

    /** Name of this theme */
    @Getter private String name;

    /** Biome for theme world. */
    @Getter private String biome;

    /** Icon representing theme. Visible to players when creating pocket world. */
    @Getter private Material icon;

    /** Description for this theme. Visible to players when creating pocket world. */
    @Getter private String description;

    /** Default spawn point for this theme in format x:y:z */
    @Getter private String spawnPoint;

    /**
     * Creates a new PocketTheme object.
     * @param id id of theme
     * @param name name of theme
     * @param description description of theme
     * @param spawnPoint default spawn point for theme
     * @param biome biome of theme world.
     * @param icon icon representing theme
     */
    public PocketTheme(UUID id, String name, String description, String spawnPoint, String biome, Material icon) {
        this.id = id;
        this.name = name;
        this.biome = biome;
        this.icon = icon;
        this.description = description;
        this.spawnPoint = spawnPoint;
    }
}
