package me.modify.pocketworld.data.mongo;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import me.modify.pocketworld.data.DAO;
import me.modify.pocketworld.theme.PocketTheme;
import me.modify.pocketworld.user.PocketUser;
import me.modify.pocketworld.world.PocketWorld;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Mongo Data Access Object
 * All access to MongoDB is done through this class.
 */
public class MongoDAO implements DAO {

    private final MongoConnection connection;
    public MongoDAO(MongoConnection connection) {
        this.connection = connection;
    }

    @Override
    public void registerPocketWorld(PocketWorld world) {
        if (world == null) return;
        connection.getMongoDatabase().getCollection(MongoConstant.worldCollection)
                .insertOne(MongoAdapter.pocketWorldToDocument(world));
    }

    @Override
    public PocketWorld getPocketWorld(UUID worldId) {
        MongoCollection<Document> worldCollection = connection.getMongoDatabase()
                .getCollection(MongoConstant.worldCollection);
        Document worldDocument = worldCollection.find(Filters.eq("_id", worldId.toString())).first();
        return worldDocument != null ? MongoAdapter.pocketWorldFromDocument(worldDocument) : null;
    }

    @Override
    public void updatePocketWorld(PocketWorld world) {
        MongoCollection<Document> worldCollection = connection.getMongoDatabase()
                .getCollection(MongoConstant.worldCollection);
        Document replacement = MongoAdapter.pocketWorldToDocument(world);
        worldCollection.replaceOne(Filters.eq("_id", world.getId().toString()), replacement);
    }

    @Override
    public boolean registerPocketUser(UUID userId, String username) {
        MongoCollection<Document> userCollection = connection.getMongoDatabase()
                .getCollection(MongoConstant.userCollection);

        Document userDoc = userCollection.find(Filters.eq("_id", userId.toString())).first();
        if (userDoc == null) {
            PocketUser user = new PocketUser(userId, username, new HashSet<>(), new HashSet<>());
            connection.getMongoDatabase().getCollection(MongoConstant.userCollection)
                    .insertOne(MongoAdapter.pocketUserToDocument(user));
            return false;
        }

        return true;
    }

    @Override
    public PocketUser getPocketUser(UUID userId) {
        MongoCollection<Document> userCollection = connection.getMongoDatabase()
                .getCollection(MongoConstant.userCollection);
        Document userDocument = userCollection.find(Filters.eq("_id", userId.toString())).first();
        return userDocument != null ? MongoAdapter.pocketUserFromDocument(userDocument) : null;
    }

    @Override
    public void updatePocketUser(PocketUser user) {
        MongoCollection<Document> userCollection = connection.getMongoDatabase()
                .getCollection(MongoConstant.userCollection);
        Document replacement = MongoAdapter.pocketUserToDocument(user);
        userCollection.replaceOne(Filters.eq("_id", user.getId().toString()), replacement);
    }

    @Deprecated
    public void updatePocketUser(UUID userId, Bson update) {
        MongoCollection<Document> userCollection = connection.getMongoDatabase()
                .getCollection(MongoConstant.userCollection);
        userCollection.findOneAndUpdate(Filters.eq("_id", userId.toString()), update);
    }

    @Override
    public PocketTheme getPocketTheme(UUID themeId) {
        MongoCollection<Document> themeCollection = connection.getMongoDatabase()
                .getCollection(MongoConstant.themeCollection);
        Document themeDocument = themeCollection.find(Filters.eq("_id", themeId.toString())).first();
        return themeDocument != null ? MongoAdapter.pocketThemeFromDocument(themeDocument) : null;
    }

    @Override
    public void registerPocketTheme(PocketTheme theme) {
        connection.getMongoDatabase().getCollection(MongoConstant.themeCollection)
                .insertOne(MongoAdapter.pocketThemeToDocument(theme));
    }

    @Override
    public Set<PocketTheme> getAllPocketThemes() {
        MongoCollection<Document> themeCollection = connection.getMongoDatabase()
                .getCollection(MongoConstant.themeCollection);
        FindIterable<Document> documents = themeCollection.find();
        MongoCursor<Document> cursor = documents.cursor();

        Set<PocketTheme> themes = new HashSet<>();
        try {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                PocketTheme theme = MongoAdapter.pocketThemeFromDocument(doc);
                themes.add(theme);
            }
        } finally {
            cursor.close();
        }

        return themes;
    }

    @Override
    public void deleteTheme(UUID themeId) {
        MongoCollection<Document> themeCollection = connection.getMongoDatabase()
                .getCollection(MongoConstant.themeCollection);
        themeCollection.deleteOne(Filters.eq("_id", themeId.toString()));
    }
}
