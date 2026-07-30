package com.pocketworld.plugin.runtime.bridge.anvil;

import com.pocketworld.plugin.runtime.WorldProperties;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Writes the minimal {@code level.dat} that makes Bukkit treat a folder as an existing world
 * (rather than a fresh one to generate) and tells it where to spawn. Fields confirmed against a
 * real Paper 26.2 (DataVersion 4903) level.dat; everything else (world-gen settings, game rules,
 * weather, raids, ...) is left for the server to synthesize via {@link org.bukkit.WorldCreator}'s
 * own parameters, the same way the Slime format itself never captured any of that either.
 */
final class LevelDatWriter {

    private static final int LEGACY_ANVIL_VERSION = 19133;

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
                .build();

        CompoundBinaryTag root = CompoundBinaryTag.builder().put("Data", data).build();

        BinaryTagIO.writer().write(root, worldFolder.resolve("level.dat"), BinaryTagIO.Compression.GZIP);
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
