package com.pocketworld.plugin;

import com.pocketworld.plugin.api.PocketWorldAPI;
import com.pocketworld.plugin.cache.UserCache;
import com.pocketworld.plugin.cache.WorldCache;
import com.pocketworld.plugin.command.CommandPocketWorld;
import com.pocketworld.plugin.command.CommandPocketWorldAdmin;
import com.pocketworld.plugin.command.CommandTheme;
import com.pocketworld.plugin.data.DataSource;
import com.pocketworld.plugin.data.config.ConfigFile;
import com.pocketworld.plugin.data.config.MessageFile;
import com.pocketworld.plugin.exceptions.DataSourceConnectionException;
import com.pocketworld.plugin.listener.ChatInputListener;
import com.pocketworld.plugin.listener.InventoryListener;
import com.pocketworld.plugin.listener.PlayerListener;
import com.pocketworld.plugin.listener.ProtectedItemListener;
import com.pocketworld.plugin.listener.ThemeCreationListener;
import com.pocketworld.plugin.listener.WorldAutoUnloadTracker;
import com.pocketworld.plugin.listener.WorldListener;
import com.pocketworld.plugin.runtime.PocketWorldCreationQueue;
import com.pocketworld.plugin.runtime.PocketWorldRuntime;
import com.pocketworld.plugin.runtime.bridge.BridgeSelector;
import com.pocketworld.plugin.runtime.bridge.WorldRuntimeBridge;
import com.pocketworld.plugin.theme.ThemeRegistry;
import com.pocketworld.plugin.util.ChatInputRegistry;
import com.pocketworld.plugin.util.MessageReader;
import com.pocketworld.plugin.util.PocketDebugger;
import com.pocketworld.plugin.util.PocketUtils;
import com.pocketworld.slime.storage.WorldLoader;
import com.pocketworld.slime.storage.loader.file.FileWorldLoader;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

public final class PocketWorldPlugin extends JavaPlugin {

    private ConfigFile configFile;
    private MessageFile messageFile;
    private MessageReader messageReader;
    private PocketDebugger debugger;
    private ChatInputRegistry chatInputRegistry;

    private PocketWorldRuntime runtime;
    private PocketWorldRuntime themeRuntime;
    private final PocketWorldCreationQueue creationQueue = new PocketWorldCreationQueue();

    /** Admins with {@code /pocketworldadmin bypass} currently toggled on - session-only, never
     *  persisted. Read/written only from the main thread (commands and the block/interact events
     *  that check it are both always main-thread), so a plain {@link HashSet} is safe here. */
    private final Set<UUID> bypassingAdmins = new HashSet<>();

    private DataSource dataSource;
    private WorldCache worldCache;
    private UserCache userCache;
    private ThemeRegistry themeRegistry;

    @Override
    public void onEnable() {
        configFile = new ConfigFile(this);
        messageFile = new MessageFile(this);
        messageReader = new MessageReader(messageFile);

        debugger = new PocketDebugger(this);
        debugger.setDebugMode(configFile.getYaml().getBoolean("debug", false));

        chatInputRegistry = new ChatInputRegistry();

        try {
            WorldLoader worldStorage = new FileWorldLoader(getDataFolder().toPath().resolve("worlds"));
            WorldLoader themeStorage = new FileWorldLoader(getDataFolder().toPath().resolve("themes"));

            WorldRuntimeBridge bridge = new BridgeSelector(this).current();
            runtime = new PocketWorldRuntime(worldStorage, bridge);
            themeRuntime = new PocketWorldRuntime(themeStorage, bridge);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to initialize PocketWorld's storage/runtime layer", e);
        }

        sweepOrphanedEditorWorlds();

        dataSource = new DataSource(this);
        try {
            dataSource.connect();
        } catch (DataSourceConnectionException e) {
            getLogger().severe("Failed to connect to PocketWorld's metadata data source: " + e.getMessage());
            return;
        }

        worldCache = new WorldCache(this);
        userCache = new UserCache(this);

        themeRegistry = new ThemeRegistry(this);
        themeRegistry.load();

        WorldAutoUnloadTracker autoUnloadTracker = new WorldAutoUnloadTracker(this);
        getServer().getPluginManager().registerEvents(new ThemeCreationListener(this), this);
        getServer().getPluginManager().registerEvents(new ProtectedItemListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this, autoUnloadTracker), this);
        getServer().getPluginManager().registerEvents(new WorldListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(), this);
        getServer().getPluginManager().registerEvents(new ChatInputListener(this), this);

        getCommand("pocketworld").setExecutor(new CommandPocketWorld(this));
        getCommand("pocketworldadmin").setExecutor(new CommandPocketWorldAdmin(this));
        getCommand("theme").setExecutor(new CommandTheme(this));

        getServer().getServicesManager().register(PocketWorldAPI.class, PocketWorldAPI.create(this), this, ServicePriority.Normal);

        getLogger().info("PocketWorld " + getDescription().getVersion() + " enabled.");
    }

    /**
     * A theme's editor world (and, in principle, any pocket world) only ever exists as a folder
     * directly in the world container once {@code prepare()}/{@code activate()} has actually run
     * against a known stored id. If the server stops hard (crash, kill, power loss) while a theme
     * was mid-creation, nothing ever runs to clean that folder up - {@link ThemeCreationRegistry}
     * is in-memory only and is gone by the next boot, so there's no code path left that even knows
     * the folder exists. Sweeping for UUID-named folders that match neither a stored world nor a
     * stored theme catches exactly that case, safely: every legitimate folder here always matches
     * one or the other (that's what the warm cache is for), so anything left over is by definition
     * orphaned, not just still warm.
     */
    private void sweepOrphanedEditorWorlds() {
        Path worldContainer = getServer().getWorldContainer().toPath();
        if (!Files.isDirectory(worldContainer)) {
            return;
        }

        try {
            Set<String> known = new HashSet<>(runtime.list());
            known.addAll(themeRuntime.list());

            try (Stream<Path> children = Files.list(worldContainer)) {
                for (Path child : children.toList()) {
                    String name = child.getFileName().toString();
                    if (!PocketUtils.isUUID(name) || known.contains(name) || !Files.isDirectory(child)) {
                        continue;
                    }

                    deleteRecursively(child);
                    getLogger().info("Removed orphaned world folder \"" + name
                            + "\" left over from an interrupted theme creation (server didn't shut down cleanly last session).");
                }
            }
        } catch (IOException e) {
            getLogger().warning("Failed to sweep for orphaned editor worlds on startup: " + e);
        }
    }

    private static void deleteRecursively(Path folder) throws IOException {
        try (Stream<Path> paths = Files.walk(folder)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    @Override
    public void onDisable() {
        if (worldCache != null) {
            worldCache.flush();
        }
        if (userCache != null) {
            userCache.flush();
        }
        if (dataSource != null && dataSource.getConnection() != null) {
            dataSource.shutdown();
        }

        getLogger().info("PocketWorld disabled.");
    }

    public ConfigFile getConfigFile() {
        return configFile;
    }

    public MessageFile getMessageFile() {
        return messageFile;
    }

    public MessageReader getMessageReader() {
        return messageReader;
    }

    public PocketDebugger getDebugger() {
        return debugger;
    }

    public boolean isCreationQueueEnabled() {
        return configFile.getYaml().getBoolean("general.creation-queue-enabled", true);
    }

    /** Extra pause, in ticks, between one queued creation/load finishing and the next one starting. */
    public long getCreationQueueDelayTicks() {
        return configFile.getYaml().getInt("general.creation-queue-delay-seconds", 0) * 20L;
    }

    public ChatInputRegistry getChatInputRegistry() {
        return chatInputRegistry;
    }

    public PocketWorldRuntime getRuntime() {
        return runtime;
    }

    public PocketWorldRuntime getThemeRuntime() {
        return themeRuntime;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public WorldCache getWorldCache() {
        return worldCache;
    }

    public UserCache getUserCache() {
        return userCache;
    }

    public ThemeRegistry getThemeRegistry() {
        return themeRegistry;
    }

    public PocketWorldCreationQueue getCreationQueue() {
        return creationQueue;
    }

    public boolean isBypassing(UUID playerId) {
        return bypassingAdmins.contains(playerId);
    }

    /** Flips the given player's bypass state and returns the new state. */
    public boolean toggleBypass(UUID playerId) {
        if (!bypassingAdmins.remove(playerId)) {
            bypassingAdmins.add(playerId);
            return true;
        }
        return false;
    }
}
