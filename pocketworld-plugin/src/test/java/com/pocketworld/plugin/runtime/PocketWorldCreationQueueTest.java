package com.pocketworld.plugin.runtime;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PocketWorldCreationQueueTest {

    private static final PocketWorldCreationQueue.PositionListener IGNORE = position -> {};

    @Test
    void firstJobRunsImmediatelyAtPositionZero() {
        PocketWorldCreationQueue queue = new PocketWorldCreationQueue();
        List<String> started = new ArrayList<>();

        int position = queue.enqueue(onComplete -> {
            started.add("a");
            onComplete.run();
        }, IGNORE);

        assertEquals(0, position);
        assertEquals(List.of("a"), started);
    }

    @Test
    void secondJobWaitsUntilFirstCallsOnComplete() {
        PocketWorldCreationQueue queue = new PocketWorldCreationQueue();
        List<String> started = new ArrayList<>();
        List<Runnable> heldCompletion = new ArrayList<>();

        int firstPosition = queue.enqueue(onComplete -> {
            started.add("first");
            heldCompletion.add(onComplete);
        }, IGNORE);
        int secondPosition = queue.enqueue(onComplete -> {
            started.add("second");
            onComplete.run();
        }, IGNORE);

        assertEquals(0, firstPosition);
        assertEquals(1, secondPosition);
        // second must not have started yet - only "first" is in the started list.
        assertEquals(List.of("first"), started);

        heldCompletion.get(0).run();

        assertEquals(List.of("first", "second"), started);
    }

    @Test
    void manyQueuedJobsRunInOrderOneAtATime() {
        PocketWorldCreationQueue queue = new PocketWorldCreationQueue();
        List<Integer> started = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            int index = i;
            queue.enqueue(onComplete -> {
                started.add(index);
                assertTrue(started.size() == index + 1, "jobs must start strictly in submission order");
                onComplete.run();
            }, IGNORE);
        }

        assertEquals(List.of(0, 1, 2, 3, 4), started);
    }

    @Test
    void aFailingJobThatStillCallsOnCompleteDoesNotBlockTheQueue() {
        PocketWorldCreationQueue queue = new PocketWorldCreationQueue();
        List<String> started = new ArrayList<>();

        queue.enqueue(onComplete -> {
            started.add("failed");
            onComplete.run(); // simulates a job that hit an error but still released the queue
        }, IGNORE);
        queue.enqueue(onComplete -> {
            started.add("next");
            onComplete.run();
        }, IGNORE);

        assertEquals(List.of("failed", "next"), started);
    }

    @Test
    void queueCanBeReusedAfterDrainingCompletely() {
        PocketWorldCreationQueue queue = new PocketWorldCreationQueue();
        List<String> started = new ArrayList<>();

        queue.enqueue(onComplete -> {
            started.add("first");
            onComplete.run();
        }, IGNORE);

        int position = queue.enqueue(onComplete -> {
            started.add("second");
            onComplete.run();
        }, IGNORE);

        assertEquals(0, position, "queue should be empty again once the first job fully completed");
        assertEquals(List.of("first", "second"), started);
    }

    @Test
    void positionListenerIsNotifiedOnEnqueueWhileWaiting() {
        PocketWorldCreationQueue queue = new PocketWorldCreationQueue();
        List<Integer> secondPositions = new ArrayList<>();
        List<Integer> thirdPositions = new ArrayList<>();

        queue.enqueue(onComplete -> { /* holds the queue open - never completes in this test */ }, IGNORE);
        queue.enqueue(onComplete -> {}, secondPositions::add);
        queue.enqueue(onComplete -> {}, thirdPositions::add);

        assertEquals(List.of(1), secondPositions, "second entry is 1 behind the active job at enqueue time");
        assertEquals(List.of(2), thirdPositions, "third entry is 2 behind the active job at enqueue time");
    }

    @Test
    void positionListenerIsRenotifiedAsEarlierEntriesFinish() {
        PocketWorldCreationQueue queue = new PocketWorldCreationQueue();
        List<Runnable> heldCompletions = new ArrayList<>();
        List<Integer> lastPositions = new ArrayList<>();

        queue.enqueue(heldCompletions::add, IGNORE);
        queue.enqueue(onComplete -> heldCompletions.add(onComplete), lastPositions::add);
        queue.enqueue(onComplete -> heldCompletions.add(onComplete), IGNORE);

        assertEquals(List.of(1), lastPositions, "starts at position 1 (behind the currently-active job)");

        heldCompletions.get(0).run(); // active job finishes - the middle entry now starts, "last" moves up

        assertEquals(List.of(1, 0), lastPositions, "moves to position 0 once the entry ahead of it starts running");
    }

    @Test
    void immediateStartDoesNotNotifyItsOwnListener() {
        PocketWorldCreationQueue queue = new PocketWorldCreationQueue();
        List<Integer> positions = new ArrayList<>();

        queue.enqueue(onComplete -> onComplete.run(), positions::add);

        assertTrue(positions.isEmpty(), "an entry that starts immediately never needed a queued notification - callers already know from the returned position");
    }
}
