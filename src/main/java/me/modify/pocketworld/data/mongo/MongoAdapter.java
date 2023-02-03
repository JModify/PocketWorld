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
    public static Document pocketThemeToDocument(PocketTheme theme) {
        UUID themeId = theme.getId();
        Document document = new Document("_id", themeId.toString());
        document.append("name", theme.getName());
        document.append("biome", theme.getBiome());
        document.append("icon", theme.getIcon().name());
        return document;
    }

    public static PocketTheme pocketThemeFromDocument(Document document) {
        UUID themeId = UUID.fromString(document.getString("_id"));
        String name = document.getString("name");
        String biome = document.getString("biome");
        Material icon = Material.valueOf(document.getString("icon"));
        return new PocketTheme(themeId, name, biome, icon);
    }

    /**
     * Converts a PocketUser object into a document.
     * @param pocketUser pocket user to convert
     * @return document
     */
    public static Document pocketUserToDocument(PocketUser pocketUser) {
        Set<UUID> worlds = pocketUser.getWorlds();
        UUID userId = pocketUser.getId();

        Document document = new Document("_id", userId.toString());
        List<Document> worldReferences = worlds.stream().map(id -> new Document("id", id.toString())).toList();
        document.append("worlds", worldReferences);
        document.append("inventory", pocketUser.getInventory());
        return document;
    }

    /**
     * Converts a user document into a valid PocketUser object.
     * @param document document to convert
     * @return pocket user object
     */
    public static PocketUser pocketUserFromDocument(Document document) {
        UUID userId = UUID.fromString(document.getString("_id"));
        List<Document> referencesRaw = document.get("worlds", List.class);

        Set<UUID> worlds = new HashSet<>();
        for (Document reference : referencesRaw) {
            worlds.add(UUID.fromString(reference.getString("id")));
        }
        String inventory = document.getString("inventory");

        return new PocketUser(userId, worlds, inventory);
    }

    public static Document pocketWorldToDocument(PocketWorld pocketWorld) {
        Document document = new Document("_id", pocketWorld.getId().toString());
        document.append("theme", pocketWorld.getThemeId().toString());
        document.append("name", pocketWorld.getWorldName());
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
        UUID themeId = UUID.fromString(document.getString("theme"));
        String worldName = document.getString("name");

        @SuppressWarnings("unchecked")
        List<Document> usersRaw = document.get("users", List.class);

        Map<UUID, WorldRank> users = new HashMap<>();
        for (Document entry : usersRaw) {
            UUID uuid = UUID.fromString(entry.getString("uuid"));
            WorldRank rank = WorldRank.valueOf(entry.getString("worldrank").toUpperCase());
            users.put(uuid, rank);
        }

        int worldSize = document.getInteger("world-size");
        WorldSpawn worldSpawn = WorldSpawn.fromString("world-spawn");
        boolean allowAnimals = document.getBoolean("allow-animals");
        boolean allowMonsters = document.getBoolean("allow-monsters");
        boolean pvp = document.getBoolean("pvp");
        long locked = document.getLong("locked");

        return new PocketWorld(id, themeId, worldName, locked, users, worldSize, worldSpawn,
                allowAnimals, allowMonsters, pvp);
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
