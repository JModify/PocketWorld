package com.pocketworld.plugin.data.mongo;

import com.pocketworld.plugin.theme.PocketTheme;
import com.pocketworld.plugin.user.PocketUser;
import com.pocketworld.plugin.world.Invitation;
import com.pocketworld.plugin.world.PermissionRank;
import com.pocketworld.plugin.world.PocketWorld;
import com.pocketworld.plugin.world.WorldAction;
import com.pocketworld.plugin.world.WorldRank;
import com.pocketworld.plugin.world.WorldSpawn;
import org.bson.Document;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Adapts plugin domain objects to and from Mongo documents. */
public final class MongoAdapter {

    private MongoAdapter() {
    }

    public static Document pocketThemeToDocument(PocketTheme theme) {
        Document document = new Document("_id", theme.getId().toString());
        document.append("name", theme.getName());
        document.append("biome", theme.getBiome());
        document.append("icon", theme.getIcon().name());
        document.append("description", theme.getDescription());
        document.append("spawnpoint", theme.getSpawnPoint());
        return document;
    }

    public static PocketTheme pocketThemeFromDocument(Document document) {
        UUID themeId = UUID.fromString(document.getString("_id"));
        String name = document.getString("name");
        String biome = document.getString("biome");
        Material icon = Material.valueOf(document.getString("icon"));
        String description = document.getString("description");
        String spawnPoint = document.getString("spawnpoint");
        return new PocketTheme(themeId, name, description, spawnPoint, biome, icon);
    }

    public static Document pocketUserToDocument(PocketUser pocketUser) {
        Document document = new Document("_id", pocketUser.getId().toString());
        document.append("name", pocketUser.getName());
        document.append("invitations", pocketUser.getInvitations().stream()
                .map(id -> new Document("id", id.toString())).toList());
        document.append("worlds", pocketUser.getWorlds().stream()
                .map(id -> new Document("id", id.toString())).toList());
        return document;
    }

    public static PocketUser pocketUserFromDocument(Document document) {
        UUID userId = UUID.fromString(document.getString("_id"));
        String name = document.getString("name");

        @SuppressWarnings("unchecked")
        List<Document> worldsRaw = document.get("worlds", List.class);
        Set<UUID> worlds = new HashSet<>();
        for (Document reference : worldsRaw) {
            worlds.add(UUID.fromString(reference.getString("id")));
        }

        @SuppressWarnings("unchecked")
        List<Document> invitationsRaw = document.get("invitations", List.class);
        Set<UUID> invitations = new HashSet<>();
        for (Document invitation : invitationsRaw) {
            invitations.add(UUID.fromString(invitation.getString("id")));
        }

        return new PocketUser(userId, name, invitations, worlds);
    }

    public static Document pocketWorldToDocument(PocketWorld pocketWorld) {
        Document document = new Document("_id", pocketWorld.getId().toString());
        document.append("name", pocketWorld.getWorldName());
        document.append("icon", pocketWorld.getIcon().name());
        document.append("biome", pocketWorld.getBiome());
        document.append("users", memberMapToDocumentList(pocketWorld.getUsers()));
        document.append("invitations", invitationsMapToDocumentList(pocketWorld.getInvitations()));
        document.append("permissions", permissionsMapToDocument(pocketWorld.getPermissions()));
        document.append("world-size", pocketWorld.getWorldSize());
        document.append("world-spawn", pocketWorld.getWorldSpawn().toString());
        document.append("allow-animals", pocketWorld.isAllowAnimals());
        document.append("allow-monsters", pocketWorld.isAllowMonsters());
        document.append("pvp", pocketWorld.isPvp());
        return document;
    }

    public static PocketWorld pocketWorldFromDocument(Document document) {
        UUID id = UUID.fromString(document.getString("_id"));
        String worldName = document.getString("name");
        String biome = document.getString("biome");
        Material icon = Material.valueOf(document.getString("icon"));

        @SuppressWarnings("unchecked")
        List<Document> usersRaw = document.get("users", List.class);
        Map<UUID, WorldRank> users = new HashMap<>();
        for (Document entry : usersRaw) {
            UUID uuid = UUID.fromString(entry.getString("uuid"));
            WorldRank rank = WorldRank.valueOf(entry.getString("worldrank").toUpperCase());
            users.put(uuid, rank);
        }

        @SuppressWarnings("unchecked")
        List<Document> invitationsRaw = document.get("invitations", List.class);
        Map<UUID, Invitation> invitations = new HashMap<>();
        for (Document entry : invitationsRaw) {
            UUID recipient = UUID.fromString(entry.getString("recipient"));
            UUID sender = UUID.fromString(entry.getString("sender"));
            long timestamp = entry.getLong("sent-at");
            invitations.put(recipient, new Invitation(sender, recipient, timestamp));
        }

        Document permissionsRaw = document.get("permissions", Document.class);
        Map<PermissionRank, Set<WorldAction>> permissions = permissionsRaw != null
                ? permissionsMapFromDocument(permissionsRaw)
                : PocketWorld.defaultPermissions();

        int worldSize = document.getInteger("world-size");
        WorldSpawn worldSpawn = WorldSpawn.fromString(document.getString("world-spawn"));
        boolean allowAnimals = document.getBoolean("allow-animals");
        boolean allowMonsters = document.getBoolean("allow-monsters");
        boolean pvp = document.getBoolean("pvp");

        return new PocketWorld(id, worldName, icon, users, invitations, biome, worldSize, worldSpawn,
                allowAnimals, allowMonsters, pvp, false, permissions);
    }

    public static Document permissionsMapToDocument(Map<PermissionRank, Set<WorldAction>> permissions) {
        Document document = new Document();
        for (Map.Entry<PermissionRank, Set<WorldAction>> entry : permissions.entrySet()) {
            document.append(entry.getKey().name(), entry.getValue().stream().map(Enum::name).toList());
        }
        return document;
    }

    public static Map<PermissionRank, Set<WorldAction>> permissionsMapFromDocument(Document document) {
        Map<PermissionRank, Set<WorldAction>> permissions = new EnumMap<>(PermissionRank.class);
        for (String key : document.keySet()) {
            @SuppressWarnings("unchecked")
            List<String> actions = document.get(key, List.class);
            permissions.put(PermissionRank.valueOf(key), actions.stream().map(WorldAction::valueOf)
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(WorldAction.class))));
        }
        return permissions;
    }

    public static List<Document> memberMapToDocumentList(Map<UUID, WorldRank> users) {
        List<Document> documents = new ArrayList<>();
        for (Map.Entry<UUID, WorldRank> entry : users.entrySet()) {
            documents.add(new Document("uuid", entry.getKey().toString())
                    .append("worldrank", entry.getValue().toString()));
        }
        return documents;
    }

    /**
     * Invitations are keyed by recipient in {@link PocketWorld#getInvitations()} - this list is
     * written and read using the named {@code recipient}/{@code sender} fields on each document
     * rather than relying on map key/value position, so the two directions can never drift apart
     * the way the original implementation's read path did (it reconstructed the map with sender and
     * recipient swapped relative to how it was written, silently breaking invitation lookups after
     * any cache-miss reload).
     */
    public static List<Document> invitationsMapToDocumentList(Map<UUID, Invitation> invitations) {
        List<Document> documents = new ArrayList<>();
        for (Invitation invitation : invitations.values()) {
            documents.add(new Document("recipient", invitation.recipient().toString())
                    .append("sender", invitation.sender().toString())
                    .append("sent-at", invitation.timestamp()));
        }
        return documents;
    }
}
