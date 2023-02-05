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
import me.modify.pocketworld.util.ColorFormat;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
        propertyMap.setValue(SlimeProperties.SPAWN_X, worldSpawn.getX());
        propertyMap.setValue(SlimeProperties.SPAWN_Y, worldSpawn.getY());
        propertyMap.setValue(SlimeProperties.SPAWN_Z, worldSpawn.getZ());
        propertyMap.setValue(SlimeProperties.ALLOW_ANIMALS, allowAnimals);
        propertyMap.setValue(SlimeProperties.ALLOW_MONSTERS, allowMonsters);
        propertyMap.setValue(SlimeProperties.PVP, pvp);

        // TODO: Make this configurable
        propertyMap.setValue(SlimeProperties.DIFFICULTY, "normal");
        return propertyMap;
    }

    public void asyncLoadWorld(PocketWorldPlugin plugin, UUID loaderId, boolean shouldTeleport, boolean shouldNotify) {
        SlimePlugin slime = plugin.getSlimeHook().getAPI();
        SlimePropertyMap properties = getPropertyMap();

        PocketTheme theme = plugin.getThemeCache().getThemeByID(themeId);

        PocketWorldRegistry.getInstance().registerWorld(PocketWorld.this);

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

                                if (shouldTeleport) {
                                    teleport(bWorld, loader);
                                }

                                WorldBorder border = bWorld.getWorldBorder();
                                border.setCenter(0.0, 0.0);
                                border.setSize(worldSize);
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

    public void unloadWorld(PocketWorldPlugin plugin) {

        DAO dao = plugin.getDataSource().getConnection().getDAO();
        dao.updatePocketWorld(this);

        SlimePlugin slime = plugin.getSlimeHook().getAPI();
        SlimeLoader loader = slime.getLoader("mongo");

        World bWorld = Bukkit.getWorld(id.toString());

        if (bWorld == null) {
            plugin.getLogger().severe("Failed to unload world " + id.toString() + ". World is not loaded?");
            return;
        }

        // Teleport players who might be in this world to main world spawn.
        World defaultWorld = Bukkit.getWorlds().get(0);
        bWorld.getPlayers().stream().forEach(p -> p.teleport(defaultWorld.getSpawnLocation()));

        try {
            loader.unlockWorld(id.toString());
        } catch (UnknownWorldException | IOException e) {
            plugin.getLogger().severe("Failed to unload pocket world: " + id.toString());
            e.printStackTrace();
        }

        Bukkit.unloadWorld(bWorld, true);
    }

    public void setAllowAnimals(boolean state) {
        this.allowAnimals = state;

        World world = Bukkit.getWorld(id.toString());
        if (world == null) {
            return;
        }

        if (!state) {
            world.getEntitiesByClass(Animals.class).forEach(c -> c.setHealth(0));
        }
    }

    public void setAllowMonsters(boolean state) {
        this.allowMonsters = state;

        World world = Bukkit.getWorld(id.toString());
        if (world == null) {
            return;
        }

        if (!state) {
            world.getEntitiesByClass(Monster.class).forEach(c -> c.setHealth(0));
        }
    }

    public void setPvp(boolean state) {
        this.pvp = state;

        World world = Bukkit.getWorld(id.toString());
        if (world == null) {
            return;
        }

        world.setPVP(false);
    }
}
