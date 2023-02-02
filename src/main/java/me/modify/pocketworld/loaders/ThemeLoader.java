package me.modify.pocketworld.loaders;

import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.util.PocketDebugger;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.NotDirectoryException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents the theme loader related to ASWM
 */
public class ThemeLoader implements SlimeLoader {

    private static final FilenameFilter WORLD_FILE_FILTER = (dir, name) -> name.endsWith(".slime");

    private final Map<String, RandomAccessFile> templateFiles = Collections.synchronizedMap(new HashMap<>());
    
    private final File templateDir;
    private final PocketDebugger debugger;

    public ThemeLoader(PocketWorldPlugin plugin) {
        this.templateDir = new File(plugin.getDataFolder() + "/themes/");
        this.debugger = plugin.getDebugger();
    }

    @Override
    public byte[] loadWorld(String worldName, boolean readOnly) throws UnknownWorldException, IOException {
        if (!worldExists(worldName)) {
            throw new UnknownWorldException(worldName);
        }

        RandomAccessFile file = templateFiles.computeIfAbsent(worldName, (world) -> {
           try {
               debugger.sendDebugInfo("Created random access file " + templateDir.getName() + " during world load.");
               return new RandomAccessFile(new File(templateDir, worldName + ".slime"), "rw");
           } catch (FileNotFoundException ex) {
               return null;
           }
        });

        if (!readOnly) {
            if (file != null && file.getChannel().isOpen()) {
                debugger.sendDebugInfo("World  " + worldName + " unlocked.");
            }
        }

        if (file != null && file.length() > Integer.MAX_VALUE) {
            throw new IndexOutOfBoundsException("World is too big!");
        }

        byte[] serializedWorld = new byte[0];
        if (file != null) {
            serializedWorld = new byte[(int) file.length()];
            file.seek(0);
            file.readFully(serializedWorld);
            debugger.sendDebugInfo("World " + worldName + " serialized properly");
        }

        return serializedWorld;
    }

    @Override
    public boolean worldExists(String worldName) throws IOException {
        return new File(templateDir, worldName + ".slime").exists();
    }

    @Override
    public List<String> listWorlds() throws IOException {
        String[] worlds = templateDir.list(WORLD_FILE_FILTER);

        if (worlds == null) {
            throw new NotDirectoryException(templateDir.getPath());
        }

        return Arrays.stream(worlds).map((c) -> c.substring(0, c.length() - 6)).collect(Collectors.toList());
    }

    @Override
    public void saveWorld(String worldName, byte[] serializedWorld, boolean lock) throws IOException {
        RandomAccessFile worldFile = templateFiles.get(worldName);
        boolean tempFile = worldFile == null;

        if (tempFile) {
            worldFile = new RandomAccessFile(new File(templateDir, worldName + ".slime"), "rw");
        }

        worldFile.seek(0); // Make sure we're at the start of the file
        worldFile.setLength(0); // Delete old data
        worldFile.write(serializedWorld);

        if (lock) {
            FileChannel channel = worldFile.getChannel();

            try {
                channel.tryLock();
            } catch (OverlappingFileLockException ignored) {

            }
        }

        if (tempFile) {
            worldFile.close();
        }
    }

    @Override
    public void unlockWorld(String worldName) throws UnknownWorldException, IOException {
        if (!worldExists(worldName)) {
            throw new UnknownWorldException(worldName);
        }

        RandomAccessFile file = templateFiles.remove(worldName);

        if(file != null) {
            FileChannel channel = file.getChannel();
            if(channel.isOpen()) {
                file.close();
                debugger.sendDebugInfo("World  " + worldName + " successfully unlocked.");
            }
        }
    }

    @Override
    public boolean isWorldLocked(String worldName) throws IOException {
        RandomAccessFile file = templateFiles.get(worldName);

        if (file == null) {
            file = new RandomAccessFile(new File(templateDir, worldName + ".slime"), "rw");
        }

        if(file.getChannel().isOpen()) {
            file.close();
        }else{
            return true;
        }
        return false;
    }

    @Override
    public void deleteWorld(String worldName) throws UnknownWorldException, IOException {
        if (!worldExists(worldName)) {
            throw new UnknownWorldException(worldName);
        }else {
            try (RandomAccessFile randomAccessFile = templateFiles.get(worldName)) {
                debugger.sendDebugInfo("Deleting world.. " + worldName + ".");
                unlockWorld(worldName);

                FileUtils.forceDelete(new File(templateDir, worldName + ".slime"));
                if(randomAccessFile != null) {
                    debugger.sendDebugInfo("Attempting to delete worldData  " + worldName + ".");

                    randomAccessFile.seek(0); // Make sure we're at the start of the file
                    randomAccessFile.setLength(0); // Delete old data
                    randomAccessFile.write(null);
                    randomAccessFile.close();

                    templateFiles.remove(worldName);
                }
                debugger.sendDebugInfo("World " + worldName + " deleted.");
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }
}
