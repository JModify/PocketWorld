package com.pocketworld.plugin.data.file;

import com.pocketworld.plugin.theme.PocketTheme;
import com.pocketworld.plugin.user.PocketUser;
import com.pocketworld.plugin.world.Invitation;
import com.pocketworld.plugin.world.PermissionRank;
import com.pocketworld.plugin.world.PocketWorld;
import com.pocketworld.plugin.world.WorldAction;
import com.pocketworld.plugin.world.WorldRank;
import com.pocketworld.plugin.world.WorldSpawn;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileDAOTest {

    @Test
    void pocketWorldRoundTripsThroughYaml(@TempDir Path tempDir) throws IOException {
        FileDAO dao = new FileDAO(tempDir);

        UUID ownerId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        Map<UUID, WorldRank> users = new HashMap<>();
        users.put(ownerId, WorldRank.OWNER);

        Map<UUID, Invitation> invitations = new HashMap<>();
        invitations.put(recipientId, new Invitation(ownerId, recipientId, 12345L));

        Map<PermissionRank, Set<WorldAction>> permissions = PocketWorld.defaultPermissions();
        permissions.put(PermissionRank.VISITOR, EnumSet.of(WorldAction.INTERACT));

        PocketWorld world = new PocketWorld(UUID.randomUUID(), "My World", Material.GRASS_BLOCK, users,
                invitations, "minecraft:plains", 150, new WorldSpawn(1.5, 65.0, -3.25, 90f, -10f),
                true, false, true, false, permissions);

        dao.registerPocketWorld(world);
        PocketWorld reloaded = dao.getPocketWorld(world.getId());

        assertEquals(world.getWorldName(), reloaded.getWorldName());
        assertEquals(world.getIcon(), reloaded.getIcon());
        assertEquals(world.getBiome(), reloaded.getBiome());
        assertEquals(world.getWorldSize(), reloaded.getWorldSize());
        assertEquals(world.isAllowAnimals(), reloaded.isAllowAnimals());
        assertEquals(world.isAllowMonsters(), reloaded.isAllowMonsters());
        assertEquals(world.isPvp(), reloaded.isPvp());
        assertEquals(WorldRank.OWNER, reloaded.getUsers().get(ownerId));
        assertEquals(permissions, reloaded.getPermissions());

        Invitation reloadedInvitation = reloaded.getInvitations().get(recipientId);
        assertEquals(ownerId, reloadedInvitation.sender());
        assertEquals(recipientId, reloadedInvitation.recipient());
        assertEquals(12345L, reloadedInvitation.timestamp());
    }

    @Test
    void pocketUserRoundTripsThroughYaml(@TempDir Path tempDir) throws IOException {
        FileDAO dao = new FileDAO(tempDir);
        UUID userId = UUID.randomUUID();

        assertTrue(!dao.registerPocketUser(userId, "Steve"), "a brand new user should not already exist");
        assertTrue(dao.registerPocketUser(userId, "Steve"), "registering the same id twice should report it already exists");

        PocketUser user = dao.getPocketUser(userId);
        user.addWorld(UUID.randomUUID());
        user.getInvitations().add(UUID.randomUUID());
        dao.updatePocketUser(user);

        PocketUser reloaded = dao.getPocketUser(userId);
        assertEquals("Steve", reloaded.getName());
        assertEquals(user.getWorlds(), reloaded.getWorlds());
        assertEquals(user.getInvitations(), reloaded.getInvitations());
    }

    @Test
    void pocketThemeRoundTripsAndDeletes(@TempDir Path tempDir) throws IOException {
        FileDAO dao = new FileDAO(tempDir);
        PocketTheme theme = new PocketTheme(UUID.randomUUID(), "Plains", "A grassy theme.",
                "0.5:100.0:0.5:0.0:0.0", "minecraft:plains", Material.GRASS_BLOCK);

        dao.registerPocketTheme(theme);
        PocketTheme reloaded = dao.getPocketTheme(theme.getId());
        assertEquals(theme.getName(), reloaded.getName());
        assertEquals(theme.getSpawnPoint(), reloaded.getSpawnPoint());

        Set<PocketTheme> all = dao.getAllPocketThemes();
        assertEquals(1, all.size());

        dao.deleteTheme(theme.getId());
        assertNull(dao.getPocketTheme(theme.getId()));
    }
}
