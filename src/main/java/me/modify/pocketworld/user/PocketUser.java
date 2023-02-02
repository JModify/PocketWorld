package me.modify.pocketworld.user;

import lombok.Getter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PocketUser {

    /* UUID of the user */
    @Getter UUID id;

    /** References to pocket worlds the user is a part of */
    @Getter Set<UUID> worlds;

    @Getter String inventory;

    public PocketUser(UUID id, Set<UUID> worlds, String inventory) {
        this.id = id;
        this.worlds = worlds;
        this.inventory = inventory;
    }

    public void addWorld(UUID id) {
        worlds.add(id);
    }
}
