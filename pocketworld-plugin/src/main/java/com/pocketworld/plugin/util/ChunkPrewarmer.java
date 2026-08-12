package com.pocketworld.plugin.util;

import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.CompletableFuture;

/**
 * Pre-warms a chunk before something else (typically a player teleport right after world creation)
 * would otherwise force it to load synchronously on the main thread. A chunk load's cost is
 * unavoidably paid somewhere; the point is moving it off the main thread when possible rather than
 * eating it inline with whatever triggered it.
 * <p>
 * Uses Paper's {@code World#getChunkAtAsync(int, int, boolean)} when it's actually present at
 * runtime, detected once via {@link MethodHandles} rather than called directly - this plugin also
 * runs on plain Spigot (see docs/COMPATIBILITY.md), where that method doesn't exist on {@link World}
 * at all, and a direct call compiled against paper-api would throw {@code NoSuchMethodError} the
 * first time this class loaded on a Spigot server. On Spigot, {@link #prewarm} just calls the plain
 * synchronous {@code getChunkAt} and invokes {@code onReady} immediately - see docs/ARCHITECTURE.md
 * §20 for why that's an acceptable cost now (well under a second once the caller has already pre-
 * seeded a known spawn - see {@code LevelDatWriter} - rather than the multi-second cost a virgin
 * world with no known spawn used to have).
 */
public final class ChunkPrewarmer {

    private static final MethodHandle GET_CHUNK_AT_ASYNC = resolveGetChunkAtAsync();

    private ChunkPrewarmer() {}

    /** @return whether the async path is actually available on this server (Paper). Exposed mainly
     *          for logging/diagnostics - callers don't need to branch on this themselves. */
    public static boolean isAsyncAvailable() {
        return GET_CHUNK_AT_ASYNC != null;
    }

    /**
     * Ensures the chunk at {@code (chunkX, chunkZ)} in {@code world} is loaded, then runs
     * {@code onReady} - asynchronously via Paper's API when available (in which case {@code onReady}
     * runs back on the main thread, matching Paper's own callback contract), or synchronously right
     * away on plain Spigot. Either way, {@code onReady} is only ever invoked once, and only ever on
     * the main thread.
     */
    public static void prewarm(Plugin plugin, World world, int chunkX, int chunkZ, Runnable onReady) {
        if (GET_CHUNK_AT_ASYNC == null) {
            world.getChunkAt(chunkX, chunkZ);
            onReady.run();
            return;
        }

        try {
            CompletableFuture<?> future = (CompletableFuture<?>) GET_CHUNK_AT_ASYNC.invoke(world, chunkX, chunkZ, true);
            future.thenRun(onReady);
        } catch (Throwable e) {
            plugin.getLogger().warning("[ChunkPrewarmer] Async pre-warm failed, falling back to synchronous: " + e);
            world.getChunkAt(chunkX, chunkZ);
            onReady.run();
        }
    }

    private static MethodHandle resolveGetChunkAtAsync() {
        try {
            return MethodHandles.publicLookup().findVirtual(World.class, "getChunkAtAsync",
                    MethodType.methodType(CompletableFuture.class, int.class, int.class, boolean.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            return null;
        }
    }
}
