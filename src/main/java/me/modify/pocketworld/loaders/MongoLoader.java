package me.modify.pocketworld.loaders;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.exceptions.WorldInUseException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.mongodb.MongoException;
import com.mongodb.client.*;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import me.modify.pocketworld.PocketWorldPlugin;
import me.modify.pocketworld.data.mongo.MongoConnection;
import me.modify.pocketworld.data.mongo.MongoConstant;
import me.modify.pocketworld.world.PocketWorldRegistry;
import org.bson.Document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Represents the Mongo loader related to ASWM
 */
public class MongoLoader implements SlimeLoader {

    public static final long MAX_LOCK_TIME = 300000L;
    public static final long LOCK_INTERVAL = 60000L;

    private static final ScheduledExecutorService SERVICE = Executors.newScheduledThreadPool(2, new ThreadFactoryBuilder()
            .setNameFormat("SWM MongoDB Lock Pool Thread #%1$d").build());

    private final Map<String, ScheduledFuture> lockedWorlds = new HashMap<>();

    private final MongoClient client;
    private final MongoDatabase database;

    private PocketWorldPlugin plugin;
    public MongoLoader(PocketWorldPlugin plugin, MongoConnection mongoConnection) {
        this.plugin = plugin;
        this.client = mongoConnection.getMongoClient();
        this.database = mongoConnection.getMongoDatabase();
    }

    @Override
    public byte[] loadWorld(String worldId, boolean readOnly) throws UnknownWorldException, WorldInUseException, IOException {
        try {
            MongoCollection<Document> collection = database.getCollection(MongoConstant.worldCollection);
            Document worldDoc = collection.find(Filters.eq("_id", worldId)).first();

            if (worldDoc == null) {
                throw new UnknownWorldException(worldId);
            }

            if (!readOnly) {
                long lockedMillis = worldDoc.getLong("locked");

                if (System.currentTimeMillis() - lockedMillis <= MAX_LOCK_TIME) {
                    throw new WorldInUseException(worldId);
                }

                updateLock(worldId, true);
            }

            GridFSBucket bucket = GridFSBuckets.create(database, MongoConstant.worldCollection);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bucket.downloadToStream(worldId, stream);

            return stream.toByteArray();
        } catch (MongoException ex) {
            throw new IOException(ex);
        }
    }

    private void updateLock(String worldName, boolean forceSchedule) {
        try {
            MongoCollection<Document> mongoCollection = database.getCollection(MongoConstant.worldCollection);
            mongoCollection.updateOne(Filters.eq("_id", worldName), Updates.set("locked", System.currentTimeMillis()));
        } catch (MongoException ex) {
            ex.printStackTrace();
        }

        if (forceSchedule || lockedWorlds.containsKey(worldName)) { // Only schedule another update if the world is still on the map
            lockedWorlds.put(worldName, SERVICE.schedule(() -> updateLock(worldName, false), LOCK_INTERVAL, TimeUnit.MILLISECONDS));
        }
    }

    @Override
    public boolean worldExists(String worldId) throws IOException {
        try {
            MongoCollection<Document> mongoCollection = database.getCollection(MongoConstant.worldCollection);
            Document worldDoc = mongoCollection.find(Filters.eq("_id", worldId)).first();

            return worldDoc != null;
        } catch (MongoException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public List<String> listWorlds() throws IOException {
        List<String> worldList = new ArrayList<>();

        try {
            MongoCollection<Document> mongoCollection = database.getCollection(MongoConstant.worldCollection);
            MongoCursor<Document> documents = mongoCollection.find().cursor();

            while (documents.hasNext()) {
                worldList.add(documents.next().getString("_id"));
            }
        } catch (MongoException ex) {
            throw new IOException(ex);
        }

        return worldList;
    }

    @Override
    public void saveWorld(String worldId, byte[] serializedWorld, boolean lock) throws IOException {
        try {
            plugin.getLogger().severe("Save world called on " + worldId);
            GridFSBucket bucket = GridFSBuckets.create(database, MongoConstant.worldCollection);
            GridFSFile oldFile = bucket.find(Filters.eq("filename", worldId)).first();

            bucket.uploadFromStream(worldId, new ByteArrayInputStream(serializedWorld));

            if (oldFile != null) {
                bucket.delete(oldFile.getObjectId());
            }

            MongoCollection<Document> mongoCollection = database.getCollection(MongoConstant.worldCollection);

            // If the world is null, there is a problem
            Document worldDoc = mongoCollection.find(Filters.eq("_id", worldId)).first();

            long lockMillis = lock ? System.currentTimeMillis() : 0L;
            if (worldDoc == null) {
                plugin.getDataSource().getConnection().getDAO()
                        .registerPocketWorld(PocketWorldRegistry.getInstance().getWorld(UUID.fromString(worldId)));
            } else if (System.currentTimeMillis() - worldDoc.getLong("locked") > MAX_LOCK_TIME && lock) {
                updateLock(worldId, true);
            }
        } catch (MongoException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void unlockWorld(String worldId) throws UnknownWorldException, IOException {
        ScheduledFuture future = lockedWorlds.remove(worldId);

        if (future != null) {
            future.cancel(false);
        }

        try {
            MongoCollection<Document> mongoCollection = database.getCollection(MongoConstant.worldCollection);
            UpdateResult result = mongoCollection.updateOne(Filters.eq("_id", worldId), Updates.set("locked", 0L));

            if (result.getMatchedCount() == 0) {
                throw new UnknownWorldException(worldId);
            }
        } catch (MongoException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public boolean isWorldLocked(String worldId) throws UnknownWorldException, IOException {
        if (lockedWorlds.containsKey(worldId)) {
            return true;
        }

        try {
            MongoCollection<Document> mongoCollection = database.getCollection(MongoConstant.worldCollection);
            Document worldDoc = mongoCollection.find(Filters.eq("_id", worldId)).first();

            if (worldDoc == null) {
                throw new UnknownWorldException(worldId);
            }

            return System.currentTimeMillis() - worldDoc.getLong("locked") <= MAX_LOCK_TIME;
        } catch (MongoException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void deleteWorld(String worldId) throws UnknownWorldException, IOException {
        ScheduledFuture future = lockedWorlds.remove(worldId);

        if (future != null) {
            future.cancel(false);
        }

        try {
            GridFSBucket bucket = GridFSBuckets.create(database, MongoConstant.worldCollection);
            GridFSFile file = bucket.find(Filters.eq("filename", worldId)).first();

            if (file == null) {
                throw new UnknownWorldException(worldId);
            }

            bucket.delete(file.getObjectId());

            // Delete backup file
            for (GridFSFile backupFile : bucket.find(Filters.eq("filename", worldId + "_backup"))) {
                bucket.delete(backupFile.getObjectId());
            }

            MongoCollection<Document> mongoCollection = database.getCollection(MongoConstant.worldCollection);
            mongoCollection.deleteOne(Filters.eq("_id", worldId));
        } catch (MongoException ex) {
            throw new IOException(ex);
        }
    }
}
