package me.modify.pocketworld.data.mongo;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import me.modify.pocketworld.data.DAO;
import me.modify.pocketworld.theme.PocketTheme;
import me.modify.pocketworld.user.PocketUser;
import me.modify.pocketworld.world.PocketWorld;
import me.modify.pocketworld.world.PocketWorldRegistry;
import org.bson.Document;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Mongo Data Access Object
 * All access to MongoDB is done through this class.
 */
public class MongoDAO implements DAO {

    private MongoConnection connection;
    public MongoDAO(MongoConnection connection) {
        this.connection = connection;
    }

    @Override
    public PocketWorld getPocketWorld(UUID worldId) {
        MongoCollection<Document> worldCollection = connection.getMongoDatabase().getCollection(MongoConstant.worldCollection);
        Document worldDocument = worldCollection.find(Filters.eq("_id", worldId.toString())).first();
        return worldDocument != null ? MongoAdapter.pocketWorldFromDocument(worldDocument) : null;
    }

    @Override
    public void updatePocketWorld(PocketWorld world) {
        MongoCollection<Document> userCollection = connection.getMongoDatabase().getCollection(MongoConstant.worldCollection);
        Document replacement = MongoAdapter.pocketWorldToDocument(world);
        userCollection.replaceOne(Filters.eq("_id", world.getId().toString()), replacement);
    }

    @Override
    public PocketUser getPocketUser(UUID userId) {
        MongoCollection<Document> userCollection = connection.getMongoDatabase().getCollection(MongoConstant.userCollection);
        Document userDocument = userCollection.find(Filters.eq("_id", userId.toString())).first();
        return userDocument != null ? MongoAdapter.pocketUserFromDocument(userDocument) : null;
    }

    @Override
    public PocketTheme getPocketTheme(UUID themeId) {
        MongoCollection<Document> themeCollection = connection.getMongoDatabase().getCollection(MongoConstant.themeCollection);
        Document themeDocument = themeCollection.find(Filters.eq("_id", themeId.toString())).first();
        return themeDocument != null ? MongoAdapter.pocketThemeFromDocument(themeDocument) : null;
    }

    @Override
    public void updatePocketUser(PocketUser user) {
        MongoCollection<Document> userCollection = connection.getMongoDatabase().getCollection(MongoConstant.userCollection);
        Document replacement = MongoAdapter.pocketUserToDocument(user);
        userCollection.replaceOne(Filters.eq("_id", user.getId().toString()), replacement);
    }

    @Override
    public Set<PocketTheme> getAllPocketThemes() {
        MongoCollection<Document> themeCollection = connection.getMongoDatabase().getCollection(MongoConstant.themeCollection);
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
    public List<PocketWorld> getPocketWorlds(UUID userId) {
        PocketUser user = getPocketUser(userId);

        Set<UUID> worldIds = user.getWorlds();
        PocketWorldRegistry worldRegistry = PocketWorldRegistry.getInstance();


        List<PocketWorld> worlds = new ArrayList<>();
        for (UUID worldId : worldIds) {
            PocketWorld loadedWorld = worldRegistry.getWorld(worldId);

            // If the target world is loaded, get it from the loaded world registry (faster)
            if (loadedWorld != null) {
                worlds.add(loadedWorld);
                continue;
            }

            // If the target world is not loaded, grab from data source (slower)
            PocketWorld unloadedWorld = getPocketWorld(worldId);
            worlds.add(unloadedWorld);
        }

        return worlds;
    }

    @Override
    public int countPocketWorlds(UUID userId) {
        PocketUser user = getPocketUser(userId);
        Set<UUID> worldIds = user.getWorlds();
        return worldIds.size();
    }

    @Override
    public void registerPocketUser(UUID userId) {
        PocketUser user = new PocketUser(userId, new HashSet<>(), null);
        if (getPocketUser(userId) == null) {
            connection.getMongoDatabase().getCollection(MongoConstant.userCollection)
                    .insertOne(MongoAdapter.pocketUserToDocument(user));
        }
    }

    @Override
    public void registerPocketWorld(PocketWorld world) {
        connection.getMongoDatabase().getCollection(MongoConstant.worldCollection)
                .insertOne(MongoAdapter.pocketWorldToDocument(world));
    }

    @Override
    public void registerPocketTheme(PocketTheme theme) {
        connection.getMongoDatabase().getCollection(MongoConstant.themeCollection)
                .insertOne(MongoAdapter.pocketThemeToDocument(theme));
    }

    @Override
    public void saveUserInventory(UUID userId, Inventory inventory) {
        ItemStack[] items = inventory.getContents();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream bukkitObjectOutputStream = new BukkitObjectOutputStream(byteArrayOutputStream);

            bukkitObjectOutputStream.writeInt(items.length);

            for (ItemStack item : items) {
                bukkitObjectOutputStream.writeObject(item);
            }
            bukkitObjectOutputStream.flush();

            byte[] rawData = byteArrayOutputStream.toByteArray();
            String encodedInventory = Base64.getEncoder().encodeToString(rawData);

            bukkitObjectOutputStream.close();
            byteArrayOutputStream.close();

            connection.getMongoDatabase().getCollection(MongoConstant.userCollection).findOneAndUpdate(
                    Filters.eq("_id", userId.toString()), Updates.set("inventory", encodedInventory));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ItemStack[] retrieveUserInventory(UUID userId) {
        MongoCollection<Document> userCollection = connection.getMongoDatabase().getCollection(MongoConstant.userCollection);
        Document userDoc = userCollection.find(Filters.eq("_id", userId.toString())).first();

        if (userDoc == null) {
            return null;
        }

        String encodedInventory = userDoc.getString("inventory");

        if (encodedInventory == null || encodedInventory.isEmpty() || encodedInventory.isBlank()) {
            return null;
        }

        byte[] rawData = Base64.getDecoder().decode(encodedInventory);

        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(rawData);
            BukkitObjectInputStream bukkitObjectInputStream = new BukkitObjectInputStream(byteArrayInputStream);

            int itemsCount = bukkitObjectInputStream.readInt();
            ItemStack[] items = new ItemStack[itemsCount];

            for (int i = 0; i < itemsCount; i++) {
                items[i] = (ItemStack) bukkitObjectInputStream.readObject();
            }

            bukkitObjectInputStream.close();
            byteArrayInputStream.close();

            userDoc.replace("inventory", null);
            userCollection.replaceOne(Filters.eq("_id", userId.toString()), userDoc);
            return items;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }
}
