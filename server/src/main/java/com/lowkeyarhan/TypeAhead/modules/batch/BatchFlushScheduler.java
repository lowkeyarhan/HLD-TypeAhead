package com.lowkeyarhan.TypeAhead.modules.batch;

import com.lowkeyarhan.TypeAhead.common.metrics.MetricsService;
import com.lowkeyarhan.TypeAhead.modules.cache.CacheNodeManager;
import com.lowkeyarhan.TypeAhead.modules.index.PrefixIndexService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

// Orchestrates periodic and size-triggered asynchronous flushes of search events to the database,
// updating in-memory indexes and invalidating caches.
@Slf4j
@Component
public class BatchFlushScheduler {

    private final SearchEventBuffer searchEventBuffer;
    private final JdbcTemplate jdbcTemplate;
    private final PrefixIndexService prefixIndexService;
    private final CacheNodeManager cacheNodeManager;
    private final MetricsService metricsService;
    private final AtomicBoolean isFlushing;
    private final AtomicLong flushCount;

    public BatchFlushScheduler(SearchEventBuffer searchEventBuffer,
                               JdbcTemplate jdbcTemplate,
                               PrefixIndexService prefixIndexService,
                               CacheNodeManager cacheNodeManager,
                               MetricsService metricsService) {
        this.searchEventBuffer = searchEventBuffer;
        this.jdbcTemplate = jdbcTemplate;
        this.prefixIndexService = prefixIndexService;
        this.cacheNodeManager = cacheNodeManager;
        this.metricsService = metricsService;
        this.isFlushing = new AtomicBoolean(false);
        this.flushCount = new AtomicLong(0);
    }

    // Triggered on a fixed delay configured in application.yml.
    @Scheduled(fixedDelayString = "${typeahead.batch.flush-interval-ms}")
    public void scheduledFlush() {
        flush();
    }

    // Triggers an early flush asynchronously.
    public void triggerFlushAsync() {
        CompletableFuture.runAsync(this::flush);
    }

    // Performs the flush operation, writing buffered data to Postgres.
    public void flush() {
        // Prevent concurrent flushes (scheduled vs early trigger)
        if (!isFlushing.compareAndSet(false, true)) {
            return;
        }

        try {
            // RESILIENCE TRADEOFF COMMENT:
            // Buffered writes in SearchEventBuffer are held purely in memory. In the event of an abrupt JVM crash or
            // power failure, any events sitting in this buffer that have not yet been flushed will be lost.
            // This represents a bounded data loss of at most one flush interval (default 5 seconds).
            // For a search typeahead suggestion system, this is a highly acceptable trade-off because query statistics
            // are not critical transactional data. Small, occasional loss of count updates does not compromise system correctness.
            // A stronger alternative to prevent data loss would be implementing a Write-Ahead Log (WAL) or pushing events to a
            // persistent queue (like Kafka or RabbitMQ) before processing, but that would introduce significant latency,
            // infrastructure overhead, and complexity.
            Map<String, BufferedEvent> drained = searchEventBuffer.drainAndSwap();

            if (drained.isEmpty()) {
                return;
            }

            // Bulk upsert into PostgreSQL database
            String sql = "INSERT INTO query_count (query_text, total_count, recent_count, last_searched_at, created_at) " +
                    "VALUES (?, ?, ?, ?, ?) " +
                    "ON CONFLICT (query_text) DO UPDATE SET " +
                    "total_count = query_count.total_count + EXCLUDED.total_count, " +
                    "recent_count = query_count.recent_count + EXCLUDED.recent_count, " +
                    "last_searched_at = EXCLUDED.last_searched_at";

            List<Object[]> batchArgs = new ArrayList<>();
            for (Map.Entry<String, BufferedEvent> entry : drained.entrySet()) {
                String queryText = entry.getKey();
                BufferedEvent event = entry.getValue();
                Timestamp ts = Timestamp.from(event.lastSeenAt());
                batchArgs.add(new Object[] { queryText, event.count(), event.count(), ts, ts });
            }

            jdbcTemplate.batchUpdate(sql, batchArgs);
            metricsService.incrementDbWrites(batchArgs.size());

            // Update in-memory prefix index incrementally and invalidate cache keys for immediate read consistency
            for (Map.Entry<String, BufferedEvent> entry : drained.entrySet()) {
                String queryText = entry.getKey();
                BufferedEvent event = entry.getValue();
                
                prefixIndexService.applyDelta(queryText, event.count(), event.lastSeenAt());

                // CACHE CONSISTENCY CHOICE:
                // We invalidate all prefix query cache entries matching prefixes of this updated query.
                // This ensures suggestions reflect changes immediately instead of waiting for cache TTL.
                for (int len = 1; len <= queryText.length(); len++) {
                    String prefix = queryText.substring(0, len);
                    // Invalidate default limit 10 cache keys
                    cacheNodeManager.invalidate("suggest:" + prefix + ":10");
                }
            }

            // Calculate and log stats and requests-to-flushes ratio
            long distinctQueries = drained.size();
            long totalIncrements = drained.values().stream().mapToLong(BufferedEvent::count).sum();
            long totalReqs = searchEventBuffer.getTotalRequests();
            long flushes = flushCount.incrementAndGet();
            double ratio = flushes > 0 ? (double) totalReqs / flushes : 0.0;

            log.info("Flush #{} completed: {} distinct queries, {} total increments. Running requests-to-flushes ratio: {} ({} requests / {} flushes)",
                    flushes, distinctQueries, totalIncrements, String.format("%.2f", ratio), totalReqs, flushes);

        } catch (Exception e) {
            log.error("Error occurred during batch flush", e);
        } finally {
            isFlushing.set(false);
        }
    }

    // Exposes cumulative flush count for metrics.
    public long getFlushCount() {
        return flushCount.get();
    }
}
