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
 */
public interface WorldRuntimeBridge {

    /** Short, log-friendly identifier, e.g. "anvil-shadow" or "nms-26_2". */
    String name();

    /** Whether this bridge's expectations about the running server actually hold right now. */
    boolean isAvailable();

    /** Turns decoded Slime data into a live, loaded Bukkit world named {@code worldName}. */
    World materialize(SlimeWorldData data, String worldName, WorldProperties properties) throws IOException;

    /** Reads a currently-loaded world's live state back out as Slime data. Does not unload it. */
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
