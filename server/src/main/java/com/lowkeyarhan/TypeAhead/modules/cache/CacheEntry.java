package com.lowkeyarhan.TypeAhead.modules.cache;

import java.time.Clock;
import java.time.Instant;

// Represents a cache entry with value and expiration metadata.
public record CacheEntry<T>(T value, Instant expiresAt) {

    // Returns true if the entry is expired according to the provided clock.
    public boolean isExpired(Clock clock) {
        return Instant.now(clock).isAfter(expiresAt);
    }
}
