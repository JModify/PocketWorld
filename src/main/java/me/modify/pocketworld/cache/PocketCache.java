package me.modify.pocketworld.cache;

import me.modify.pocketworld.PocketWorldPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class PocketCache<T> {

    /** The hash map containing the cached items themselves */
    protected Map<UUID, T> cache;

    /** The main plugin instance */
    protected PocketWorldPlugin plugin;

    /**
     * Constructs a new PocketCache
     * @param plugin main plugin instance
     */
    public PocketCache(PocketWorldPlugin plugin) {
        this.plugin = plugin;
        this.cache = new ConcurrentHashMap<>();
    }

    /**
     * Determines if this cache contains an item under the provided id.
     * @param itemId id of item to check.
     * @return true if cache contains, else false.
     */
    public boolean contains(UUID itemId) {
        return cache.containsKey(itemId);
    }

    /**
     * Adopts reach through strategy to return data from the cache if it exists,
     * otherwise queries the data source for the item under the given ID, returns it then stores in cache
     * on the way back.
     * @param itemId id of item to get
     * @return the item under the given id
     */
    public T readThrough(UUID itemId) {
        if (contains(itemId)) {
            return cache.get(itemId);
        }

        return get(itemId);
    }

    /**
     * Adds an item into this cache.
     * Does NOT add the item to the data source, this method should only be used for newly created objects.
     * @param itemId id of item to add
     * @param item the item itself
     */
    public void add(UUID itemId, T item) {
        cache.put(itemId, item);
    }

    /**
     * Removes an item from this cache if it contains it.
     * Does NOT delete the item from data source, that should be done directly via DAO.
     * @param itemId id of item to delete from cache.
     */
    public void remove(UUID itemId) {
        cache.remove(itemId);
    }

    /**
     * Retrieves the item under the given ID directly from the data source.
     * @param itemId id of item to retrieve
     * @return item under the given id OR null if such an item does not exist in data source.
     */
    protected abstract T get(UUID itemId);

    /**
     * Flush the target item from this cache to data source.
     * If the item does not exist in this cache, nothing will be done.
     * @param itemId id of object to flush
     */
    public abstract void flush(UUID itemId);

    /**
     * Flush all contents of this cache.
     */
    public abstract void flush();

}
