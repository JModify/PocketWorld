package com.pocketworld.plugin.runtime.bridge.anvil;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.util.VoidGenerator;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A small standby pool of already-{@code Bukkit.createWorld()}-ed, empty world folders ("slots"),
 * so a real pocket world's {@link AnvilShadowBridge#prepare}/{@link AnvilShadowBridge#activate} can
 * reuse one instead of always paying a brand-new folder's full first-touch cost - confirmed
 * empirically to be the dominant cost of creating a pocket world (several seconds, vs. well under a
 * second to reuse an already-touched folder, even one renamed to a name Bukkit has never seen this
 * session and even with completely different chunk content written to it while unloaded). See
 * docs/ARCHITECTURE.md for the measurements this is built on.
 * <p>
 * A slot is just a name (see {@link #SLOT_NAME_PREFIX} - deliberately not a valid UUID, so
 * {@code PocketWorldPlugin#sweepOrphanedEditorWorlds} already leaves idle slot folders alone with no
 * changes needed there) whose on-disk folder has already been through {@code Bukkit.createWorld()}
 * and an immediate unload at least once. {@link #claim} hands out an idle slot's actual folder path
 * (as reported by {@link World#getWorldFolder()} when it was warmed, which is the only reliable way
 * to know where a world's data really lives - Paper 26.2's own internal per-dimension layout nests it
 * somewhere other than the classic {@code <worldContainer>/<name>} path, confirmed empirically) for a
 * caller to write real chunk data into directly; {@link #consumeClaim} hands that same path back so
 * {@code activate()} can rename it into place before calling {@code Bukkit.createWorld()} under the
 * real target name. Every claim triggers replenishment of a fresh slot in the background, through the
 * same {@link com.pocketworld.plugin.runtime.PocketWorldCreationQueue} real creations go through, so
 * warming never competes with (or blocks) an actual player's request - a burst of creations that
 * outpaces replenishment just degrades to today's un-pooled behavior rather than failing.
 * <p>
 * Correctness never depends on a slot being available: every caller ({@link AnvilShadowBridge}) must
 * treat an empty {@link #claim} result as "fall back to the normal path," never as an error.
 */
final class AnvilSlotPool {

    private static final String SLOT_NAME_PREFIX = "_pocketworld_slot_";

    private final PocketWorldPlugin plugin;
    private final boolean enabled;
    private final int poolSize;

    /** Slot index -> its actual on-disk folder, once warmed and currently idle (unclaimed). */
    private final Map<Integer, Path> idleSlots = new ConcurrentHashMap<>();
    /** World name -> the slot folder claimed for it (removed from {@link #idleSlots}), until
     *  {@link #consumeClaim} hands it back to {@code activate()} or the claim is abandoned. */
    private final Map<String, Path> claims = new ConcurrentHashMap<>();
    /** Slot indices currently being (re)warmed, so a slot is never queued to warm twice at once. */
    private final Set<Integer> warming = ConcurrentHashMap.newKeySet();

    AnvilSlotPool(PocketWorldPlugin plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfigFile().getYaml().getBoolean("general.slot-pool-enabled", true);
        this.poolSize = Math.max(0, plugin.getConfigFile().getYaml().getInt("general.slot-pool-size", 3));

        if (enabled && poolSize > 0) {
            // Deferred one tick rather than warmed inline here: this constructor runs during the
            // enabling plugin's own onEnable(), and warming goes through the same creation queue and
            // scheduler real creations use - safer to let the plugin finish enabling first.
            Bukkit.getScheduler().runTask(plugin, this::warmAll);
        }
    }

    private void warmAll() {
        for (int i = 0; i < poolSize; i++) {
            warmSlot(i);
        }
    }

    /** Claims an idle slot's folder for {@code worldName}, or empty if the pool is disabled, not yet
     *  warmed, or temporarily exhausted by other in-flight claims. Also kicks off replenishment. */
    Optional<Path> claim(String worldName) {
        if (!enabled) {
            return Optional.empty();
        }

        for (Map.Entry<Integer, Path> entry : idleSlots.entrySet()) {
            int index = entry.getKey();
            Path folder = entry.getValue();
            if (idleSlots.remove(index, folder)) {
                claims.put(worldName, folder);
                plugin.getDebugger().info("[AnvilSlotPool] Claimed slot " + index + " for \"" + worldName + "\"");
                warmSlot(index);
                return Optional.of(folder);
            }
        }
        plugin.getDebugger().info("[AnvilSlotPool] No idle slot available for \"" + worldName
                + "\" - falling back to a fresh folder");
        return Optional.empty();
    }

    /** Hands back the folder {@link #claim} returned for {@code worldName}, if any, and forgets the
     *  claim - {@code activate()} calls this exactly once per {@code prepare()} that claimed a slot. */
    Optional<Path> consumeClaim(String worldName) {
        return Optional.ofNullable(claims.remove(worldName));
    }

    private void warmSlot(int index) {
        if (idleSlots.containsKey(index) || !warming.add(index)) {
            return;
        }

        plugin.getCreationQueue().enqueue(onComplete -> Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                warmSlotNow(index);
            } finally {
                warming.remove(index);
                onComplete.run();
            }
        }), position -> { });
    }

    private void warmSlotNow(int index) {
        String slotName = SLOT_NAME_PREFIX + index;
        WorldCreator creator = new WorldCreator(slotName)
                .environment(World.Environment.NORMAL)
                .generateStructures(false)
                .generator(new VoidGenerator());
        World world = Bukkit.createWorld(creator);
        if (world == null) {
            plugin.getLogger().warning("[AnvilSlotPool] Failed to warm slot " + index + " (\"" + slotName + "\")");
            return;
        }

        world.setKeepSpawnInMemory(false);
        Path folder = world.getWorldFolder().toPath();
        Bukkit.unloadWorld(world, false);
        idleSlots.put(index, folder);
        plugin.getDebugger().info("[AnvilSlotPool] Warmed slot " + index + " at " + folder);
    }
}
