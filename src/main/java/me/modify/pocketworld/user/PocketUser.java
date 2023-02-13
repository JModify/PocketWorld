package me.modify.pocketworld.user;

import lombok.Getter;
import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.data.DAO;
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
    @Getter private String name;

    //TODO: Implement this. value = id of pocket world invited too
    /** Invitations to pocket worlds */
    @Getter private Set<UUID> invitations;

    public PocketUser(UUID id, String name, Set<UUID> worlds) {
        this.id = id;
        this.name = name;
        this.worlds = worlds;
    }

    public void addWorld(UUID id) {
        worlds.add(id);
    }

    public void removeWorld(UUID id) {
        worlds.remove(id);
    }

    public void update(PocketWorldPlugin plugin) {
        DAO dao = plugin.getDataSource().getConnection().getDAO();
        dao.updatePocketUser(this);
    }

    public boolean isInPocketWorld(PocketWorldPlugin plugin, UUID worldId) {
        Player player = Bukkit.getPlayer(id);
        if (player == null) {
            return false;
        }

        // If the world pocket world cannot be pulled using the cache get() method. It does not exist.
        PocketWorld world = plugin.getWorldCache().get(worldId);
        if (world == null) {
            return false;
        }

        if (!world.isLoaded()) {
            return false;
        }

        // If it exists, attempt to get it as a bukkit world. This will indicate whether it is loaded or not.
        World playerWorld = player.getLocation().getWorld();
        World pocketWorld = Bukkit.getWorld(worldId.toString());
        if (pocketWorld == null || playerWorld == null) {
            return false;
        }

        return playerWorld.getName().equals(pocketWorld.getName());
    }
}
