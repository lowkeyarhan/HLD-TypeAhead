package com.lowkeyarhan.TypeAhead.modules.batch;

import com.lowkeyarhan.TypeAhead.common.metrics.MetricsService;
import com.lowkeyarhan.TypeAhead.modules.cache.service.CacheNodeManager;
import com.lowkeyarhan.TypeAhead.modules.index.service.PrefixIndexService;
import com.lowkeyarhan.TypeAhead.modules.batch.service.BatchFlushScheduler;
import com.lowkeyarhan.TypeAhead.modules.batch.service.SearchEventBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import static org.mockito.Mockito.*;

// Unit tests for BatchFlushScheduler lifecycle and integration actions during database flush.
class BatchFlushSchedulerTest {

    private SearchEventBuffer searchEventBuffer;
    private JdbcTemplate jdbcTemplate;
    private PrefixIndexService prefixIndexService;
    private CacheNodeManager cacheNodeManager;
    private MetricsService metricsService;
    private BatchFlushScheduler scheduler;

    @BeforeEach
    void setUp() {
        searchEventBuffer = mock(SearchEventBuffer.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        prefixIndexService = mock(PrefixIndexService.class);
        cacheNodeManager = mock(CacheNodeManager.class);
        metricsService = mock(MetricsService.class);
        scheduler = new BatchFlushScheduler(searchEventBuffer, jdbcTemplate, prefixIndexService, cacheNodeManager,
                metricsService);
    }

    @Test
    void testFlushSkipsWhenEmpty() {
        when(searchEventBuffer.drainAndSwap()).thenReturn(Map.of());

        scheduler.flush();

        verifyNoInteractions(jdbcTemplate);
        verifyNoInteractions(prefixIndexService);
        verifyNoInteractions(cacheNodeManager);
    }

    @Test
    void testFlushExecutesBatchUpsertAndUpdatesIndexAndInvalidatesCache() {
        Map<String, BufferedEvent> drained = new HashMap<>();
        Instant now = Instant.now();
        drained.put("iphone", new BufferedEvent(5L, now));

        when(searchEventBuffer.drainAndSwap()).thenReturn(drained);
        when(searchEventBuffer.getTotalRequests()).thenReturn(5L);

        scheduler.flush();

        verify(jdbcTemplate, times(1)).batchUpdate(anyString(), anyList());
        verify(prefixIndexService, times(1)).applyDelta("iphone", 5L, now);

        // CacheNodeManager should be invalidated for all prefixes of the query: "i",
        // "ip", "iph", "ipho", "iphon", "iphone"
        verify(cacheNodeManager, times(1)).invalidate("suggest:i:10");
        verify(cacheNodeManager, times(1)).invalidate("suggest:ip:10");
        verify(cacheNodeManager, times(1)).invalidate("suggest:iph:10");
        verify(cacheNodeManager, times(1)).invalidate("suggest:ipho:10");
        verify(cacheNodeManager, times(1)).invalidate("suggest:iphon:10");
        verify(cacheNodeManager, times(1)).invalidate("suggest:iphone:10");
    }
}
