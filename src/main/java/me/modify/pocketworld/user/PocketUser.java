package me.modify.pocketworld.user;

import lombok.Getter;
import lombok.Setter;
import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.util.PocketUtils;
import me.modify.pocketworld.world.PocketWorld;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public class PocketUser {

    /* UUID of the user */
    @Getter private UUID id;

    /** References to pocket worlds the user is a part of */
    @Getter private Set<UUID> worlds;

    /** Username the user currently goes by */
    @Getter @Setter private String name;

    //TODO: Implement this. value = id of pocket world invited too
    /** IDs of PocketWorld's a user is invited too. */
    @Getter private Set<UUID> invitations;

    /**
     * Create a new Pocket user using the given parameters.
     * @param id uuid of the user.
     * @param name username of the user.
     * @param worlds reference list of the user's associated worlds (member, mod, owner)
     */
    public PocketUser(UUID id, String name, Set<UUID> invitations, Set<UUID> worlds) {
        this.id = id;
        this.name = name;
        this.invitations = invitations;
        this.worlds = worlds;
    }

    /**
     * Add a world to the user's world reference list.
     * @param id id of world to add.
     */
    public void addWorld(UUID id) {
        worlds.add(id);
    }

    /**
     * Remove a world from the user's world reference list.
     * @param id id of world to remove.
     */
    public void removeWorld(UUID id) {
        worlds.remove(id);
    }

    /**
     * Determines if a player is currently inside a pocket world under the given ID.
     * @param plugin plugin instance
     * @param worldId id of pocket world to check if player is inside of.
     * @return true if player is in this pocket world, else false.
     */
    public boolean isInPocketWorld(PocketWorldPlugin plugin, UUID worldId) {
        Player player = Bukkit.getPlayer(id);
        if (player == null) {
            return false;
        }

        // If the player's world is null or not a uuid, they are not in a pocket world.
        World playerWorld = player.getLocation().getWorld();
        if (playerWorld == null || !PocketUtils.isUUID(playerWorld.getName())) {
            return false;
        }

        // If the world pocket world cannot be pulled using the cache get() method. It does not exist.
        PocketWorld world = plugin.getWorldCache().readThrough(worldId);
        if (world == null) {
            return false;
        }

        // If the world is not loaded, player cannot be inside of it.
        if (!world.isLoaded()) {
            return false;
        }

        // If it exists, attempt to get it as a bukkit world. This will indicate whether it is loaded or not.
        World pocketWorld = Bukkit.getWorld(worldId.toString());
        if (pocketWorld == null) {
            return false;
        }

        // If world is loaded, and player's world equals this world, then player is in that pocket world.
        return playerWorld.getName().equals(pocketWorld.getName());
    }
}
