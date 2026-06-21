package com.lowkeyarhan.TypeAhead.modules.cache;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

// Represents a logical cache node holding items in memory and tracking hit/miss statistics.
public class CacheNode {

    private final String id;
    private final ConcurrentHashMap<String, CacheEntry<Object>> cacheMap;
    private final AtomicLong hitCount;
    private final AtomicLong missCount;

    public CacheNode(String id) {
        this.id = id;
        this.cacheMap = new ConcurrentHashMap<>();
        this.hitCount = new AtomicLong(0);
        this.missCount = new AtomicLong(0);
    }

    public String getId() {
        return id;
    }

    // Retrieves an entry from cache node, checking expiration and incrementing
    // metrics.
    public Optional<Object> get(String key, Clock clock) {
        CacheEntry<Object> entry = cacheMap.get(key);
        if (entry == null) {
            missCount.incrementAndGet();
            return Optional.empty();
        }
        if (entry.isExpired(clock)) {
            cacheMap.remove(key);
            missCount.incrementAndGet();
            return Optional.empty();
        }
        hitCount.incrementAndGet();
        return Optional.of(entry.value());
    }

    // Puts a value into the cache node with an explicit expiration.
    public void put(String key, Object value, Instant expiresAt) {
        cacheMap.put(key, new CacheEntry<>(value, expiresAt));
    }

    // Invalidates a specific key in this cache node.
    public void invalidate(String key) {
        cacheMap.remove(key);
    }

    // Checks if a key has a valid, non-expired entry without incrementing hit/miss
    // counters.
    public boolean hasValidEntry(String key, java.time.Clock clock) {
        CacheEntry<Object> entry = cacheMap.get(key);
        if (entry == null) {
            return false;
        }
        if (entry.isExpired(clock)) {
            cacheMap.remove(key);
            return false;
        }
        return true;
    }

    // Clears all entries in this node.
    public void clear() {
        cacheMap.clear();
    }

    public long getHitCount() {
        return hitCount.get();
    }

    public long getMissCount() {
        return missCount.get();
    }

    // Exposes the underlying map size for testing and diagnostics.
    public int size() {
        return cacheMap.size();
    }
}
