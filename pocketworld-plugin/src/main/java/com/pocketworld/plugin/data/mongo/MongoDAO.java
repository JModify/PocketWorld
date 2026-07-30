package com.pocketworld.plugin.data.mongo;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.pocketworld.plugin.data.DAO;
import com.pocketworld.plugin.theme.PocketTheme;
import com.pocketworld.plugin.user.PocketUser;
import com.pocketworld.plugin.world.PocketWorld;
import org.bson.Document;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MongoDAO implements DAO {

    private final MongoConnection connection;

    public MongoDAO(MongoConnection connection) {
        this.connection = connection;
    }

    @Override
    public void registerPocketWorld(PocketWorld world) {
        if (world == null) {
            return;
        }
        connection.getMongoDatabase().getCollection(MongoConstant.WORLD_COLLECTION)
                .insertOne(MongoAdapter.pocketWorldToDocument(world));
    }

    @Override
    public PocketWorld getPocketWorld(UUID worldId) {
        MongoCollection<Document> worldCollection = connection.getMongoDatabase()
                .getCollection(MongoConstant.WORLD_COLLECTION);
        Document worldDocument = worldCollection.find(Filters.eq("_id", worldId.toString())).first();
        return worldDocument != null ? MongoAdapter.pocketWorldFromDocument(worldDocument) : null;
    }

    @Override
    public void updatePocketWorld(PocketWorld world) {
        MongoCollection<Document> worldCollection = connection.getMongoDatabase()
                .getCollection(MongoConstant.WORLD_COLLECTION);
        worldCollection.replaceOne(Filters.eq("_id", world.getId().toString()), MongoAdapter.pocketWorldToDocument(world));
    }

    @Override
    public boolean registerPocketUser(UUID userId, String username) {
        MongoCollection<Document> userCollection = connection.getMongoDatabase()
                .getCollection(MongoConstant.USER_COLLECTION);

        Document userDoc = userCollection.find(Filters.eq("_id", userId.toString())).first();
        if (userDoc == null) {
            PocketUser user = new PocketUser(userId, username, new HashSet<>(), new HashSet<>());
            userCollection.insertOne(MongoAdapter.pocketUserToDocument(user));
            return false;
        }

        return true;
    }

    @Override
    public PocketUser getPocketUser(UUID userId) {
        MongoCollection<Document> userCollection = connection.getMongoDatabase()
                .getCollection(MongoConstant.USER_COLLECTION);
        Document userDocument = userCollection.find(Filters.eq("_id", userId.toString())).first();
        return userDocument != null ? MongoAdapter.pocketUserFromDocument(userDocument) : null;
    }

    @Override
    public void updatePocketUser(PocketUser user) {
        MongoCollection<Document> userCollection = connection.getMongoDatabase()
                .getCollection(MongoConstant.USER_COLLECTION);
        userCollection.replaceOne(Filters.eq("_id", user.getId().toString()), MongoAdapter.pocketUserToDocument(user));
    }

    @Override
    public PocketTheme getPocketTheme(UUID themeId) {
        MongoCollection<Document> themeCollection = connection.getMongoDatabase()
                .getCollection(MongoConstant.THEME_COLLECTION);
        Document themeDocument = themeCollection.find(Filters.eq("_id", themeId.toString())).first();
        return themeDocument != null ? MongoAdapter.pocketThemeFromDocument(themeDocument) : null;
    }

    @Override
    public void registerPocketTheme(PocketTheme theme) {
        connection.getMongoDatabase().getCollection(MongoConstant.THEME_COLLECTION)
                .insertOne(MongoAdapter.pocketThemeToDocument(theme));
    }

    @Override
    public Set<PocketTheme> getAllPocketThemes() {
        MongoCollection<Document> themeCollection = connection.getMongoDatabase()
                .getCollection(MongoConstant.THEME_COLLECTION);

        Set<PocketTheme> themes = new HashSet<>();
        FindIterable<Document> documents = themeCollection.find();
        try (MongoCursor<Document> cursor = documents.cursor()) {
            while (cursor.hasNext()) {
                themes.add(MongoAdapter.pocketThemeFromDocument(cursor.next()));
            }
        }

        return themes;
    }

    @Override
    public void deleteTheme(UUID themeId) {
        connection.getMongoDatabase().getCollection(MongoConstant.THEME_COLLECTION)
                .deleteOne(Filters.eq("_id", themeId.toString()));
    }
}
