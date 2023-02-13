package me.modify.pocketworld.data.mongo;

import me.modify.pocketworld.theme.PocketTheme;
import me.modify.pocketworld.user.PocketUser;
import me.modify.pocketworld.world.PocketWorld;
import me.modify.pocketworld.world.WorldRank;
import me.modify.pocketworld.world.WorldSpawn;
import org.bson.Document;
import org.bukkit.Material;

import java.util.*;

/**
 * Adapts plugin related objects to and from Mongo objects.
 */
public class MongoAdapter {

    /**
     * Adapts a PocketTheme object into a document
     * @param theme theme to adapt
     * @return document of pocket theme
     */
    public static Document pocketThemeToDocument(PocketTheme theme) {
        UUID themeId = theme.getId();
        Document document = new Document("_id", themeId.toString());
        document.append("name", theme.getName());
        document.append("biome", theme.getBiome());
        document.append("icon", theme.getIcon().name());
        document.append("description", theme.getDescription());
        document.append("spawnpoint", theme.getSpawnPoint());
        return document;
    }

    /**
     * Adapts a theme document into a PocketTheme object.
     * @param document document to adapt
     * @return a PocketTheme object representing the document.
     */
    public static PocketTheme pocketThemeFromDocument(Document document) {
        UUID themeId = UUID.fromString(document.getString("_id"));
        String name = document.getString("name");
        String biome = document.getString("biome");
        Material icon = Material.valueOf(document.getString("icon"));
        String description = document.getString("description");
        String spawnPoint = document.getString("spawnpoint");
        return new PocketTheme(themeId, name, description, spawnPoint, biome, icon);
    }

    /**
     * Adapts a PocketUser object into a document.
     * @param pocketUser pocket user to adapt
     * @return document to adapt
     */
    public static Document pocketUserToDocument(PocketUser pocketUser) {
        Set<UUID> worlds = pocketUser.getWorlds();
        UUID userId = pocketUser.getId();

        Document document = new Document("_id", userId.toString());
        document.append("name", pocketUser.getName());
        List<Document> worldReferences = worlds.stream().map(id -> new Document("id", id.toString())).toList();
        document.append("worlds", worldReferences);
        return document;
    }

    /**
     * Adapts a user document into a PocketUser object.
     * @param document document to convert
     * @return pocket user object
     */
    public static PocketUser pocketUserFromDocument(Document document) {
        UUID userId = UUID.fromString(document.getString("_id"));
        String name = document.getString("name");
        List<Document> referencesRaw = document.get("worlds", List.class);

        Set<UUID> worlds = new HashSet<>();
        for (Document reference : referencesRaw) {
            worlds.add(UUID.fromString(reference.getString("id")));
        }

        return new PocketUser(userId, name, worlds);
    }

    public static Document pocketWorldToDocument(PocketWorld pocketWorld) {
        Document document = new Document("_id", pocketWorld.getId().toString());
        document.append("name", pocketWorld.getWorldName());
        document.append("icon", pocketWorld.getIcon().name());
        document.append("biome", pocketWorld.getBiome());
        document.append("users", memberMapToDocumentList(pocketWorld.getUsers()));
        document.append("world-size", pocketWorld.getWorldSize());
        document.append("world-spawn", pocketWorld.getWorldSpawn().toString());
        document.append("allow-animals", pocketWorld.isAllowAnimals());
        document.append("allow-monsters", pocketWorld.isAllowMonsters());
        document.append("pvp", pocketWorld.isPvp());
        document.append("locked", pocketWorld.getLocked());
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

        int worldSize = document.getInteger("world-size");
        WorldSpawn worldSpawn = WorldSpawn.fromString(document.getString("world-spawn"));
        boolean allowAnimals = document.getBoolean("allow-animals");
        boolean allowMonsters = document.getBoolean("allow-monsters");
        boolean pvp = document.getBoolean("pvp");
        long locked = document.getLong("locked");

        return new PocketWorld(id, worldName, icon, locked, users, biome, worldSize, worldSpawn,
                allowAnimals, allowMonsters, pvp, false);
    }

    public static List<Document> memberMapToDocumentList(Map<UUID, WorldRank> map) {
        List<Document> documents = new ArrayList<>();

        for (Map.Entry<UUID, WorldRank> entry : map.entrySet()) {
            UUID id = entry.getKey();
            WorldRank rank = entry.getValue();

            Document member = new Document("uuid", id.toString());
            member.append("worldrank", rank.toString());

            documents.add(member);
        }

        return documents;
    }

}
