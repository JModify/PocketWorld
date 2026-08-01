package com.pocketworld.plugin.runtime;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PocketWorldCreationQueueTest {

    @Test
    void firstJobRunsImmediatelyAtPositionZero() {
        PocketWorldCreationQueue queue = new PocketWorldCreationQueue();
        List<String> started = new ArrayList<>();

        int position = queue.enqueue(onComplete -> {
            started.add("a");
            onComplete.run();
        });

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
        });
        int secondPosition = queue.enqueue(onComplete -> {
            started.add("second");
            onComplete.run();
        });

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
            });
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
        });
        queue.enqueue(onComplete -> {
            started.add("next");
            onComplete.run();
        });

        assertEquals(List.of("failed", "next"), started);
    }

    @Test
    void queueCanBeReusedAfterDrainingCompletely() {
        PocketWorldCreationQueue queue = new PocketWorldCreationQueue();
        List<String> started = new ArrayList<>();

        queue.enqueue(onComplete -> {
            started.add("first");
            onComplete.run();
        });

        int position = queue.enqueue(onComplete -> {
            started.add("second");
            onComplete.run();
        });

        assertEquals(0, position, "queue should be empty again once the first job fully completed");
        assertEquals(List.of("first", "second"), started);
    }
}
