package me.modify.pocketworld.world;

import com.grinderwolf.swm.api.SlimePlugin;
import com.grinderwolf.swm.api.exceptions.CorruptedWorldException;
import com.grinderwolf.swm.api.exceptions.NewerFormatException;
import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.exceptions.WorldInUseException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import lombok.Getter;
import lombok.Setter;
import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.user.PocketUser;
import me.modify.pocketworld.util.ColorFormat;
import me.modify.pocketworld.util.MessageReader;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class PocketWorld implements Listener {

    /** ID of this PocketWorld */
    @Getter private final UUID id;

    /* Lock status of this world. Handled by SlimeWorldManager */
    @Getter @Setter private long locked;

    /** Users a part of this pocket world */
    @Getter private final Map<UUID, WorldRank> users;

    /** World owner creates this when creating their pocket world */
    @Getter private final String worldName;

    /** Size of this pocket world. Determines where world border is set */
    @Getter @Setter private int worldSize;

    /** Spawn for this pocket world. Modifiable world owner */
    @Getter private final WorldSpawn worldSpawn;

    /** Allow animals for this pocket world? Modifiable by world owner */
    @Getter private boolean allowAnimals;

    /** Allow monsters for this pocket world? Modifiable by world owner */
    @Getter private boolean allowMonsters;

    /** Allow pvp for this pocket world? Modifiable by world owner */
    @Getter private boolean pvp;

    /* Default biome for this world */
    @Getter private final String biome;

    /* Icon used to represent this world in menus. Copied from theme icon on creation. */
    @Getter private final Material icon;

    /** Indicates if this world is loaded */
    @Getter @Setter private boolean loaded;

    /** Invitations to this pocket world. key = recipient, value = sender */
    @Getter private final Map<UUID, UUID> invitations;

    public PocketWorld(UUID id, String worldName, Material icon, long locked, Map<UUID, WorldRank> users,
                       Map<UUID, UUID> invitations, String biome, int worldSize, WorldSpawn worldSpawn,
                       boolean allowAnimals, boolean allowMonsters, boolean pvp, boolean loaded) {
        this.id = id;
        this.locked = locked;
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
    }

    /**
     * Retrieve the Slime property map related to this world.
     * @param plugin main plugin instance
     * @return slime property map of this world's properties.
     */
    public SlimePropertyMap getPropertyMap(PocketWorldPlugin plugin) {
        SlimePropertyMap propertyMap = new SlimePropertyMap();

        propertyMap.setValue(SlimeProperties.SPAWN_X, (int) worldSpawn.getX());
        propertyMap.setValue(SlimeProperties.SPAWN_Y, (int) worldSpawn.getY());
        propertyMap.setValue(SlimeProperties.SPAWN_Z, (int) worldSpawn.getZ());

        // These values always true, allowAnimals and allowMonsters handled on CreatureSpawnEvent
        propertyMap.setValue(SlimeProperties.ALLOW_ANIMALS, true);
        propertyMap.setValue(SlimeProperties.ALLOW_MONSTERS, true);

        propertyMap.setValue(SlimeProperties.DEFAULT_BIOME, biome);
        propertyMap.setValue(SlimeProperties.PVP, pvp);

        String difficulty = plugin.getConfigFile().getYaml().getString("world-difficulty", "normal");
        if (!difficulty.equalsIgnoreCase("hard") && !difficulty.equalsIgnoreCase("easy")
                && !difficulty.equalsIgnoreCase("peaceful")) {
            plugin.getDebugger().warning("Configuration value for PocketWorld difficulty is invalid. " +
                    "Entry must be one of the following: peaceful, easy, normal, hard.");
            difficulty = "normal";
        }
        propertyMap.setValue(SlimeProperties.DIFFICULTY, difficulty);

        return propertyMap;
    }

    /**
     * Load this world on the server. Should not be used for creating/generating a new world, that should be done
     * through the PocketWorldCreator object.
     * @param plugin main plugin instance
     * @param loaderId id of user loading the world
     * @param shouldTeleport should the user should be teleported upon load completion.
     * @param shouldNotify should the user should be notified that the world has been loaded (and time it took).
     */
    public void load(PocketWorldPlugin plugin, UUID loaderId, boolean shouldTeleport, boolean shouldNotify) {
        SlimePlugin slime = plugin.getSlimeHook().getAPI();
        SlimePropertyMap properties = getPropertyMap(plugin);
        SlimeLoader mongoLoader = slime.getLoader(plugin.getDataSource().getSlimeLoaderName());

        // Asynchronously load the world
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    long start = System.currentTimeMillis();

                    // Check that the world exists, cannot load if the world is not created
                    if (!mongoLoader.worldExists(id.toString())) {
                        plugin.getLogger().severe("Failed to load world " + id.toString() + ". World does not exist.");
                        return;
                    }
                    SlimeWorld world = slime.loadWorld(mongoLoader, id.toString(), false, properties);

                    // Synchronously generate the world and perform post load actions.
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            slime.generateWorld(world);

                            long time = System.currentTimeMillis() - start;
                            Player loader = Bukkit.getPlayer(loaderId);
                            if (loader != null) {
                                if (shouldNotify) {
                                    plugin.getMessageReader().send("world-load-success", loader,
                                            "{TIME}:" + time);
                                }
                                // Grab the bukkit world, should never be null since load and generation passed.
                                World bukkitWorld = Bukkit.getWorld(id.toString());
                                if (bukkitWorld != null) {
                                    // Perform post load actions.
                                    setWorldBorder();
                                    setWorldSpawn(worldSpawn.getBukkitLocation(bukkitWorld));
                                    if (shouldTeleport) {
                                        plugin.getServer().getScheduler()
                                                .runTaskLater(plugin, () -> teleport(loader), 20L);
                                    }
                                }
                            }
                            setLoaded(true);
                            plugin.getLogger().info("Successfully loaded pocket world " + id + " in "
                                    + time + "ms!");
                        }

                    }.runTask(plugin);

                } catch (IOException | CorruptedWorldException | WorldInUseException
                         | NewerFormatException | UnknownWorldException e) {
                    plugin.getLogger().severe("Failed to load pocket world " + id + ".");
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Delete this world from the cache and data source. Unloads the world first if it is loaded.
     * @param plugin main plugin instance
     */
    public void delete(PocketWorldPlugin plugin) {
        // Unload the world without saving.
        unload(plugin, false);

        // Remove this world from the world cache.
        plugin.getWorldCache().remove(id);

        // Delete this world from the data source/
        SlimePlugin slime = plugin.getSlimeHook().getAPI();
        SlimeLoader loader = slime.getLoader(plugin.getDataSource().getSlimeLoaderName());
        try {
            loader.deleteWorld(id.toString());
        } catch (UnknownWorldException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Unloads this world from the server. Upon world unloading the following actions are performed:
     * - All entities in the world are killed excluding players and armorstands.
     * - Players which are inside the world are teleported out.
     * - The world is unlocked
     * - The respective bukkit world is unloaded.
     * - This world object is flagged as loaded being false.
     * @param plugin main plugin instance
     * @param save should the world be saved upon unloading.
     */
    public void unload(PocketWorldPlugin plugin, boolean save) {
        // If the world is not loaded, return.
        if (!loaded) return;

        World bWorld = Bukkit.getWorld(id.toString());
        if (bWorld == null) {
            plugin.getDebugger().severe("Failed to unload world " + id + ". " +
                    "Bukkit world for this world does not exist, world might be unloaded?");
            return;
        }

        // TODO: Make entity killing on world unload configurable.
        killEntities();

        // TODO: Make default world configurable
        // Teleport players who might be in this world to main world spawn.
        World defaultWorld = Bukkit.getWorlds().get(0);
        for (Player player : bWorld.getPlayers()) {
            player.teleport(defaultWorld.getSpawnLocation());
        }

        unlock(plugin);
        Bukkit.unloadWorld(bWorld, save);

        setLoaded(false);
    }

    /**
     * Unlocks the world. Necessary for a world to be saved.
     * Unlocking a world essentially disables it's "readOnly" property
     * @param plugin main plugin instance.
     */
    private void unlock(PocketWorldPlugin plugin) {
        SlimeLoader loader = plugin.getSlimeHook().getAPI().getLoader(plugin.getDataSource().getSlimeLoaderName());
        try {
            // Check that the loader under the given name exists and that the world is locked.
            if (loader != null && loader.isWorldLocked(id.toString())) {
                loader.unlockWorld(id.toString());
                plugin.getDebugger().severe("World is locked, unlocking...");
            }
        } catch (UnknownWorldException | IOException e) {
            throw new RuntimeException(e);
        }
        plugin.getDebugger().severe("Successfully unlocked world " + id.toString());
    }

    /**
     * Teleports an online player to this pocket world.
     * If the world is not loaded, nothing will be done.
     * @param player
     */
    public void teleport(Player player) {
        if (!loaded) return;

        World world = Bukkit.getWorld(id.toString());
        if (world == null) {
            return;
        }

        player.teleport(worldSpawn.getBukkitLocation(world));
    }

    /**
     * Sends an invitation to join this pocket world to the target from the sender.
     * @param plugin main plugin instance.
     * @param sender player sending the invite.
     * @param target target receiving the invite.
     */
    public void sendInvitation(PocketWorldPlugin plugin, Player sender, Player target) {
        invitations.put(target.getUniqueId(), sender.getUniqueId());

        PocketUser user = plugin.getUserCache().readThrough(target.getUniqueId());
        user.getInvitations().add(id);

        MessageReader reader = plugin.getMessageReader();

        // Announce to pocket world a new invitation.
        announce(reader.read("world-invite-sent", "{PLAYER}:" + sender.getName(),
                "{TARGET}:" + target.getName(), "{WORLD_NAME}:" + worldName));

        // Send invite receive message to target
        reader.send("world-invite-received", target, "{WORLD_NAME}:" + worldName);
    }

    /**
     * Revokes an invitation to this pocket world.
     * @param plugin main plugin instance
     * @param sender user revoking the invitation
     * @param targetId target being revoked an invite.
     */
    public void revokeInvitation(PocketWorldPlugin plugin, Player sender, UUID targetId) {
        invitations.remove(targetId);

        // Send revoke invitation message to all online world members.
        MessageReader reader = plugin.getMessageReader();
        announce(reader.read("world-invite-revoke",
                "{PLAYER}:" + sender.getName(),
                "{TARGET}:" + Bukkit.getOfflinePlayer(targetId).getName(),
                "{WORLD_NAME}:" + worldName));
    }

    /**
     * Announces a message to all online members for this pocket world.
     * Message is colorized using the ColorFormat class before sending.
     * @param message message to send, color codes supported.
     */
    public void announce(String message) {
        Set<UUID> userIds = users.keySet();
        for (UUID id : userIds) {
            Player player = Bukkit.getPlayer(id);

            if (player == null || !player.isOnline()) {
                continue;
            }

            player.sendMessage(ColorFormat.format(message));
        }
    }

    /**
     * Retrieves a (delimiter) separated list of the names of all members for this PocketWorld.
     * @param delimiter delimiter separating the names of all members for the string.
     * @return string of members names separated by the delimiter.
     */
    public String getMembersFormatted(String delimiter) {
        return users.keySet().stream()
                .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                .collect(Collectors.joining(delimiter));
    }

    /**
     * Retrieves a string representing the world size for this PocketWorld.
     * Eg. 100x100
     * @return formatted string for the world size of this world.
     */
    public String getWorldSizeFormatted() {
        return worldSize + "x" + worldSize;
    }

    /**
     * Sets whether animals can spawn in this world.
     * <p>
     * If state state is being set to FALSE:
     * This method will purge all currently living animals which may exist in the world (if world is loaded)
     * @param state whether animals can spawn
     */
    public void setAllowAnimals(boolean state) {
        this.allowAnimals = state;

        World world = Bukkit.getWorld(id.toString());
        if (world == null) {
            return;
        }
        // Purge animals which might exist in the loaded world if allowAnimals is false.
        if (!state) world.getEntitiesByClass(Animals.class).forEach(Entity::remove);
    }

    /**
     * Sets whether monsters can spawn in this world.
     * <p>
     * If state state is being set to FALSE:
     * This method will purge all currently living monsters which may exist in the world (if world is loaded)
     * @param state whether monsters can spawn
     */
    public void setAllowMonsters(boolean state) {
        this.allowMonsters = state;

        World world = Bukkit.getWorld(id.toString());
        if (world == null) {
            return;
        }
        // Purge monsters which might exist in the loaded world if allowMonsters is false.
        if (!state) world.getEntitiesByClass(Monster.class).forEach(Entity::remove);
    }

    /**
     * Sets whether pvp can occur in this world.
     * @param state whether pvp is allowed.
     */
    public void setPvp(boolean state) {
        this.pvp = state;

        World world = Bukkit.getWorld(id.toString());
        if (world == null) {
            return;
        }
        world.setPVP(state);
    }

    /**
     * Sets the world border to the world size of this pocket world.
     */
    public void setWorldBorder() {
        World world = Bukkit.getWorld(id.toString());
        if (world == null) {
            return;
        }

        WorldBorder border = world.getWorldBorder();
        border.setCenter(0.0, 0.0);
        border.setSize(worldSize);
    }

    /**
     * Set the Bukkit-World spawn location to the PocketWorld spawn location.
     * @param location location to set world spawn.
     */
    public void setWorldSpawn(Location location) {
        worldSpawn.setX(location.getX());
        worldSpawn.setY(location.getY());
        worldSpawn.setZ(location.getZ());
        worldSpawn.setYaw(location.getYaw());
        worldSpawn.setPitch(location.getPitch());
        location.getWorld().setSpawnLocation(location);
    }

    /**
     * Kills all entities in the pocket world excluding players and armor stands.
     */
    private void killEntities() {
        World world = Bukkit.getWorld(id.toString());
        if (world == null) {
            return;
        }

        // Kill all living entities with the exception of armor stands and players.
        List<LivingEntity> entities = world.getLivingEntities();
        for (LivingEntity entity : entities) {
            if (entity instanceof ArmorStand || entity instanceof Player) {
                continue;
            }
            entity.remove();
        }
    }
}
