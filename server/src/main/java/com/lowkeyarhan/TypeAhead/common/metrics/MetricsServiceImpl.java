package com.lowkeyarhan.TypeAhead.common.metrics;

import com.lowkeyarhan.TypeAhead.modules.batch.service.BatchFlushScheduler;
import com.lowkeyarhan.TypeAhead.modules.batch.service.SearchEventBuffer;
import com.lowkeyarhan.TypeAhead.modules.cache.CacheNode;
import com.lowkeyarhan.TypeAhead.modules.cache.service.CacheNodeManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

// Implementation of MetricsService collecting rolling latencies, database events, and cache rates.
@Service
public class MetricsServiceImpl implements MetricsService {

    private static final int MAX_SAMPLES = 10000;
    private final ConcurrentLinkedQueue<Long> latencySamples;
    private final AtomicInteger sampleSize;

    private final AtomicLong dbReadCount;
    private final AtomicLong dbWriteCount;

    private final CacheNodeManager cacheNodeManager;
    private final SearchEventBuffer searchEventBuffer;
    private final BatchFlushScheduler batchFlushScheduler;

    public MetricsServiceImpl(CacheNodeManager cacheNodeManager,
                              SearchEventBuffer searchEventBuffer,
                              @Lazy BatchFlushScheduler batchFlushScheduler) {
        this.cacheNodeManager = cacheNodeManager;
        this.searchEventBuffer = searchEventBuffer;
        this.batchFlushScheduler = batchFlushScheduler;
        this.latencySamples = new ConcurrentLinkedQueue<>();
        this.sampleSize = new AtomicInteger(0);
        this.dbReadCount = new AtomicLong(0);
        this.dbWriteCount = new AtomicLong(0);
    }

    @Override
    public void recordSuggestLatency(long durationNanos) {
        latencySamples.add(durationNanos);
        if (sampleSize.incrementAndGet() > MAX_SAMPLES) {
            if (latencySamples.poll() != null) {
                sampleSize.decrementAndGet();
            }
        }
    }

    @Override
    public double getSuggestP95LatencyMs() {
        List<Long> samples = new ArrayList<>(latencySamples);
        if (samples.isEmpty()) {
            return 0.0;
        }
        Collections.sort(samples);
        int index = (int) Math.ceil(0.95 * samples.size()) - 1;
        if (index < 0) {
            index = 0;
        }
        return samples.get(index) / 1_000_000.0;
    }

    @Override
    public double getOverallCacheHitRate() {
        long hits = 0;
        long misses = 0;
        for (CacheNode node : cacheNodeManager.getNodes()) {
            hits += node.getHitCount();
            misses += node.getMissCount();
        }
        long total = hits + misses;
        return total > 0 ? (double) hits / total : 0.0;
    }

    @Override
    public Map<String, Double> getPerNodeCacheHitRates() {
        Map<String, Double> rates = new HashMap<>();
        for (CacheNode node : cacheNodeManager.getNodes()) {
            long hits = node.getHitCount();
            long misses = node.getMissCount();
            long total = hits + misses;
            rates.put(node.getId(), total > 0 ? (double) hits / total : 0.0);
        }
        return rates;
    }

    @Override
    public long getDbReadCount() {
        return dbReadCount.get();
    }

    @Override
    public long getDbWriteCount() {
        return dbWriteCount.get();
    }

    @Override
    public void incrementDbReads(long delta) {
        dbReadCount.addAndGet(delta);
    }

    @Override
    public void incrementDbWrites(long delta) {
        dbWriteCount.addAndGet(delta);
    }

    @Override
    public double getRequestsToFlushesRatio() {
        long totalReqs = searchEventBuffer.getTotalRequests();
        long flushes = batchFlushScheduler.getFlushCount();
        return flushes > 0 ? (double) totalReqs / flushes : 0.0;
    }
}
