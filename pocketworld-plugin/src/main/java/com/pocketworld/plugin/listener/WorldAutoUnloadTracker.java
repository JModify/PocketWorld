package com.pocketworld.plugin.listener;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.world.PocketWorld;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unloads pocket worlds a configurable grace period after their last online member leaves, instead
 * of the instant it happens. The original implementation's equivalent method was left dead (never
 * called from any listener) with a comment noting per-disconnect unloading was "way too server
 * intensive" - which was true given it unloaded immediately, with no debounce, so a player quickly
 * reconnecting (or a second member logging in moments later) would still cause a needless
 * unload/reload cycle. The grace period here fixes that without giving up automatic unloading
 * entirely: a world only actually unloads if it's STILL empty once the delay elapses.
 */
public final class WorldAutoUnloadTracker {

    private final PocketWorldPlugin plugin;
    private final Map<UUID, BukkitTask> pendingUnloads = new ConcurrentHashMap<>();

    public WorldAutoUnloadTracker(PocketWorldPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Call once per disconnect, with the quitting player's id: checks every currently-loaded world
     * for having gone empty. The quitting player's id must be passed and excluded explicitly - while
     * {@link org.bukkit.event.player.PlayerQuitEvent} handlers are running, {@code Bukkit.getPlayer()}
     * still returns them (removal from the online-player list happens only after the event finishes
     * firing, a well-known Bukkit quirk), so without this exclusion a world whose only online member
     * just quit would incorrectly look "still occupied" and never get scheduled for unload at all.
     */
    public void onPlayerQuit(UUID quittingPlayerId) {
        for (PocketWorld world : plugin.getWorldCache().getLoadedWorlds()) {
            scheduleIfEmpty(world, quittingPlayerId);
        }
    }

    /** Call once per join: cancels any pending unload for a world this player is a member of. */
    public void onPlayerJoin(UUID playerId) {
        for (UUID worldId : pendingUnloads.keySet()) {
            PocketWorld world = plugin.getWorldCache().getIfCached(worldId);
            if (world != null && world.getUsers().containsKey(playerId)) {
                cancelPending(worldId);
            }
        }
    }

    private void scheduleIfEmpty(PocketWorld world, UUID excludePlayerId) {
        UUID worldId = world.getId();
        if (pendingUnloads.containsKey(worldId) || hasOnlineMember(world, excludePlayerId)) {
            return;
        }

        int delaySeconds = plugin.getConfigFile().getYaml().getInt("general.auto-unload-delay-seconds", 60);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingUnloads.remove(worldId);

            // Re-check on the way out: someone may have joined during the grace period.
            PocketWorld current = plugin.getWorldCache().getIfCached(worldId);
            if (current != null && current.isLoaded() && !hasOnlineMember(current)) {
                current.unload(plugin, true);
            }
        }, delaySeconds * 20L);

        pendingUnloads.put(worldId, task);
    }

    private void cancelPending(UUID worldId) {
        BukkitTask task = pendingUnloads.remove(worldId);
        if (task != null) {
            task.cancel();
        }
    }

    private boolean hasOnlineMember(PocketWorld world) {
        return hasOnlineMember(world, null);
    }

    private boolean hasOnlineMember(PocketWorld world, UUID excludePlayerId) {
        return world.getUsers().keySet().stream()
                .filter(id -> !id.equals(excludePlayerId))
                .anyMatch(id -> Bukkit.getPlayer(id) != null);
    }
}
