package com.pocketworld.plugin.runtime;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Serializes pocket-world creation server-wide: only one creation is ever in flight at a time,
 * everyone else waits their turn. {@code Bukkit.createWorld()} is unavoidably main-thread-blocking
 * with no async alternative (confirmed ~100ms per world); letting many players' creations finish
 * their async prepare work around the same moment and all land on the main thread in the same tick
 * is what turns that per-world cost into a multi-second server freeze under a burst of simultaneous
 * requests. A global FIFO queue caps the worst case to one world's cost at a time, naturally spread
 * out over however long each creation's own async work takes.
 */
public final class PocketWorldCreationQueue {

    /** One creation attempt. Must call {@code onComplete} exactly once, on every outcome (success
     *  or failure) - otherwise every later-queued creation is stuck waiting forever. */
    public interface Job {
        void run(Runnable onComplete);
    }

    private final Deque<Job> pending = new ArrayDeque<>();
    private boolean active = false;

    /**
     * Queues {@code job}, running it immediately if nothing else is in progress.
     *
     * @return how many creations are already ahead of this one (0 means it starts right away).
     */
    public synchronized int enqueue(Job job) {
        int position = pending.size() + (active ? 1 : 0);
        pending.addLast(job);
        if (!active) {
            runNext();
        }
        return position;
    }

    private synchronized void runNext() {
        Job job = pending.pollFirst();
        if (job == null) {
            active = false;
            return;
        }
        active = true;
        job.run(this::onJobComplete);
    }

    private synchronized void onJobComplete() {
        runNext();
    }
}
