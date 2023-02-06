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
public class LoadedWorldRegistry {

    @Getter
    private static final LoadedWorldRegistry instance = new LoadedWorldRegistry();

    @Getter private final Set<PocketWorld> worlds;
    public LoadedWorldRegistry() {
        this.worlds = new LinkedHashSet<>();
    }

    public PocketWorld getWorld(UUID worldId) {
        Optional<PocketWorld> possibleWorld = worlds.stream()
                .filter(world -> world.getId().equals(worldId)).findFirst();
        return possibleWorld.orElse(null);
    }

    public void add(PocketWorld world) {
        if (!containsWorld(world.getId())) {
            worlds.add(world);
        }
    }

    public void delete(UUID worldId) {
        Optional<PocketWorld> possibleWorld = worlds.stream()
                .filter(world -> world.getId().equals(worldId)).findFirst();
        possibleWorld.ifPresent(worlds::remove);
    }

    public void update(PocketWorld world) {
        if (containsWorld(world.getId())) {
            delete(world.getId());
            add(world);
        }
    }

    public boolean containsWorld(UUID worldId) {
        return worlds.stream().anyMatch(w -> w.getId().equals(worldId));
    }

    public void shutdown(PocketWorldPlugin plugin) {
        for (PocketWorld world : worlds) {
            world.unload(plugin, true);
        }
        worlds.clear();
    }

}
