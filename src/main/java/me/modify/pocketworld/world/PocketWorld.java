package me.modify.pocketworld.world;

import com.grinderwolf.swm.api.SlimePlugin;
import com.grinderwolf.swm.api.exceptions.*;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import lombok.Getter;
import lombok.Setter;
import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.data.DAO;
import me.modify.pocketworld.data.DataSource;
import me.modify.pocketworld.theme.PocketTheme;
import me.modify.pocketworld.user.PocketUser;
import me.modify.pocketworld.util.ColorFormat;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.util.*;

public class PocketWorld implements Listener {

    /** ID of this PocketWorld */
    @Getter private UUID id;

    @Getter @Setter private long locked;

    /** Users a part of this pocket world */
    @Getter private Map<UUID, WorldRank> users;

    /** World owner creates this when creating their pocket world */
    @Getter private String worldName;

    /** Size of this pocket world. Determines where world border is set */
    @Getter @Setter private int worldSize;

    /** Spawn for this pocket world. Modifiable world owner */
    @Getter private WorldSpawn worldSpawn;

    /** Allow animals for this pocket world? Modifiable by world owner */
    @Getter private boolean allowAnimals;

    /** Allow monsters for this pocket world? Modifiable by world owner */
    @Getter private boolean allowMonsters;

    /** Allow pvp for this pocket world? Modifiable by world owner */
    @Getter private boolean pvp;

    @Getter private String biome;

    @Getter private Material icon;

    @Getter @Setter private boolean loaded;

    //TODO: Implement this - invitations of users to the pocket world. key = sender, value = recipient
    @Getter private Map<UUID, UUID> invitations;

    public PocketWorld(UUID id, String worldName, Material icon, long locked, Map<UUID, WorldRank> users, String biome, int worldSize,
                       WorldSpawn worldSpawn, boolean allowAnimals, boolean allowMonsters, boolean pvp, boolean loaded) {
        this.id = id;
        this.locked = locked;
        this.users = users;
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

    public static PocketWorld create(PocketWorldPlugin plugin, UUID creatorId, String worldName, UUID themeId) {
        // Set the default spawn point for this pocket world based on theme setup.
        PocketTheme theme = plugin.themeRegistry.getThemeByID(themeId);
        String[] parts = theme.getSpawnPoint().split(":");
        double x = Double.parseDouble(parts[0]);
        double y = Double.parseDouble(parts[1]);
        double z = Double.parseDouble(parts[2]);
        float yaw = Float.parseFloat(parts[3]);
        float pitch = Float.parseFloat(parts[4]);

        PocketWorld world = new PocketWorld(UUID.randomUUID(), worldName, theme.getIcon(), 0, new HashMap<>(),
                theme.getBiome(), 100, new WorldSpawn(x, y, z, yaw, pitch), true, true, true, false);
        world.getUsers().put(creatorId, WorldRank.OWNER);
        return world;
    }

    private SlimePropertyMap getPropertyMap() {
        SlimePropertyMap propertyMap = new SlimePropertyMap();

        propertyMap.setValue(SlimeProperties.SPAWN_X, (int) worldSpawn.getX());
        propertyMap.setValue(SlimeProperties.SPAWN_Y, (int) worldSpawn.getY());
        propertyMap.setValue(SlimeProperties.SPAWN_Z, (int) worldSpawn.getZ());

        // These values always true, allowAnimals and allowMonsters handled on CreatureSpawnEvent
        propertyMap.setValue(SlimeProperties.ALLOW_ANIMALS, true);
        propertyMap.setValue(SlimeProperties.ALLOW_MONSTERS, true);

        propertyMap.setValue(SlimeProperties.DEFAULT_BIOME, biome);
        propertyMap.setValue(SlimeProperties.PVP, pvp);

        // TODO: Make this configurable
        propertyMap.setValue(SlimeProperties.DIFFICULTY, "normal");
        return propertyMap;
    }

    public void load(PocketWorldPlugin plugin, UUID loaderId, boolean shouldTeleport, boolean shouldNotify) {

        SlimePlugin slime = plugin.getSlimeHook().getAPI();
        SlimePropertyMap properties = getPropertyMap();
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
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        slime.generateWorld(world);

                        long time = System.currentTimeMillis() - start;
                        Player loader = Bukkit.getPlayer(loaderId);
                        if (loader != null) {
                            if (shouldNotify) {
                                loader.sendMessage(ColorFormat.format("&aWorld successfully loaded in " + time + "ms"));
                            }
                            // Grab the bukkit world, should never be null since load and generation passed.
                            World bukkitWorld = Bukkit.getWorld(id.toString());
                            if (bukkitWorld != null) {
                                // Perform post load actions.
                                setWorldBorder(bukkitWorld);
                                setSpawnPoint(worldSpawn.getBukkitLocation(bukkitWorld));
                                if (shouldTeleport) {
                                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                        loader.teleport(worldSpawn.getBukkitLocation(bukkitWorld));
                                    }, 20L);
                                }
                            }
                        }

                        setLoaded(true);
                        plugin.getLogger().info("Successfully loaded pocket world " + id.toString() + " in "
                                + time + "ms!");
                    });

                } catch (IOException | CorruptedWorldException | WorldInUseException
                         | NewerFormatException | UnknownWorldException e) {
                    plugin.getLogger().severe("Failed to load pocket world " + id.toString() + ".");
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public static void createWorldFromTheme(PocketWorldPlugin plugin, PocketWorld world, PocketTheme theme,
                                            UUID creatorId) {
        plugin.getWorldCache().add(world);

        SlimePlugin slime = plugin.getSlimeHook().getAPI();
        SlimePropertyMap properties = world.getPropertyMap();
        SlimeLoader mongoLoader = slime.getLoader(plugin.getDataSource().getSlimeLoaderName());
        // Asynchronously clone and load world.
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    long start = System.currentTimeMillis();
                    UUID worldId = world.getId();

                    // Check if the world already exists, if it does, return
                    if (mongoLoader.worldExists(worldId.toString())) {
                        plugin.getLogger().severe("Attempted world creation for " + worldId +
                                " but this world already exists!");
                        return;
                    }

                    // Clone this new world from the theme world.
                    SlimeLoader templateLoader = slime.getLoader("theme");
                    SlimeWorld original = slime.loadWorld(templateLoader, theme.getId().toString(),
                            false, properties);
                    SlimeWorld slimeWorld  = original.clone(worldId.toString(), mongoLoader);

                    // Synchronously generate the world and perform post creation actions.
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        slime.generateWorld(slimeWorld);
                        long time = System.currentTimeMillis() - start;

                        // Check if the creator of the world is online.
                        Player creator = Bukkit.getPlayer(creatorId);
                        if (creator != null) {
                            creator.sendMessage(ColorFormat.format("&aWorld successfully created in " + time + "ms"));

                            // Grab the bukkit world, should never be null since load and generation passed.
                            World bWorld = Bukkit.getWorld(worldId.toString());
                            if (bWorld != null) {

                                // Perform post load actions.
                                world.setWorldBorder(bWorld);
                                bWorld.setSpawnLocation(world.getWorldSpawn().getBukkitLocation(bWorld));

                                // Wait 1 second then teleport player to world (not doing this would teleport
                                // player to some other world spawn for some reason)
                                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                    creator.teleport(world.getWorldSpawn().getBukkitLocation(bWorld));
                                }, 20L);
                            }
                        }

                        world.setLoaded(true);
                        plugin.getLogger().info("Successfully created pocket world " + world.getId().toString()
                                + " in " + time + "ms!");
                    });
                } catch (IOException | CorruptedWorldException | WorldInUseException
                         | NewerFormatException | UnknownWorldException | WorldAlreadyExistsException e) {
                    plugin.getLogger().info("Failed to create pocket world " + world.getId().toString() + ".");
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void delete(PocketWorldPlugin plugin) {
        unload(plugin, false);

        // If the world was loaded remove from loaded worlds registry.
        plugin.getWorldCache().remove(id);

        SlimePlugin slime = plugin.getSlimeHook().getAPI();
        SlimeLoader loader = slime.getLoader(plugin.getDataSource().getSlimeLoaderName());
        try {
            loader.deleteWorld(id.toString());
        } catch (UnknownWorldException | IOException e) {
            throw new RuntimeException(e);
        }

        // Update all users apart of the world so that they no longer have a reference to this world.
        DAO dao = plugin.getDataSource().getConnection().getDAO();
        for (UUID memberId : users.keySet()) {
            PocketUser user = dao.getPocketUser(memberId);
            user.removeWorld(id);
            user.update(plugin);
        }
    }

    public void unload(PocketWorldPlugin plugin, boolean save) {
        // If the world is not loaded, return.
        if (!loaded) return;

        SlimePlugin slime = plugin.getSlimeHook().getAPI();
        SlimeLoader loader = slime.getLoader(plugin.getDataSource().getSlimeLoaderName());

        World bWorld = Bukkit.getWorld(id.toString());
        if (bWorld == null) {
            plugin.getLogger().severe("Failed to unload world " + id.toString() + ". World is not loaded?");
            return;
        }
        // TODO: Make entity killing on world unload configurable.
        killEntities();

        // TODO: Make default world configurable
        // Teleport players who might be in this world to main world spawn.
        World defaultWorld = Bukkit.getWorlds().get(0);
        bWorld.getPlayers().forEach(p -> p.teleport(defaultWorld.getSpawnLocation()));
        try {
            loader.unlockWorld(id.toString());
        } catch (UnknownWorldException | IOException e) {
            plugin.getLogger().severe("Failed to unload pocket world: " + id.toString());
            e.printStackTrace();
        }
        Bukkit.unloadWorld(bWorld, save);
        setLoaded(false);
    }

    public void teleport(World world, Player player) {
        player.teleport(worldSpawn.getBukkitLocation(world));
    }

    private void setWorldBorder(World world) {
        WorldBorder border = world.getWorldBorder();
        border.setCenter(0.0, 0.0);
        border.setSize(worldSize);
    }

    public void setAllowAnimals(boolean state) {
        this.allowAnimals = state;

        World world = Bukkit.getWorld(id.toString());
        if (world == null) {
            return;
        }
        // Purge animals which might exist in the loaded world if allowAnimals is false.
        if (!state) world.getEntitiesByClass(Animals.class).forEach(Entity::remove);
    }

    public void setAllowMonsters(boolean state) {
        this.allowMonsters = state;

        World world = Bukkit.getWorld(id.toString());
        if (world == null) {
            return;
        }
        // Purge monsters which might exist in the loaded world if allowMonsters is false.
        if (!state) world.getEntitiesByClass(Monster.class).forEach(Entity::remove);
    }

    public void setPvp(boolean state) {
        this.pvp = state;

        World world = Bukkit.getWorld(id.toString());
        if (world == null) {
            return;
        }
        world.setPVP(state);
    }

    public void setSpawnPoint(Location location) {
        worldSpawn.setX(location.getX());
        worldSpawn.setY(location.getY());
        worldSpawn.setZ(location.getZ());
        worldSpawn.setYaw(location.getYaw());
        worldSpawn.setPitch(location.getPitch());

        World world = Bukkit.getWorld(id.toString());
        if (world == null) {
            return;
        }
        world.setSpawnLocation(location);
    }

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
