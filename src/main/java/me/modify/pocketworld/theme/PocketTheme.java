package me.modify.pocketworld.theme;

import lombok.Getter;
import org.bukkit.Material;

import java.util.UUID;

public class PocketTheme {

    @Getter
    private UUID id;

    @Getter
    private String name;

    @Getter
    private String biome;

    @Getter
    private Material icon;

    public PocketTheme(UUID id, String name, String biome, Material icon) {
        this.id = id;
        this.name = name;
        this.biome = biome;
        this.icon = icon;
    }
}
