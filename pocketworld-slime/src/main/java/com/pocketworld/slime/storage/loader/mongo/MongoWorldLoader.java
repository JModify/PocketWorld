package com.pocketworld.slime.storage.loader.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.pocketworld.slime.storage.UnknownWorldException;
import com.pocketworld.slime.storage.WorldLoader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

/**
 * Stores each world as a GridFS file keyed by filename == world id, so world size isn't bounded by
 * the 16MB BSON document limit a plain collection would impose.
 */
public final class MongoWorldLoader implements WorldLoader, AutoCloseable {

    private final MongoClient client;
    private final GridFSBucket bucket;

    public MongoWorldLoader(String connectionString, String databaseName, String bucketName) {
        this.client = MongoClients.create(connectionString);
        MongoDatabase database = client.getDatabase(databaseName);
        this.bucket = GridFSBuckets.create(database, bucketName);
    }

    @Override
    public boolean exists(String worldId) {
        return findFile(worldId) != null;
    }

    @Override
    public byte[] read(String worldId) throws IOException {
        if (findFile(worldId) == null) {
            throw new UnknownWorldException(worldId);
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        bucket.downloadToStream(worldId, buffer);
        return buffer.toByteArray();
    }

    @Override
    public void write(String worldId, byte[] data) {
        // GridFS allows multiple revisions under the same filename; for a key-value store we want
        // exactly one, so the previous revision (if any) is removed first.
        GridFSFile existing = findFile(worldId);
        if (existing != null) {
            bucket.delete(existing.getObjectId());
        }
        bucket.uploadFromStream(worldId, new ByteArrayInputStream(data));
    }

    @Override
    public void delete(String worldId) throws IOException {
        GridFSFile file = findFile(worldId);
        if (file == null) {
            throw new UnknownWorldException(worldId);
        }
        bucket.delete(file.getObjectId());
    }

    @Override
    public List<String> list() {
        List<String> ids = new ArrayList<>();
        for (GridFSFile file : bucket.find()) {
            ids.add(file.getFilename());
        }
        return ids;
    }

    private GridFSFile findFile(String worldId) {
        return bucket.find(eq("filename", worldId)).first();
    }

    @Override
    public void close() {
        client.close();
    }
}
