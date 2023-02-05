package me.modify.pocketworld.world;

import lombok.Getter;
import me.modify.pocketworld.PocketWorldPlugin;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Represents the list of pocket worlds loaded by players on the server.
 */
public class PocketWorldRegistry {

    @Getter
    private static final PocketWorldRegistry instance = new PocketWorldRegistry();

    @Getter private final Set<PocketWorld> worlds;
    public PocketWorldRegistry() {
        this.worlds = new LinkedHashSet<>();
    }

    public PocketWorld getWorld(UUID worldId) {
        Optional<PocketWorld> possibleWorld = worlds.stream().filter(world -> world.getId().equals(worldId)).findFirst();
        return possibleWorld.orElse(null);
    }

    public void registerWorld(PocketWorld world) {
        if (!containsWorld(world.getId())) {
            worlds.add(world);
        }
    }

    public boolean containsWorld(UUID worldId) {
        return worlds.stream().anyMatch(w -> w.getId().equals(worldId));
    }

    public void shutdown(PocketWorldPlugin plugin) {
        for (PocketWorld world : worlds) {
            world.unloadWorld(plugin);
        }
        worlds.clear();
    }

}
