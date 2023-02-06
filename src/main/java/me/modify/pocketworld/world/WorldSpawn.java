package me.modify.pocketworld.world;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class WorldSpawn {

    @Getter @Setter double x;
    @Getter @Setter double y;
    @Getter @Setter double z;

    @Getter @Setter float yaw;
    @Getter @Setter float pitch;

    public WorldSpawn(double x, double y, double z, float yaw, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    @Override
    public String toString() {
        return String.format("%f:%f:%f:%f:%f", x, y, z, yaw, pitch);
    }

    public static WorldSpawn fromString(String spawn) {
        String[] parts = spawn.split(":");

        double x = 0, y = 100, z = 0;
        float yaw = 0, pitch = 0;

        try {
            x = Double.parseDouble(parts[0]);
            y = Double.parseDouble(parts[1]);
            z = Double.parseDouble(parts[2]);
            yaw = Float.parseFloat(parts[3]);
            pitch = Float.parseFloat(parts[4]);

        } catch (NumberFormatException ignored){}

        return new WorldSpawn(x, y, z, yaw, pitch);
    }

    public Location getBukkitLocation(World world) {
        return new Location(world, x, y, z, yaw, pitch);
    }
}
