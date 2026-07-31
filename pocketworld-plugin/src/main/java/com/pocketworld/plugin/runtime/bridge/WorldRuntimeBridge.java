package com.pocketworld.plugin.runtime.bridge;

import com.pocketworld.plugin.runtime.WorldProperties;
import com.pocketworld.slime.model.SlimeWorldData;
import org.bukkit.World;

import java.io.IOException;
import java.nio.file.Path;
import java.util.OptionalInt;

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
 * <p>
 * A bridge may keep a warm, ready-to-{@link #activate} cache across a world's unload/reload cycle
 * within one server session (see {@link #cachedDataVersion}/{@link #afterUnload}) rather than
 * rebuilding it from scratch on every single load - the common case for a small, frequently
 * revisited pocket world is "nothing changed since it was last here a minute ago," and paying a
 * full decode-and-rewrite for that case is pure waste. Implementations that have nothing worth
 * caching (or can't safely tell whether cached state is still valid) are free to always report no
 * cache and rebuild every time; correctness must never depend on the cache being used.
 */
public interface WorldRuntimeBridge {

    /** Short, log-friendly identifier, e.g. "anvil-shadow" or "nms-26_2". */
    String name();

    /** Whether this bridge's expectations about the running server actually hold right now. */
    boolean isAvailable();

    /**
     * If this bridge already has on-disk/in-memory state for {@code worldName} that's still valid
     * (from a previous {@link #afterUnload} with {@code retain = true}, matching what's currently
     * in storage), returns its data version - {@link #prepare} may be skipped entirely and
     * {@link #activate} called directly. Empty means there's nothing usable cached; the normal
     * decode-then-{@link #prepare} path must run.
     */
    OptionalInt cachedDataVersion(String worldName);

    /** Writes whatever on-disk/in-memory state this bridge needs for {@code worldName}. Pure I/O - safe off the main thread. */
    void prepare(SlimeWorldData data, String worldName) throws IOException;

    /**
     * Activates the live Bukkit world for {@code worldName}, which must already have been
     * {@link #prepare}d (or have a valid {@link #cachedDataVersion}). Must run on the main thread.
     * {@code dataVersion} must match the value {@code prepare} was called with (or the cached one),
     * so any world-level metadata this bridge writes (a level.dat, for an Anvil-shadow-style bridge)
     * claims the same Minecraft version as the chunks themselves - mismatching them risks the server
     * treating already-out-of-date chunks as current and skipping the DataFixerUpper upgrade they need.
     */
    World activate(String worldName, int dataVersion, WorldProperties properties) throws IOException;

    /**
     * Reads a just-unloaded world's on-disk state back out as Slime data. {@code worldFolder} must
     * be the exact folder the live world reported via {@code getWorldFolder()} before it was
     * unloaded. Deliberately takes a folder, not a live {@link World}: reading while the world is
     * still live risks racing Paper's own async chunk-saving I/O, which does not necessarily finish
     * writing every chunk to disk synchronously within a plain {@code World.save()} call - confirmed
     * by a real in-game report of exactly that (one chunk out of several built moments apart came
     * back empty after creating a world from a theme, non-deterministically). {@code
     * Bukkit.unloadWorld(world, true)} is the one operation the whole Bukkit ecosystem already
     * depends on to guarantee a world's data is fully flushed before it's considered gone, so callers
     * unload first and only read the folder afterward - see {@link com.pocketworld.plugin.runtime.PocketWorldRuntime#unloadSync}.
     */
    SlimeWorldData extractUnloaded(Path worldFolder) throws IOException;

    /**
     * Called after a world has been unloaded. {@code worldFolder} is the live world's own
     * {@code getWorldFolder()}, captured before unloading - the caller-visible folder path isn't
     * necessarily derivable from the world name alone (Paper 26.2's internal per-dimension layout,
     * for instance, nests it under the primary world rather than at a predictable name-based path).
     * <p>
     * If {@code retain} is true, the unload was a successful save (an {@link #extract} of
     * {@code dataVersion} just ran against this exact on-disk state), so the bridge may keep
     * whatever lets it report a {@link #cachedDataVersion} next time instead of deleting everything
     * now. If false (discarding unsaved changes), any on-disk/in-memory state for this world must be
     * fully cleaned up - it cannot be trusted to still match storage.
     */
    void afterUnload(String worldName, Path worldFolder, boolean retain, int dataVersion) throws IOException;

    /**
     * Permanently removes any cached/on-disk state for {@code worldName}, regardless of whether it
     * was ever loaded this session - called when a world is being deleted outright, since a world
     * can be deleted while already unloaded (with a still-retained cache from an earlier session).
     */
    void evictCache(String worldName) throws IOException;
}
