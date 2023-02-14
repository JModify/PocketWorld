package me.modify.pocketworld.listener;

import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.util.PocketUtils;
import me.modify.pocketworld.world.PocketWorld;
import me.modify.pocketworld.world.PocketWorldCache;
import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.UUID;
import java.util.regex.Pattern;

public class WorldListener implements Listener {

    private final PocketWorldPlugin plugin;
    public WorldListener(PocketWorldPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        World world = event.getEntity().getWorld();
        String name = world.getName();

        if (PocketUtils.isUUID(name)) {

            PocketWorldCache cache = plugin.getWorldCache();
            UUID uuid = UUID.fromString(name);

            PocketWorld pocketWorld = cache.get(uuid);
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
}
