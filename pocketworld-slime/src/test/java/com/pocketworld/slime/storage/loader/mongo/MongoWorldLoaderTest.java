package com.pocketworld.slime.storage.loader.mongo;

import com.pocketworld.slime.storage.WorldLoader;
import com.pocketworld.slime.storage.WorldLoaderContract;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MongoDBContainer;

/**
 * Runs the shared {@link WorldLoaderContract} against a real, ephemeral MongoDB container. Skips
 * (does not fail) when no local Docker daemon is available.
 */
class MongoWorldLoaderTest extends WorldLoaderContract {

    private static MongoDBContainer container;

    @BeforeAll
    static void startContainer() {
        Assumptions.assumeTrue(isDockerAvailable(), "Docker is not available - skipping MongoDB storage backend tests");
        container = new MongoDBContainer("mongo:7.0");
        container.start();
    }

    @AfterAll
    static void stopContainer() {
        if (container != null) {
            container.stop();
        }
    }

    private static boolean isDockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    protected WorldLoader createLoader() throws Exception {
        String bucket = "worlds_" + System.nanoTime();
        return new MongoWorldLoader(container.getConnectionString(), "pocketworld_test", bucket);
    }
}
