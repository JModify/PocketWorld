package com.pocketworld.plugin.runtime.bridge;

import com.pocketworld.plugin.runtime.WorldProperties;
import com.pocketworld.slime.model.SlimeWorldData;
import org.bukkit.World;

import java.io.IOException;
import java.nio.file.Path;

/**
 * The seam between "a decoded Slime world" and "a live Bukkit {@link World}". Everything
 * version-specific about how a Minecraft server actually stores/loads chunks lives behind this
 * interface; the rest of the plugin only ever talks to a {@code WorldRuntimeBridge}, never to
 * NMS or a specific Minecraft version directly.
 * <p>
 * Split into an I/O phase ({@link #prepare}) and a main-thread activation phase
 * ({@link #activate}) rather than one combined call: {@code Bukkit.createWorld()} (or an NMS
 * bridge's equivalent) must run on the main thread, but the disk/network I/O that feeds it
 * shouldn't block that thread - which is exactly the kind of main-thread-blocking bug this
 * project's own research into the original codebase flagged as worth not repeating.
 */
public interface WorldRuntimeBridge {

    /** Short, log-friendly identifier, e.g. "anvil-shadow" or "nms-26_2". */
    String name();

    /** Whether this bridge's expectations about the running server actually hold right now. */
    boolean isAvailable();

    /** Writes whatever on-disk/in-memory state this bridge needs for {@code worldName}. Pure I/O - safe off the main thread. */
    void prepare(SlimeWorldData data, String worldName) throws IOException;

    /**
     * Activates the live Bukkit world for {@code worldName}, which must already have been
     * {@link #prepare}d. Must run on the main thread. {@code dataVersion} must match the value
     * {@code prepare} was called with, so any world-level metadata this bridge writes (a level.dat,
     * for an Anvil-shadow-style bridge) claims the same Minecraft version as the chunks themselves -
     * mismatching them risks the server treating already-out-of-date chunks as current and skipping
     * the DataFixerUpper upgrade they need.
     */
    World activate(String worldName, int dataVersion, WorldProperties properties) throws IOException;

    /** Reads a currently-loaded world's live state back out as Slime data. Does not unload it. Main-thread only. */
    SlimeWorldData extract(World world) throws IOException;

    /**
     * Cleans up whatever on-disk/in-memory state this bridge created for {@code worldName}, after
     * it's been unloaded. {@code worldFolder} is the live world's own {@code getWorldFolder()},
     * captured before unloading - the caller-visible folder path isn't necessarily derivable from
     * the world name alone (Paper 26.2's internal per-dimension layout, for instance, nests it
     * under the primary world rather than at a predictable name-based path).
     */
    void discard(String worldName, Path worldFolder) throws IOException;
}
