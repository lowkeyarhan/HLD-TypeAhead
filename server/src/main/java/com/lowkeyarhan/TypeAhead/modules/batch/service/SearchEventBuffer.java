package com.lowkeyarhan.TypeAhead.modules.batch.service;

import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import com.lowkeyarhan.TypeAhead.modules.batch.BufferedEvent;

// In-memory event buffer accumulating search count increments and last searched timestamps.
// drainAndSwap() is synchronized to prevent lost updates across a flush boundary.
@Component
public class SearchEventBuffer {

    private final Clock clock;
    private ConcurrentHashMap<String, LongAdder> pendingCounts;
    private ConcurrentHashMap<String, Instant> lastSeenAt;
    private final AtomicLong totalRequests;

    public SearchEventBuffer(Clock clock) {
        this.clock = clock;
        this.pendingCounts = new ConcurrentHashMap<>();
        this.lastSeenAt = new ConcurrentHashMap<>();
        this.totalRequests = new AtomicLong(0);
    }

    // Increments count for a query and updates its last searched timestamp.
    public void increment(String queryText) {
        pendingCounts.computeIfAbsent(queryText, k -> new LongAdder()).increment();
        lastSeenAt.put(queryText, Instant.now(clock));
        totalRequests.incrementAndGet();
    }

    // Atomically drains the current accumulated map and swaps in fresh empty maps.
    public synchronized Map<String, BufferedEvent> drainAndSwap() {
        ConcurrentHashMap<String, LongAdder> oldCounts = this.pendingCounts;
        ConcurrentHashMap<String, Instant> oldLastSeen = this.lastSeenAt;
        this.pendingCounts = new ConcurrentHashMap<>();
        this.lastSeenAt = new ConcurrentHashMap<>();
        Map<String, BufferedEvent> drained = new HashMap<>();
        for (String query : oldCounts.keySet()) {
            drained.put(query, new BufferedEvent(oldCounts.get(query).sum(), oldLastSeen.get(query)));
        }
        return drained;
    }

    // Returns the number of distinct queries currently in the buffer.
    public int size() {
        return pendingCounts.size();
    }

    // Returns the cumulative count of search requests received.
    public long getTotalRequests() {
        return totalRequests.get();
    }
}
