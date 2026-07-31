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
 * {@code WorldGenSettings} is the one exception: Paper 26.2 tolerates its absence, but Paper
 * 1.21.11's world-loading codec throws ({@code IllegalStateException: No key dimensions ...; No key
 * seed ...}) without one - discovered by actually activating a bridge-materialized world against a
 * real 1.21.11 server, not documented anywhere. The block written here is a structural copy of what
 * a real Paper 1.21.11 server writes for an ordinary, freshly-generated secondary world (confirmed
 * by creating one and inspecting its level.dat directly): {@code generator.settings} is just a
 * string reference to a built-in noise-settings preset name ({@code "minecraft:overworld"} etc.),
 * not an inline definition, so this needs no per-world generation parameters of our own. The seed
 * and generator are never actually exercised in normal operation - every chunk a pocket world's
 * world border lets a player reach is one this bridge already wrote explicitly - so a fixed
 * constant seed is fine; this exists purely to satisfy the schema, not to drive real generation.
 */
final class LevelDatWriter {

    private static final int LEGACY_ANVIL_VERSION = 19133;
    private static final long PLACEHOLDER_SEED = 0L;

    private LevelDatWriter() {}

    static void write(Path worldFolder, String levelName, int dataVersion, WorldProperties properties) throws IOException {
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
    }

    private static CompoundBinaryTag worldGenSettings() {
        return CompoundBinaryTag.builder()
                .putLong("seed", PLACEHOLDER_SEED)
                .putByte("generate_features", (byte) 1)
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
