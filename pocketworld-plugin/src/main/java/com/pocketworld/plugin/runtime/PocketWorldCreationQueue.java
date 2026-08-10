package com.pocketworld.plugin.runtime;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Serializes pocket-world creation/loading server-wide: only one is ever in flight at a time,
 * everyone else waits their turn. {@code Bukkit.createWorld()} is unavoidably main-thread-blocking
 * with no async alternative (confirmed ~100ms per world); letting many players' requests finish
 * their async prepare work around the same moment and all land on the main thread in the same tick
 * is what turns that per-world cost into a multi-second server freeze under a burst of simultaneous
 * requests. A global FIFO queue caps the worst case to one world's cost at a time, naturally spread
 * out over however long each request's own async work takes.
 */
public final class PocketWorldCreationQueue {

    /** One creation/load attempt. Must call {@code onComplete} exactly once, on every outcome
     *  (success or failure) - otherwise every later-queued entry is stuck waiting forever. */
    public interface Job {
        void run(Runnable onComplete);
    }

    /** Notified with this entry's current position (0 = about to start) every time it changes -
     *  once at enqueue time, and again each time an earlier entry finishes. */
    public interface PositionListener {
        void onPositionChanged(int position);
    }

    private record QueuedEntry(Job job, PositionListener onPositionChanged) {}

    private final Deque<QueuedEntry> pending = new ArrayDeque<>();
    private boolean active = false;

    /**
     * Queues {@code job}, running it immediately if nothing else is in progress.
     *
     * @return how many entries are already ahead of this one (0 means it starts right away).
     */
    public synchronized int enqueue(Job job, PositionListener onPositionChanged) {
        int position = pending.size() + (active ? 1 : 0);
        pending.addLast(new QueuedEntry(job, onPositionChanged));
        if (!active) {
            runNext();
        } else {
            onPositionChanged.onPositionChanged(position);
        }
        return position;
    }

    private synchronized void runNext() {
        QueuedEntry entry = pending.pollFirst();
        if (entry == null) {
            active = false;
            return;
        }

        // Only tell this entry its turn has come if it was ever actually waiting - the very first
        // entry ever enqueued runs via this same method but was never "queued" from the caller's
        // perspective (enqueue() already told it 0 synchronously via its return value).
        boolean wasWaiting = active;
        active = true;
        if (wasWaiting) {
            entry.onPositionChanged().onPositionChanged(0);
        }

        notifyRemainingPositions();
        entry.job().run(this::onJobComplete);
    }

    private synchronized void onJobComplete() {
        runNext();
    }

    /** Every entry still waiting just moved one position closer to the front - tell each one. */
    private void notifyRemainingPositions() {
        int position = active ? 1 : 0;
        for (QueuedEntry entry : pending) {
            entry.onPositionChanged().onPositionChanged(position);
            position++;
        }
    }
}
