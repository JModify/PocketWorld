package me.modify.pocketworld.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PocketLocation {

    UUID worldId;
    int x;
    int y;
    int z;
    float pitch;
    float yaw;

    public PocketLocation(UUID worldId, int x, int y, int z, float yaw, float pitch) {
        this.worldId = worldId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public void teleport(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        World world = Bukkit.getWorld(worldId);
        Location location = new Location(world, x, y, z, yaw, pitch);
        player.teleport(location);
    }

}
