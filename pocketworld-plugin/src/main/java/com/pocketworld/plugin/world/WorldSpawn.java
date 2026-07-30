package com.pocketworld.plugin.world;

import org.bukkit.Location;
import org.bukkit.World;

/** A world's configurable respawn point - mutable since owners can reset it in-game. */
public final class WorldSpawn {

    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;

    public WorldSpawn(double x, double y, double z, float yaw, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public double x() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double y() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double z() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public float yaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float pitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    @Override
    public String toString() {
        return String.format("%f:%f:%f:%f:%f", x, y, z, yaw, pitch);
    }

    public static WorldSpawn fromString(String spawn) {
        String[] parts = spawn.split(":");
        return new WorldSpawn(
                Double.parseDouble(parts[0]),
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Float.parseFloat(parts[3]),
                Float.parseFloat(parts[4]));
    }

    public Location getBukkitLocation(World bukkitWorld) {
        return new Location(bukkitWorld, x, y, z, yaw, pitch);
    }
}
