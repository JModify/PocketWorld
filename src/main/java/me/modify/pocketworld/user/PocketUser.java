package me.modify.pocketworld.user;

import lombok.Getter;
import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.data.DAO;
import me.modify.pocketworld.world.LoadedWorldRegistry;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PocketUser {

    /* UUID of the user */
    @Getter private UUID id;

    /** References to pocket worlds the user is a part of */
    @Getter private Set<UUID> worlds;

    @Getter private String inventory;

    //TODO: Implement this. value = id of pocket world invited too
    /** Invitations to pocket worlds */
    @Getter private Set<UUID> invitations;

    public PocketUser(UUID id, Set<UUID> worlds, String inventory) {
        this.id = id;
        this.worlds = worlds;
        this.inventory = inventory;
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

    public boolean isInPocketWorld(UUID worldId) {
        Player player = Bukkit.getPlayer(id);
        if (player == null) {
            return false;
        }

        LoadedWorldRegistry loadedWorldRegistry = LoadedWorldRegistry.getInstance();
        if (!loadedWorldRegistry.containsWorld(worldId)) {
            return false;
        }

        World playerWorld = player.getLocation().getWorld();
        World pocketWorld = Bukkit.getWorld(worldId.toString());

        if (pocketWorld == null || playerWorld == null) {
            return false;
        }

        return playerWorld.getName().equals(pocketWorld.getName());
    }
}
