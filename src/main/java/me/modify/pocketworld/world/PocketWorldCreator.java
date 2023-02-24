package me.modify.pocketworld.world;

import com.grinderwolf.swm.api.SlimePlugin;
import com.grinderwolf.swm.api.exceptions.*;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.theme.PocketTheme;
import me.modify.pocketworld.util.ColorFormat;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class PocketWorldCreator {

    private PocketWorldPlugin plugin;
    public PocketWorldCreator(PocketWorldPlugin plugin) {
        this.plugin = plugin;
    }

    public PocketWorld create(UUID creatorId, String worldName, UUID themeId) {
        // Set the default spawn point for this pocket world based on theme setup.
        PocketTheme theme = plugin.getThemeRegistry().getThemeByID(themeId);
        String[] parts = theme.getSpawnPoint().split(":");
        double x = Double.parseDouble(parts[0]);
        double y = Double.parseDouble(parts[1]);
        double z = Double.parseDouble(parts[2]);
        float yaw = Float.parseFloat(parts[3]);
        float pitch = Float.parseFloat(parts[4]);

        PocketWorld world = new PocketWorld(UUID.randomUUID(), worldName, theme.getIcon(), 0, new HashMap<>(),
                new HashMap<>(), theme.getBiome(), 100, new WorldSpawn(x, y, z, yaw, pitch), true, true, true, false);
        world.getUsers().put(creatorId, WorldRank.OWNER);
        return world;
    }

    public void generateWorldFromTheme(PocketWorldPlugin plugin, PocketWorld world, PocketTheme theme,
                                       UUID creatorId) {

        // Add world to world cache and update user world reference list.
        plugin.getWorldCache().add(world.getId(), world);
        plugin.getUserCache().readThrough(creatorId).addWorld(world.getId());

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
                                world.setWorldBorder();
                                bWorld.setSpawnLocation(world.getWorldSpawn().getBukkitLocation(bWorld));

                                // Wait 1 second then teleport player to world (not doing this would teleport
                                // player to some other world spawn for some reason)
                                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                    world.teleport(creator);
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


}
