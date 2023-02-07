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
import me.modify.pocketworld.theme.PocketTheme;
import me.modify.pocketworld.user.PocketUser;
import me.modify.pocketworld.util.ColorFormat;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.util.*;

public class PocketWorld {

    /** ID of this PocketWorld */
    @Getter private UUID id;

    @Getter private UUID themeId;

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

    //TODO: Implement this - invitations of users to the pocket world. key = sender, value = recipient
    @Getter private Map<UUID, UUID> invitations;

    public PocketWorld(UUID id, UUID themeId, String worldName, long locked, Map<UUID, WorldRank> users, int worldSize,
                       WorldSpawn worldSpawn, boolean allowAnimals, boolean allowMonsters, boolean pvp) {
        this.id = id;
        this.themeId = themeId;
        this.locked = locked;
        this.users = users;
        this.worldSize = worldSize;
        this.worldSpawn = worldSpawn;
        this.allowAnimals = allowAnimals;
        this.allowMonsters = allowMonsters;
        this.pvp = pvp;
        this.worldName = worldName;
    }

    public static PocketWorld create(String worldName, UUID themeId) {
        return new PocketWorld(UUID.randomUUID(), themeId, worldName, 0, new HashMap<>(),
                100, new WorldSpawn(0, 100, 0, 0, 0), true, true, true);
    }

    private SlimePropertyMap getPropertyMap() {
        SlimePropertyMap propertyMap = new SlimePropertyMap();

        //TODO: fix bug where player's are spawned to this inaccurate location when first loading the world.
        // something to do with first world join, idk
        propertyMap.setValue(SlimeProperties.SPAWN_X, (int) worldSpawn.getX());
        propertyMap.setValue(SlimeProperties.SPAWN_Y, (int) worldSpawn.getY());
        propertyMap.setValue(SlimeProperties.SPAWN_Z, (int) worldSpawn.getZ());

        // These values always true, allowAnimals and allowMonsters handled on CreatureSpawnEvent
        propertyMap.setValue(SlimeProperties.ALLOW_ANIMALS, true);
        propertyMap.setValue(SlimeProperties.ALLOW_MONSTERS, true);

        propertyMap.setValue(SlimeProperties.PVP, pvp);

        // TODO: Make this configurable
        propertyMap.setValue(SlimeProperties.DIFFICULTY, "normal");
        return propertyMap;
    }

    public void load(PocketWorldPlugin plugin, UUID loaderId, boolean shouldTeleport, boolean shouldNotify) {
        SlimePlugin slime = plugin.getSlimeHook().getAPI();
        SlimePropertyMap properties = getPropertyMap();

        PocketTheme theme = plugin.getThemeCache().getThemeByID(themeId);

        LoadedWorldRegistry.getInstance().add(PocketWorld.this);

        if (theme == null) {
            plugin.getLogger().severe("Failed to load world " + id.toString() + ". Theme " + themeId.toString() +
                    " not found in cache.");
            return;
        }

        properties.setValue(SlimeProperties.DEFAULT_BIOME, "minecraft:" + theme.getBiome());

        SlimeLoader mongoLoader = slime.getLoader("mongo");
        // Asynchronously load and clone world
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    long start = System.currentTimeMillis();

                    SlimeWorld world;
                    // Check if the world for this domain already exists. If it does, just load it.
                    // If it does, just load it
                    if (mongoLoader.worldExists(id.toString())) {
                        plugin.getLogger().severe("World already exists, trying to load it.");
                        world = slime.loadWorld(mongoLoader, id.toString(), false, properties);
                    } else {
                        //If world for this domain does not exist, load from template and clone it.
                        plugin.getLogger().severe("World does not exist, copying from theme.");
                        SlimeLoader templateLoader = slime.getLoader("theme");
                        SlimeWorld original = slime.loadWorld(templateLoader, themeId.toString(), false, properties);
                        plugin.getLogger().severe("Clone world called");
                        world  = original.clone(id.toString(), mongoLoader);
                    }

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        plugin.getLogger().severe("Generate world called");
                        slime.generateWorld(world);

                        long time = System.currentTimeMillis() - start;

                        Player loader = Bukkit.getPlayer(loaderId);
                        if (loader != null) {

                            if (shouldNotify) {
                                loader.sendMessage(ColorFormat.format("&aWorld successfully loaded in " + time + "ms"));
                            }

                            World bWorld = Bukkit.getWorld(id.toString());

                            if (bWorld != null) {
                                WorldBorder border = bWorld.getWorldBorder();
                                border.setCenter(0.0, 0.0);
                                border.setSize(worldSize);

                                bWorld.setSpawnLocation(worldSpawn.getBukkitLocation(bWorld));

                                if (shouldTeleport) {
                                    teleport(bWorld, loader);
                                }
                            }
                        }

                        // Set world border, other post load actions.
                        plugin.getLogger().info("Successfully loaded pocket world " + id.toString() + " in " + time + "ms!");
                    });

                } catch (IOException | CorruptedWorldException | WorldInUseException
                         | NewerFormatException | UnknownWorldException | WorldAlreadyExistsException e) {
                    plugin.getLogger().info("Failed to load pocket world " + id.toString() + ".");
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void teleport(World world, Player player) {
        player.teleport(worldSpawn.getBukkitLocation(world));
    }

    public void unload(PocketWorldPlugin plugin, boolean save) {
        // If the world is not loaded, return.
        if (!LoadedWorldRegistry.getInstance().containsWorld(id)) {
            return;
        }

        SlimePlugin slime = plugin.getSlimeHook().getAPI();
        SlimeLoader loader = slime.getLoader("mongo");

        World bWorld = Bukkit.getWorld(id.toString());
        if (bWorld == null) {
            plugin.getLogger().severe("Failed to unload world " + id.toString() + ". World is not loaded?");
            return;
        }

        // Entities are killed when a world is unloaded (excludes player and armor stands).
        killEntities();

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
    }

    public void delete(PocketWorldPlugin plugin) {
        DAO dao = plugin.getDataSource().getConnection().getDAO();

        unload(plugin, false);

        // If the world was loaded remove from loaded worlds registry.
        LoadedWorldRegistry registry = LoadedWorldRegistry.getInstance();
        if (registry.containsWorld(id)) {
            registry.delete(id);
        }

        // Run deletion 2 seconds later to let world unload.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Delete the world file from data source
            SlimePlugin slime = plugin.getSlimeHook().getAPI();
            SlimeLoader loader = slime.getLoader("mongo");
            try {
                loader.deleteWorld(id.toString());
            } catch (UnknownWorldException | IOException e) {
                throw new RuntimeException(e);
            }
        }, 2 * 20L);

        // Update all users apart of the world so that they no longer have a reference to this world.
        for (UUID memberId : users.keySet()) {
            PocketUser user = dao.getPocketUser(memberId);
            user.removeWorld(id);
            user.update(plugin);
        }
    }

    public void update(PocketWorldPlugin plugin) {
        // World first updated in data source
        DAO dao = plugin.getDataSource().getConnection().getDAO();
        dao.updatePocketWorld(this);

        // If this world is loaded, update it in the LoadedWorldRegistry.
        LoadedWorldRegistry registry = LoadedWorldRegistry.getInstance();
        registry.update(this);
    }

    public String getWorldSizeFormatted() {
        return worldSize + "x" + worldSize;
    }

    public void setAllowAnimals(boolean state) {
        this.allowAnimals = state;

        // World will be null if the world is not loaded on the server.
        World world = Bukkit.getWorld(id.toString());
        if (world == null) {
            return;
        }

        // Remove all animals currently in the world.
        if (!state) {
            world.getEntitiesByClass(Animals.class).forEach(Entity::remove);
        }
    }

    public void setAllowMonsters(boolean state) {
        this.allowMonsters = state;

        // World will be null if the world is not loaded on the server.
        // If a world isn't loaded, no need to go further and purge entities.
        World world = Bukkit.getWorld(id.toString());
        if (world == null) {
            return;
        }

        // Remove all monsters currently in the world.
        if (!state) {
            world.getEntitiesByClass(Monster.class).forEach(Entity::remove);
        }
    }

    public void setPvp(boolean state) {
        this.pvp = state;

        // World will be null if the world is not loaded on the server.
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

        // World will be null if the world is not loaded on the server.
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

        List<LivingEntity> entities = world.getLivingEntities();
        for (LivingEntity entity : entities) {
            if (entity instanceof ArmorStand || entity instanceof Player) {
                continue;
            }

            entity.remove();
        }
    }
}
