package com.lowkeyarhan.TypeAhead.modules.index;

import com.lowkeyarhan.TypeAhead.common.metrics.MetricsService;
import com.lowkeyarhan.TypeAhead.modules.data.QueryCount;
import com.lowkeyarhan.TypeAhead.modules.data.repository.QueryCountRepository;
import com.lowkeyarhan.TypeAhead.modules.index.service.impl.PrefixIndexServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// Unit test verifying prefix search logic and subMap operations.
class PrefixIndexServiceTest {

    private QueryCountRepository repository;
    private MetricsService metricsService;
    private PrefixIndexServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(QueryCountRepository.class);
        metricsService = Mockito.mock(MetricsService.class);
        service = new PrefixIndexServiceImpl(repository, metricsService);
    }

    @Test
    void testSearchReturnsCorrectPrefixMatchesSortedByCount() {
        List<QueryCount> data = List.of(
                new QueryCount(1L, "apple", 10L, 0L, null, Instant.now()),
                new QueryCount(2L, "apricot", 5L, 0L, null, Instant.now()),
                new QueryCount(3L, "banana", 20L, 0L, null, Instant.now()),
                new QueryCount(4L, "application", 15L, 0L, null, Instant.now()));
        when(repository.findAll()).thenReturn(data);
        service.rebuild();

        // Search for "ap"
        List<QueryCount> results = service.search("ap", 10);
        assertThat(results).hasSize(3);
        // Sorted by total count descending: "application" (15), "apple" (10), "apricot"
        // (5)
        assertThat(results.get(0).getQueryText()).isEqualTo("application");
        assertThat(results.get(1).getQueryText()).isEqualTo("apple");
        assertThat(results.get(2).getQueryText()).isEqualTo("apricot");
    }

    @Test
    void testSearchCaseInsensitiveAndTrims() {
        List<QueryCount> data = List.of(
                new QueryCount(1L, "apple", 10L, 0L, null, Instant.now()));
        when(repository.findAll()).thenReturn(data);
        service.rebuild();

        List<QueryCount> results = service.search("  ApP  ", 10);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getQueryText()).isEqualTo("apple");
    }

    @Test
    void testSearchEmptyAndNoMatch() {
        List<QueryCount> data = List.of(
                new QueryCount(1L, "apple", 10L, 0L, null, Instant.now()));
        when(repository.findAll()).thenReturn(data);
        service.rebuild();

        assertThat(service.search("", 10)).isEmpty();
        assertThat(service.search("  ", 10)).isEmpty();
        assertThat(service.search(null, 10)).isEmpty();
        assertThat(service.search("orange", 10)).isEmpty();
    }

    @Test
    void testApplyDeltaIncrementsCounts() {
        service.applyDelta("cherry", 5L, Instant.now());
        List<QueryCount> results = service.search("che", 10);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getQueryText()).isEqualTo("cherry");
        assertThat(results.get(0).getTotalCount()).isEqualTo(5L);

        service.applyDelta("cherry", 3L, Instant.now());
        results = service.search("che", 10);
        assertThat(results.get(0).getTotalCount()).isEqualTo(8L);
    }

    @Test
    void testSearchPerformanceOnLargeDataset() {
        List<QueryCount> largeData = new ArrayList<>();
        for (int i = 0; i < 100000; i++) {
            largeData.add(new QueryCount((long) i, "query_" + i, (long) (100000 - i), 0L, null, Instant.now()));
        }
        when(repository.findAll()).thenReturn(largeData);
        service.rebuild();

        long start = System.nanoTime();
        List<QueryCount> results = service.search("query_999", 10);
        long end = System.nanoTime();

        long durationMs = (end - start) / 1000000;
        assertThat(results).isNotEmpty();
        // Assert search is faster than 15ms
        assertThat(durationMs).isLessThan(15);
    }
}
