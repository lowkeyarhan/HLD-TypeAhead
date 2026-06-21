package com.lowkeyarhan.TypeAhead;

import com.lowkeyarhan.TypeAhead.modules.batch.BatchFlushScheduler;
import com.lowkeyarhan.TypeAhead.modules.cache.CacheController;
import com.lowkeyarhan.TypeAhead.modules.cache.CacheDebugResponseDTO;
import com.lowkeyarhan.TypeAhead.modules.data.QueryCount;
import com.lowkeyarhan.TypeAhead.modules.data.QueryCountRepository;
import com.lowkeyarhan.TypeAhead.modules.index.PrefixIndexService;
import com.lowkeyarhan.TypeAhead.modules.suggestion.SuggestionController;
import com.lowkeyarhan.TypeAhead.modules.suggestion.dto.SuggestionResponseDTO;
import com.lowkeyarhan.TypeAhead.modules.search.SearchController;
import com.lowkeyarhan.TypeAhead.modules.search.dto.SearchRequestDTO;
import com.lowkeyarhan.TypeAhead.modules.trending.TrendingController;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

// End-to-end integration test verifying query ingestion, routing diagnostics, cache invalidation, and ranking.
@SpringBootTest
@ActiveProfiles("test")
public class EndToEndIntegrationTest {

    @Autowired
    private SuggestionController suggestionController;

    @Autowired
    private SearchController searchController;

    @Autowired
    private CacheController cacheController;

    @Autowired
    private TrendingController trendingController;

    @Autowired
    private PrefixIndexService prefixIndexService;

    @Autowired
    private BatchFlushScheduler batchFlushScheduler;

    @MockBean
    private QueryCountRepository repository;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @Test
    void testEndToEndSystemFlow() {
        // Step 1: Initialize DB state (Mocked) and rebuild index
        List<QueryCount> initialDbRows = List.of(
                new QueryCount(1L, "iphone 13", 10L, 0L, null, Instant.now()),
                new QueryCount(2L, "iphone 14", 20L, 0L, null, Instant.now()));
        when(repository.findAll()).thenReturn(initialDbRows);
        prefixIndexService.rebuild();

        // Verify routing diagnostics shows miss on the key initially
        ResponseEntity<CacheDebugResponseDTO> debugResponse0 = cacheController.getDebugInfo("iph", 10);
        assertThat(debugResponse0.getBody().hit()).isFalse();

        // Step 2: Query suggestions. First call should be cache miss (and will populate
        // cache).
        ResponseEntity<SuggestionResponseDTO> suggestResponse1 = suggestionController.suggest("iph", 10);
        assertThat(suggestResponse1.getStatusCode().is2xxSuccessful()).isTrue();
        SuggestionResponseDTO dto1 = suggestResponse1.getBody();
        assertThat(dto1).isNotNull();
        assertThat(dto1.suggestions()).hasSize(2);
        // Default ranking: popularity sorting puts "iphone 14" (20) before "iphone 13"
        // (10)
        assertThat(dto1.suggestions().get(0).query()).isEqualTo("iphone 14");

        // Verify routing diagnostics shows hit on the key now that it is cached
        ResponseEntity<CacheDebugResponseDTO> debugResponse1 = cacheController.getDebugInfo("iph", 10);
        assertThat(debugResponse1.getBody().hit()).isTrue();

        // Step 3: Query suggestions again. Should be cache hit now.
        ResponseEntity<SuggestionResponseDTO> suggestResponse2 = suggestionController.suggest("iph", 10);
        assertThat(suggestResponse2.getStatusCode().is2xxSuccessful()).isTrue();

        // Step 4: Submit multiple searches for "iphone 13" (historically less popular)
        // Submit 15 searches to push its count above "iphone 14"
        for (int i = 0; i < 15; i++) {
            searchController.search(new SearchRequestDTO("iphone 13"));
        }

        // Step 5: Trigger flush. Writes buffered increments to DB and updates index +
        // invalidates cache.
        batchFlushScheduler.flush();

        // Step 6: Verify cache invalidation occurred
        ResponseEntity<CacheDebugResponseDTO> debugResponse3 = cacheController.getDebugInfo("iph", 10);
        assertThat(debugResponse3.getBody().hit()).isFalse();

        // Step 7: Retrieve suggestions again. Check that "iphone 13" now outranks
        // "iphone 14"
        ResponseEntity<SuggestionResponseDTO> suggestResponse3 = suggestionController.suggest("iph", 10);
        SuggestionResponseDTO dto3 = suggestResponse3.getBody();
        assertThat(dto3).isNotNull();
        assertThat(dto3.suggestions()).hasSize(2);
        // "iphone 13" count should be updated: 10 (base) + 15 (searches) = 25
        assertThat(dto3.suggestions().get(0).query()).isEqualTo("iphone 13");
        assertThat(dto3.suggestions().get(0).count()).isEqualTo(25L);
        assertThat(dto3.suggestions().get(1).query()).isEqualTo("iphone 14");
    }
}
