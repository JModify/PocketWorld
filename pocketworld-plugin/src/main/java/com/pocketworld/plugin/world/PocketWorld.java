package com.pocketworld.plugin.world;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.api.event.PocketWorldLoadEvent;
import com.pocketworld.plugin.api.event.PocketWorldUnloadEvent;
import com.pocketworld.plugin.user.PocketUser;
import com.pocketworld.plugin.util.ColorFormat;
import com.pocketworld.plugin.util.MessageReader;
import com.pocketworld.plugin.runtime.WorldProperties;
import com.pocketworld.slime.format.SlimeFormatException;
import com.pocketworld.slime.model.SlimeWorldData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Animals;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.io.IOException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A player-owned instanced world. Lifecycle (load/unload/delete) is now orchestrated entirely
 * through {@link com.pocketworld.plugin.runtime.PocketWorldRuntime} instead of SlimeWorldManager -
 * see that class for the I/O-vs-main-thread contract each step follows.
 */
public class PocketWorld implements Listener {

    /** World size newly created pocket worlds get - also used for the theme editor world's border, so a
     *  theme creator builds within the same bounds a player will actually get once a world is created from it. */
    public static final int DEFAULT_WORLD_SIZE = 100;

    private final UUID id;
    private final Map<UUID, WorldRank> users;
    private final String worldName;
    private int worldSize;
    private final WorldSpawn worldSpawn;
    private boolean allowAnimals;
    private boolean allowMonsters;
    private boolean pvp;
    private final String biome;
    private final Material icon;
    private boolean loaded;
    /** Invitations to this pocket world, keyed by recipient. */
    private final Map<UUID, Invitation> invitations;
    /** Which {@link WorldAction}s each non-owner {@link PermissionRank} is allowed to perform here. */
    private final Map<PermissionRank, Set<WorldAction>> permissions;

    public PocketWorld(UUID id, String worldName, Material icon, Map<UUID, WorldRank> users,
                        Map<UUID, Invitation> invitations, String biome, int worldSize, WorldSpawn worldSpawn,
                        boolean allowAnimals, boolean allowMonsters, boolean pvp, boolean loaded,
                        Map<PermissionRank, Set<WorldAction>> permissions) {
        this.id = id;
        this.users = users;
        this.invitations = invitations;
        this.worldSize = worldSize;
        this.biome = biome;
        this.worldSpawn = worldSpawn;
        this.allowAnimals = allowAnimals;
        this.allowMonsters = allowMonsters;
        this.pvp = pvp;
        this.worldName = worldName;
        this.icon = icon;
        this.loaded = loaded;
        this.permissions = permissions;
    }

    /**
     * The permission defaults a brand-new pocket world is created with: MOD keeps today's
     * hardcoded invite/kick access, MEMBER and VISITOR start with nothing extra (no build/break/
     * interact for visitors) - so existing behavior is unchanged until an owner opens the new
     * permissions menu and deliberately changes it.
     */
    public static Map<PermissionRank, Set<WorldAction>> defaultPermissions() {
        Map<PermissionRank, Set<WorldAction>> defaults = new EnumMap<>(PermissionRank.class);
        defaults.put(PermissionRank.VISITOR, EnumSet.noneOf(WorldAction.class));
        defaults.put(PermissionRank.MEMBER, EnumSet.noneOf(WorldAction.class));
        defaults.put(PermissionRank.MOD, EnumSet.of(WorldAction.INVITE, WorldAction.KICK));
        return defaults;
    }

    public UUID getId() {
        return id;
    }

    public Map<UUID, WorldRank> getUsers() {
        return users;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getWorldSize() {
        return worldSize;
    }

    public void setWorldSize(int worldSize) {
        this.worldSize = worldSize;
    }

    public WorldSpawn getWorldSpawn() {
        return worldSpawn;
    }

    public boolean isAllowAnimals() {
        return allowAnimals;
    }

    public boolean isAllowMonsters() {
        return allowMonsters;
    }

    public boolean isPvp() {
        return pvp;
    }

    public String getBiome() {
        return biome;
    }

    public Material getIcon() {
        return icon;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public Map<UUID, Invitation> getInvitations() {
        return invitations;
    }

    public Map<PermissionRank, Set<WorldAction>> getPermissions() {
        return permissions;
    }

    /**
     * Resolves whether {@code playerId} may perform {@code action} in this world right now. Owners
     * always pass, regardless of the configured matrix. Anyone else is checked against the
     * permission set for their {@link PermissionRank} - their {@link WorldRank} if they're a member
     * at any rank, or {@link PermissionRank#VISITOR} if they're not a member at all.
     */
    public boolean hasPermission(UUID playerId, WorldAction action) {
        WorldRank rank = users.get(playerId);
        if (rank == WorldRank.OWNER) {
            return true;
        }

        PermissionRank permissionRank = switch (rank) {
            case MOD -> PermissionRank.MOD;
            case MEMBER -> PermissionRank.MEMBER;
            case OWNER -> throw new IllegalStateException("unreachable");
            case null -> PermissionRank.VISITOR;
        };

        return permissions.getOrDefault(permissionRank, Set.of()).contains(action);
    }

    /** The world-instantiation parameters for this world's current spawn/pvp/difficulty settings. */
    WorldProperties toWorldProperties(PocketWorldPlugin plugin) {
        String difficulty = plugin.getConfigFile().getYaml().getString("world-difficulty", "normal");
        if (!difficulty.equalsIgnoreCase("hard") && !difficulty.equalsIgnoreCase("easy")
                && !difficulty.equalsIgnoreCase("peaceful") && !difficulty.equalsIgnoreCase("normal")) {
            plugin.getDebugger().warning("Configuration value for PocketWorld difficulty is invalid. " +
                    "Entry must be one of the following: peaceful, easy, normal, hard.");
            difficulty = "normal";
        }

        return new WorldProperties(worldSpawn.x(), worldSpawn.y(), worldSpawn.z(),
                worldSpawn.yaw(), worldSpawn.pitch(), difficulty, pvp);
    }

    /**
     * Loads this world on the server. Should not be used for creating/generating a new world - that
     * goes through {@link PocketWorldCreator}. Safe to call from any thread: the storage read and
     * bridge preparation happen off the main thread, and only the final activation step (which
     * touches live Bukkit world state) is scheduled back onto it.
     *
     * @param loaderId id of user loading the world
     * @param shouldTeleport should the user be teleported upon load completion
     * @param shouldNotify should the user be notified that the world has loaded (and how long it took)
     */
    public void load(PocketWorldPlugin plugin, UUID loaderId, boolean shouldTeleport, boolean shouldNotify) {
        if (loaded) {
            return;
        }

        if (!plugin.isCreationQueueEnabled()) {
            loadNow(plugin, loaderId, shouldTeleport, shouldNotify, () -> {});
            return;
        }

        int position = plugin.getCreationQueue().enqueue(
                onComplete -> loadNow(plugin, loaderId, shouldTeleport, shouldNotify, onComplete),
                newPosition -> notifyQueuePosition(plugin, loaderId, newPosition));

        if (position > 0) {
            Player loader = Bukkit.getPlayer(loaderId);
            if (loader != null) {
                plugin.getMessageReader().send("world-load-queued", loader, "{POSITION}:" + position);
            }
        }
    }

    private void loadNow(PocketWorldPlugin plugin, UUID loaderId, boolean shouldTeleport, boolean shouldNotify, Runnable onComplete) {
        long start = System.currentTimeMillis();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (!plugin.getRuntime().exists(id.toString())) {
                    plugin.getLogger().severe("Failed to load world " + id + ". World does not exist.");
                    onComplete.run();
                    return;
                }

                int dataVersion = plugin.getRuntime().prepareLoad(id.toString());

                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        World bukkitWorld = plugin.getRuntime().activate(id.toString(), dataVersion, toWorldProperties(plugin));

                        setWorldBorder();
                        setWorldSpawn(worldSpawn.getBukkitLocation(bukkitWorld));
                        setLoaded(true);
                        Bukkit.getPluginManager().callEvent(new PocketWorldLoadEvent(this, bukkitWorld));

                        long time = System.currentTimeMillis() - start;
                        Player loader = Bukkit.getPlayer(loaderId);
                        if (loader != null) {
                            if (shouldNotify) {
                                plugin.getMessageReader().send("world-load-success", loader, "{TIME}:" + time);
                            }
                            if (shouldTeleport) {
                                // Bukkit.createWorld() (called by activate() above) synchronously
                                // prepares the spawn area before returning, so the world is already
                                // ready to receive players by this point - no artificial delay needed.
                                teleport(loader);
                            }
                        }
                        plugin.getLogger().info("Successfully loaded pocket world " + id + " in " + time + "ms!");
                    } catch (IOException e) {
                        plugin.getLogger().severe("Failed to activate pocket world " + id + ": " + e);
                    } finally {
                        onComplete.run();
                    }
                });
            } catch (SlimeFormatException e) {
                // Distinguished from a generic IOException so both the admin log and the player see
                // "this data is actually corrupted" rather than a vague failure that looks retryable -
                // an admin can confirm with /pocketworldadmin validate world <id> and should restore
                // from backup, since there's nothing this plugin can automatically reconstruct.
                plugin.getLogger().severe("Pocket world " + id + " failed to load: its stored data is corrupted (" + e.getMessage() + "). This needs manual recovery, e.g. from a backup.");
                Player loader = Bukkit.getPlayer(loaderId);
                if (loader != null) {
                    plugin.getMessageReader().send("world-load-corrupted", loader);
                }
                onComplete.run();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to load pocket world " + id + ": " + e);
                onComplete.run();
            }
        });
    }

    private static void notifyQueuePosition(PocketWorldPlugin plugin, UUID playerId, int position) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            plugin.getMessageReader().sendActionBar("world-queue-position", player, "{POSITION}:" + position);
        }
    }

    /**
     * Deletes this world from the cache and storage, unloading it first if it's loaded. Must be
     * called on the main thread (it may synchronously unload a live world).
     */
    public void delete(PocketWorldPlugin plugin) {
        unload(plugin, false);
        plugin.getWorldCache().remove(id);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getRuntime().deleteStored(id.toString());
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to delete stored pocket world " + id + ": " + e);
            }
        });
    }

    /**
     * Unloads this world from the server: kills non-player/non-armorstand entities, teleports any
     * players out, extracts and unloads the live Bukkit world, then (if {@code save}) persists the
     * extracted data to storage asynchronously so the storage write never blocks the main thread.
     * <p>
     * Must be called on the main thread - it calls {@code Bukkit.unloadWorld} internally, which is
     * itself a main-thread-only operation.
     */
    public void unload(PocketWorldPlugin plugin, boolean save) {
        unload(plugin, save, true);
    }

    /**
     * Same as {@link #unload(PocketWorldPlugin, boolean)}, but lets the caller force the final
     * storage write to happen synchronously instead of being scheduled onto the async scheduler.
     * <p>
     * Must be {@code false} when called from {@code onDisable()} (directly or indirectly, e.g. via
     * {@link com.pocketworld.plugin.cache.WorldCache#flush()}): by the time a plugin's
     * {@code onDisable()} runs, Bukkit has already marked it disabled, and {@code
     * Bukkit.getScheduler().runTaskAsynchronously} throws {@code IllegalPluginAccessException}
     * immediately rather than queuing the task - silently discarding whatever data that task would
     * have written. Confirmed by a real server log: a world left loaded when the server shut down
     * had its data extracted successfully but never persisted, because the scheduling call itself
     * threw before the write could happen. Normal (non-shutdown) unloads should keep using {@code
     * true} so the storage write never blocks the main thread.
     */
    public void unload(PocketWorldPlugin plugin, boolean save, boolean asyncPersist) {
        if (!loaded) {
            return;
        }

        World bWorld = Bukkit.getWorld(id.toString());
        if (bWorld == null) {
            plugin.getDebugger().severe("Failed to unload world " + id + ". " +
                    "Bukkit world for this world does not exist, world might be unloaded?");
            return;
        }

        Bukkit.getPluginManager().callEvent(new PocketWorldUnloadEvent(this));

        killEntities();

        World defaultWorld = Bukkit.getWorlds().get(0);
        for (Player player : bWorld.getPlayers()) {
            player.teleport(defaultWorld.getSpawnLocation());
        }

        try {
            SlimeWorldData data = plugin.getRuntime().unloadSync(bWorld, id.toString(), save);
            setLoaded(false);

            if (data != null) {
                if (asyncPersist) {
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        try {
                            plugin.getRuntime().persist(id.toString(), data);
                        } catch (IOException e) {
                            plugin.getLogger().severe("Failed to persist pocket world " + id + " on unload: " + e);
                        }
                    });
                } else {
                    try {
                        plugin.getRuntime().persist(id.toString(), data);
                    } catch (IOException e) {
                        plugin.getLogger().severe("Failed to persist pocket world " + id + " on unload: " + e);
                    }
                }
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to unload pocket world " + id + ": " + e);
        }
    }

    /**
     * Teleports an online player to this pocket world. If the world is not loaded, nothing is done.
     */
    public void teleport(Player player) {
        if (!loaded) {
            return;
        }

        World world = Bukkit.getWorld(id.toString());
        if (world == null) {
            return;
        }

        player.teleport(worldSpawn.getBukkitLocation(world));
    }

    public void sendInvitation(PocketWorldPlugin plugin, Player sender, Player target) {
        invitations.put(target.getUniqueId(), new Invitation(sender.getUniqueId(), target.getUniqueId()));

        PocketUser user = plugin.getUserCache().readThrough(target.getUniqueId());
        user.getInvitations().add(id);

        MessageReader reader = plugin.getMessageReader();
        announce(reader.read("world-invite-sent", "{PLAYER}:" + sender.getName(),
                "{TARGET}:" + target.getName(), "{WORLD_NAME}:" + worldName));
        reader.send("world-invite-received", target, "{WORLD_NAME}:" + worldName);
    }

    public void revokeInvitation(PocketWorldPlugin plugin, Player sender, UUID targetId) {
        invitations.remove(targetId);

        MessageReader reader = plugin.getMessageReader();
        announce(reader.read("world-invite-revoke",
                "{PLAYER}:" + sender.getName(),
                "{TARGET}:" + Bukkit.getOfflinePlayer(targetId).getName(),
                "{WORLD_NAME}:" + worldName));
    }

    /** Announces a colorized message to all online members of this pocket world. */
    public void announce(String message) {
        Set<UUID> userIds = users.keySet();
        for (UUID userId : userIds) {
            Player player = Bukkit.getPlayer(userId);
            if (player == null || !player.isOnline()) {
                continue;
            }

            player.sendMessage(ColorFormat.format(message));
        }
    }

    public String getMembersFormatted(String delimiter) {
        return users.keySet().stream()
                .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                .collect(Collectors.joining(delimiter));
    }

    public String getWorldSizeFormatted() {
        return worldSize + "x" + worldSize;
    }

    /** Sets whether animals can spawn; purges any living animals from the loaded world if disabled. */
    public void setAllowAnimals(boolean state) {
        this.allowAnimals = state;

        World world = Bukkit.getWorld(id.toString());
        if (world == null) {
            return;
        }
        if (!state) {
            world.getEntitiesByClass(Animals.class).forEach(Entity::remove);
        }
    }

    /** Sets whether monsters can spawn; purges any living monsters from the loaded world if disabled. */
    public void setAllowMonsters(boolean state) {
        this.allowMonsters = state;

        World world = Bukkit.getWorld(id.toString());
        if (world == null) {
            return;
        }
        if (!state) {
            world.getEntitiesByClass(Monster.class).forEach(Entity::remove);
        }
    }

    public void setPvp(boolean state) {
        this.pvp = state;

        World world = Bukkit.getWorld(id.toString());
        if (world == null) {
            return;
        }
        world.setPVP(state);
    }

    public void setWorldBorder() {
        World world = Bukkit.getWorld(id.toString());
        if (world == null) {
            return;
        }

        WorldBorder border = world.getWorldBorder();
        border.setCenter(0.0, 0.0);
        border.setSize(worldSize);
    }

    public void setWorldSpawn(Location location) {
        worldSpawn.setX(location.getX());
        worldSpawn.setY(location.getY());
        worldSpawn.setZ(location.getZ());
        worldSpawn.setYaw(location.getYaw());
        worldSpawn.setPitch(location.getPitch());
        location.getWorld().setSpawnLocation(location);
    }

    /** Kills all entities in the world except players and armor stands. */
    private void killEntities() {
        World world = Bukkit.getWorld(id.toString());
        if (world == null) {
            return;
        }

        List<LivingEntity> entities = world.getLivingEntities();
        for (LivingEntity entity : entities) {
            if (entity instanceof ArmorStand || entity instanceof Player) {
                continue;
            }
            entity.remove();
        }
    }
}
