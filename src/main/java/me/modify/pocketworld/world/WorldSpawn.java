package me.modify.pocketworld.world;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class WorldSpawn {

    @Getter int x;
    @Getter int y;
    @Getter int z;

    @Getter float yaw;
    @Getter float pitch;

    public WorldSpawn(int x, int y, int z, float yaw, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    @Override
    public String toString() {
        return String.format("%d:%d:%d:%f:%f", x, y, z, yaw, pitch);
    }

    public static WorldSpawn fromString(String spawn) {
        String[] parts = spawn.split(":");

        int x = 0, y = 100, z = 0;
        float yaw = 0, pitch = 0;

        try {
            x = Integer.parseInt(parts[0]);
            y = Integer.parseInt(parts[1]);
            z = Integer.parseInt(parts[2]);
            yaw = Float.parseFloat(parts[3]);
            pitch = Float.parseFloat(parts[4]);

        } catch (NumberFormatException ignored){}

        return new WorldSpawn(x, y, z, yaw, pitch);
    }

    public Location getBukkitLocation(World world) {
        return new Location(world, x, y, z, yaw, pitch);
    }
}
