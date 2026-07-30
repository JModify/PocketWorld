package com.pocketworld.slime.storage.loader.file;

import com.pocketworld.slime.storage.WorldLoader;
import com.pocketworld.slime.storage.WorldLoaderContract;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;

class FileWorldLoaderTest extends WorldLoaderContract {

    @TempDir
    Path tempDir;

    @Override
    protected WorldLoader createLoader() throws Exception {
        return new FileWorldLoader(tempDir);
    }

    @Test
    void rejectsWorldIdsThatCouldEscapeTheStorageDirectory() {
        FileWorldLoader loader = (FileWorldLoader) this.loader;
        assertThrows(IllegalArgumentException.class, () -> loader.exists("../outside"));
        assertThrows(IllegalArgumentException.class, () -> loader.exists("a/b"));
        assertThrows(IllegalArgumentException.class, () -> loader.exists(""));
    }
}
