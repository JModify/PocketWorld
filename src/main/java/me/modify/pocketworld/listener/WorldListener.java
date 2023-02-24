package me.modify.pocketworld.listener;

import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.world.SlimeWorld;
import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.util.PocketUtils;
import me.modify.pocketworld.world.PocketWorld;
import me.modify.pocketworld.cache.WorldCache;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.io.IOException;
import java.util.UUID;

public class WorldListener implements Listener {

    private final PocketWorldPlugin plugin;
    public WorldListener(PocketWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) {
/*        SlimeWorld world = plugin.getSlimeHook().getAPI().getWorld(event.getWorld().getName());
        if (world != null) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> unlockWorld(world));
        }*/
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        World world = event.getEntity().getWorld();
        String name = world.getName();

        if (PocketUtils.isUUID(name)) {

            WorldCache cache = plugin.getWorldCache();
            UUID uuid = UUID.fromString(name);

            PocketWorld pocketWorld = cache.readThrough(uuid);
            if (pocketWorld == null) {
                return;
            }

            if (!pocketWorld.isLoaded()) {
                return;
            }

            Entity entity = event.getEntity();
            if (entity instanceof Animals) {
                if (!pocketWorld.isAllowAnimals()) {
                    event.setCancelled(true);
                }
            }

            if (entity instanceof Monster) {
                if (!pocketWorld.isAllowMonsters()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private void unlockWorld(SlimeWorld world) {
        try {
            world.getLoader().unlockWorld(world.getName());
        } catch (IOException ex) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> unlockWorld(world), 100);
        } catch (UnknownWorldException ignored) {

        }
    }
}
