package com.pocketworld.plugin.runtime.bridge.anvil;

import com.pocketworld.plugin.runtime.WorldProperties;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Writes the minimal {@code level.dat} that makes Bukkit treat a folder as an existing world
 * (rather than a fresh one to generate) and tells it where to spawn. Fields confirmed against a
 * real Paper 26.2 (DataVersion 4903) level.dat; everything else (game rules, weather, raids, ...)
 * is left for the server to synthesize via {@link org.bukkit.WorldCreator}'s own parameters, the
 * same way the Slime format itself never captured any of that either.
 * <p>
 * {@code WorldGenSettings} is embedded in {@code level.dat} for older-version compatibility (Paper
 * 1.21.11's world-loading codec throws {@code IllegalStateException: No key dimensions ...; No key
 * seed ...} without one - discovered by actually activating a bridge-materialized world against a
 * real 1.21.11 server), but on this project's current floor it's actually vestigial: confirmed
 * empirically (by creating an ordinary secondary world, saving it, and inspecting the result) that
 * a non-primary world's dimension-generator settings are read from a separate
 * {@code data/minecraft/world_gen_settings.dat} file instead - {@link #writeWorldGenSettingsFile}
 * writes that same structure there. Paper 26.2 happens to tolerate that file's absence (falls back
 * silently); Spigot 26.2 does not (throws {@code IllegalStateException: Overworld settings missing}
 * out of {@code Bukkit.createWorld()} - a real, live-server-observed crash, not a hypothetical),
 * which is what actually forced writing this file rather than leaving it as a nice-to-have. Both
 * copies share the same dimensions structure, built once by {@link #worldGenSettings()}.
 */
public final class LevelDatWriter {

    private static final int LEGACY_ANVIL_VERSION = 19133;
    private static final long PLACEHOLDER_SEED = 0L;

    private LevelDatWriter() {}

    /**
     * Also used directly by theme creation to pre-seed a brand-new editor world's level.dat with a
     * known spawn before {@link org.bukkit.WorldCreator} ever runs - without that, vanilla's own
     * "find/prepare a valid spawn" step runs unconditionally on first creation and touches (and
     * permanently persists) a large fixed radius of otherwise-untouched void chunks around origin,
     * confirmed empirically to be completely independent of {@code keepSpawnLoaded} or how soon the
     * world border is set afterward - the only thing that avoided it was the level already declaring
     * itself initialized with a known spawn before Bukkit's own creation call ever sees it.
     */
    public static void write(Path worldFolder, String levelName, int dataVersion, WorldProperties properties) throws IOException {
        CompoundBinaryTag versionTag = CompoundBinaryTag.builder()
                .putInt("Id", dataVersion)
                .putString("Name", "PocketWorld")
                .putString("Series", "main")
                .putByte("Snapshot", (byte) 0)
                .build();

        CompoundBinaryTag difficultySettings = CompoundBinaryTag.builder()
                .putByte("difficulty", difficultyId(properties.difficulty()))
                .putByte("hardcore", (byte) 0)
                .putByte("locked", (byte) 0)
                .build();

        CompoundBinaryTag spawn = CompoundBinaryTag.builder()
                .putIntArray("pos", new int[] {
                        (int) Math.floor(properties.spawnX()),
                        (int) Math.floor(properties.spawnY()),
                        (int) Math.floor(properties.spawnZ())})
                .putFloat("yaw", properties.spawnYaw())
                .putFloat("pitch", properties.spawnPitch())
                .putString("dimension", "minecraft:overworld")
                .build();

        CompoundBinaryTag dataPacks = CompoundBinaryTag.builder()
                .put("Enabled", net.kyori.adventure.nbt.ListBinaryTag.from(
                        java.util.List.of(net.kyori.adventure.nbt.StringBinaryTag.stringBinaryTag("vanilla"))))
                .put("Disabled", net.kyori.adventure.nbt.ListBinaryTag.empty())
                .build();

        CompoundBinaryTag data = CompoundBinaryTag.builder()
                .putInt("DataVersion", dataVersion)
                .put("Version", versionTag)
                .putInt("version", LEGACY_ANVIL_VERSION)
                .putString("LevelName", levelName)
                .putInt("GameType", 0)
                .put("difficulty_settings", difficultySettings)
                .putByte("allowCommands", (byte) 0)
                .putByte("initialized", (byte) 1)
                .putByte("WasModded", (byte) 0)
                .put("spawn", spawn)
                .putLong("Time", 0L)
                .putLong("LastPlayed", System.currentTimeMillis())
                .put("DataPacks", dataPacks)
                .put("WorldGenSettings", worldGenSettings())
                .build();

        CompoundBinaryTag root = CompoundBinaryTag.builder().put("Data", data).build();

        BinaryTagIO.writer().write(root, worldFolder.resolve("level.dat"), BinaryTagIO.Compression.GZIP);
        writeWorldGenSettingsFile(worldFolder, dataVersion);
    }

    /**
     * The actual, load-bearing source of a non-primary world's dimension-generator settings on this
     * project's current floor - see the class doc. Structure confirmed against a real Paper 26.2
     * (DataVersion 4903) {@code data/minecraft/world_gen_settings.dat}, created by letting a real
     * server generate and save an ordinary secondary world, then inspecting the result directly.
     */
    private static void writeWorldGenSettingsFile(Path worldFolder, int dataVersion) throws IOException {
        CompoundBinaryTag root = CompoundBinaryTag.builder()
                .put("data", worldGenSettings())
                .putInt("DataVersion", dataVersion)
                .build();

        Path dataFolder = worldFolder.resolve("data").resolve("minecraft");
        java.nio.file.Files.createDirectories(dataFolder);
        BinaryTagIO.writer().write(root, dataFolder.resolve("world_gen_settings.dat"), BinaryTagIO.Compression.GZIP);
    }

    private static CompoundBinaryTag worldGenSettings() {
        return CompoundBinaryTag.builder()
                .putLong("seed", PLACEHOLDER_SEED)
                .putByte("generate_structures", (byte) 1)
                .putByte("bonus_chest", (byte) 0)
                .put("dimensions", CompoundBinaryTag.builder()
                        .put("minecraft:overworld", dimension("minecraft:overworld", "minecraft:overworld", "minecraft:overworld"))
                        .put("minecraft:the_nether", dimension("minecraft:the_nether", "minecraft:nether", "minecraft:nether"))
                        .put("minecraft:the_end", endDimension())
                        .build())
                .build();
    }

    private static CompoundBinaryTag dimension(String type, String generatorSettings, String biomePreset) {
        CompoundBinaryTag biomeSource = CompoundBinaryTag.builder()
                .putString("type", "minecraft:multi_noise")
                .putString("preset", biomePreset)
                .build();
        CompoundBinaryTag generator = CompoundBinaryTag.builder()
                .putString("type", "minecraft:noise")
                .putString("settings", generatorSettings)
                .put("biome_source", biomeSource)
                .build();
        return CompoundBinaryTag.builder()
                .putString("type", type)
                .put("generator", generator)
                .build();
    }

    /** The end uses a fixed single-biome source (just "the_end"), not a multi_noise preset. */
    private static CompoundBinaryTag endDimension() {
        CompoundBinaryTag biomeSource = CompoundBinaryTag.builder()
                .putString("type", "minecraft:the_end")
                .build();
        CompoundBinaryTag generator = CompoundBinaryTag.builder()
                .putString("type", "minecraft:noise")
                .putString("settings", "minecraft:end")
                .put("biome_source", biomeSource)
                .build();
        return CompoundBinaryTag.builder()
                .putString("type", "minecraft:the_end")
                .put("generator", generator)
                .build();
    }

    private static byte difficultyId(String name) {
        return switch (name.toLowerCase(java.util.Locale.ROOT)) {
            case "peaceful" -> 0;
            case "easy" -> 1;
            case "hard" -> 3;
            default -> 2; // normal
        };
    }
}
