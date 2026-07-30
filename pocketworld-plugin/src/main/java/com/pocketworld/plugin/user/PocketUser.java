package com.pocketworld.plugin.user;

import com.pocketworld.plugin.PocketWorldPlugin;
import com.pocketworld.plugin.util.PocketUtils;
import com.pocketworld.plugin.world.PocketWorld;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public class PocketUser {

    private final UUID id;
    private final Set<UUID> worlds;
    private String name;
    /** IDs of the PocketWorlds this user has a pending invitation to - a denormalized reverse index. */
    private final Set<UUID> invitations;

    public PocketUser(UUID id, String name, Set<UUID> invitations, Set<UUID> worlds) {
        this.id = id;
        this.name = name;
        this.invitations = invitations;
        this.worlds = worlds;
    }

    public UUID getId() {
        return id;
    }

    public Set<UUID> getWorlds() {
        return worlds;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<UUID> getInvitations() {
        return invitations;
    }

    public void addWorld(UUID id) {
        worlds.add(id);
    }

    public void removeWorld(UUID id) {
        worlds.remove(id);
    }

    /** Whether this user is currently physically standing inside the given (loaded) pocket world. */
    public boolean isInPocketWorld(PocketWorldPlugin plugin, UUID worldId) {
        Player player = Bukkit.getPlayer(id);
        if (player == null) {
            return false;
        }

        World playerWorld = player.getLocation().getWorld();
        if (playerWorld == null || !PocketUtils.isUUID(playerWorld.getName())) {
            return false;
        }

        PocketWorld world = plugin.getWorldCache().readThrough(worldId);
        if (world == null || !world.isLoaded()) {
            return false;
        }

        World pocketWorld = Bukkit.getWorld(worldId.toString());
        return pocketWorld != null && playerWorld.getName().equals(pocketWorld.getName());
    }
}
