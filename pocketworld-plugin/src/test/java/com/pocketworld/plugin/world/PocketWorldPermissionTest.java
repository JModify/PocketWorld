package com.pocketworld.plugin.world;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PocketWorldPermissionTest {

    private static PocketWorld worldWith(Map<UUID, WorldRank> users, Map<PermissionRank, Set<WorldAction>> permissions) {
        return new PocketWorld(UUID.randomUUID(), "Test World", Material.GRASS_BLOCK, users, new HashMap<>(),
                "minecraft:plains", PocketWorld.DEFAULT_WORLD_SIZE, new WorldSpawn(0, 64, 0, 0f, 0f),
                true, true, true, false, permissions);
    }

    @Test
    void ownerAlwaysHasPermissionRegardlessOfMatrix() {
        UUID ownerId = UUID.randomUUID();
        Map<UUID, WorldRank> users = new HashMap<>();
        users.put(ownerId, WorldRank.OWNER);

        Map<PermissionRank, Set<WorldAction>> permissions = new EnumMap<>(PermissionRank.class);
        permissions.put(PermissionRank.MOD, EnumSet.noneOf(WorldAction.class));
        permissions.put(PermissionRank.MEMBER, EnumSet.noneOf(WorldAction.class));
        permissions.put(PermissionRank.VISITOR, EnumSet.noneOf(WorldAction.class));

        PocketWorld world = worldWith(users, permissions);

        for (WorldAction action : WorldAction.values()) {
            assertTrue(world.hasPermission(ownerId, action));
        }
    }

    @Test
    void modDefaultsGrantInviteAndKickOnly() {
        UUID modId = UUID.randomUUID();
        Map<UUID, WorldRank> users = new HashMap<>();
        users.put(modId, WorldRank.MOD);

        PocketWorld world = worldWith(users, PocketWorld.defaultPermissions());

        assertTrue(world.hasPermission(modId, WorldAction.INVITE));
        assertTrue(world.hasPermission(modId, WorldAction.KICK));
        assertFalse(world.hasPermission(modId, WorldAction.SET_SPAWN));
        assertFalse(world.hasPermission(modId, WorldAction.BUILD));
    }

    @Test
    void memberGetsNothingByDefault() {
        UUID memberId = UUID.randomUUID();
        Map<UUID, WorldRank> users = new HashMap<>();
        users.put(memberId, WorldRank.MEMBER);

        PocketWorld world = worldWith(users, PocketWorld.defaultPermissions());

        for (WorldAction action : WorldAction.values()) {
            assertFalse(world.hasPermission(memberId, action));
        }
    }

    @Test
    void nonMemberIsTreatedAsVisitor() {
        UUID visitorId = UUID.randomUUID();
        Map<PermissionRank, Set<WorldAction>> permissions = PocketWorld.defaultPermissions();
        permissions.put(PermissionRank.VISITOR, EnumSet.of(WorldAction.INTERACT));

        PocketWorld world = worldWith(new HashMap<>(), permissions);

        assertTrue(world.hasPermission(visitorId, WorldAction.INTERACT));
        assertFalse(world.hasPermission(visitorId, WorldAction.BUILD));
    }
}
