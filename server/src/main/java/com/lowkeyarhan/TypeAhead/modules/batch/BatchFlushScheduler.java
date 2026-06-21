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

// Orchestrates periodic and size-triggered flushes of buffered search events to Postgres.
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
            // RESILIENCE: events in buffer at crash time are lost (bounded to one flush
            // interval). Acceptable for search count updates.
            Map<String, BufferedEvent> drained = searchEventBuffer.drainAndSwap();

            if (drained.isEmpty()) {
                return;
            }

            // Bulk upsert into PostgreSQL database
            String sql = "INSERT INTO query_count (query_text, total_count, recent_count, last_searched_at, created_at) "
                    +
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

            for (Map.Entry<String, BufferedEvent> entry : drained.entrySet()) {
                String queryText = entry.getKey();
                BufferedEvent event = entry.getValue();
                prefixIndexService.applyDelta(queryText, event.count(), event.lastSeenAt());
                // Invalidate all prefix cache keys for this query to ensure immediate
                // consistency
                for (int len = 1; len <= queryText.length(); len++) {
                    cacheNodeManager.invalidate("suggest:" + queryText.substring(0, len) + ":10");
                }
            }

            long distinctQueries = drained.size();
            long totalIncrements = drained.values().stream().mapToLong(BufferedEvent::count).sum();
            long totalReqs = searchEventBuffer.getTotalRequests();
            long flushes = flushCount.incrementAndGet();
            double ratio = flushes > 0 ? (double) totalReqs / flushes : 0.0;

            log.info(
                    "Flush #{} completed: {} distinct queries, {} total increments. Running requests-to-flushes ratio: {} ({} requests / {} flushes)",
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
