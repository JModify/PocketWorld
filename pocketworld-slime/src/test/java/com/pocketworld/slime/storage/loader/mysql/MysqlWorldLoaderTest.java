package com.pocketworld.slime.storage.loader.mysql;

import com.pocketworld.slime.storage.WorldLoader;
import com.pocketworld.slime.storage.WorldLoaderContract;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;

/**
 * Runs the shared {@link WorldLoaderContract} against a real, ephemeral MySQL container, so this
 * exercises the actual SQL dialect (upsert syntax, LONGBLOB handling) rather than a stand-in.
 * Skips (does not fail) when no local Docker daemon is available.
 */
class MysqlWorldLoaderTest extends WorldLoaderContract {

    private static MySQLContainer<?> container;

    @BeforeAll
    static void startContainer() {
        Assumptions.assumeTrue(isDockerAvailable(), "Docker is not available - skipping MySQL storage backend tests");
        container = new MySQLContainer<>("mysql:8.0");
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
        String table = "worlds_" + System.nanoTime();
        return new MysqlWorldLoader(container.getJdbcUrl(), container.getUsername(), container.getPassword(), table);
    }
}
