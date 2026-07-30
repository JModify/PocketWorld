package com.pocketworld.plugin.cache;

import com.pocketworld.plugin.PocketWorldPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Reach-through cache: reads fall back to the data source and populate the cache on the way back. */
public abstract class PocketCache<T> {

    protected final Map<UUID, T> cache;
    protected final PocketWorldPlugin plugin;

    public PocketCache(PocketWorldPlugin plugin) {
        this.plugin = plugin;
        this.cache = new ConcurrentHashMap<>();
    }

    public boolean contains(UUID itemId) {
        return cache.containsKey(itemId);
    }

    public T readThrough(UUID itemId) {
        if (contains(itemId)) {
            return cache.get(itemId);
        }

        return get(itemId);
    }

    /** Returns the cached item, or {@code null} if it isn't cached - never falls through to the data source. */
    public T getIfCached(UUID itemId) {
        return cache.get(itemId);
    }

    /** Adds an item into this cache. Does NOT persist it - only for newly created objects. */
    public void add(UUID itemId, T item) {
        cache.put(itemId, item);
    }

    /** Removes an item from this cache. Does NOT delete it from the data source. */
    public void remove(UUID itemId) {
        cache.remove(itemId);
    }

    /** Retrieves the item directly from the data source, caching it on success. */
    protected abstract T get(UUID itemId);

    /** Flushes the target item from this cache to the data source, if cached. */
    public abstract void flush(UUID itemId);

    /** Flushes all cached items to the data source. */
    public abstract void flush();
}
