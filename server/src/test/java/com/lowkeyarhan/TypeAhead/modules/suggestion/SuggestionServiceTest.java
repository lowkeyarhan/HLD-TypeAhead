package com.lowkeyarhan.TypeAhead.modules.suggestion;

import com.lowkeyarhan.TypeAhead.modules.data.QueryCount;
import com.lowkeyarhan.TypeAhead.modules.index.service.PrefixIndexService;
import com.lowkeyarhan.TypeAhead.modules.cache.service.CacheNodeManager;
import com.lowkeyarhan.TypeAhead.modules.trending.service.impl.PopularityRankingStrategy;
import com.lowkeyarhan.TypeAhead.modules.suggestion.service.impl.SuggestionServiceImpl;
import com.lowkeyarhan.TypeAhead.modules.suggestion.dto.SuggestResultDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Unit test verifying suggestion service mapping logic.
class SuggestionServiceTest {

    private PrefixIndexService prefixIndexService;
    private CacheNodeManager cacheNodeManager;
    private SuggestionServiceImpl service;

    @BeforeEach
    void setUp() {
        prefixIndexService = Mockito.mock(PrefixIndexService.class);
        cacheNodeManager = Mockito.mock(CacheNodeManager.class);
        service = new SuggestionServiceImpl(prefixIndexService, cacheNodeManager, new PopularityRankingStrategy());
    }

    @Test
    void testGetSuggestionsMapsIndexResultsToDto() {
        List<QueryCount> matching = List.of(
                new QueryCount(1L, "iphone 15", 50L, 0L, null, Instant.now()),
                new QueryCount(2L, "iphone charger", 20L, 0L, null, Instant.now()));
        Mockito.when(cacheNodeManager.get(Mockito.anyString())).thenReturn(java.util.Optional.empty());
        Mockito.when(prefixIndexService.search("iphone", Integer.MAX_VALUE)).thenReturn(matching);

        List<SuggestResultDTO> results = service.getSuggestions("iphone", 10);
        assertThat(results).hasSize(2);
        assertThat(results.get(0).query()).isEqualTo("iphone 15");
        assertThat(results.get(0).count()).isEqualTo(50L);
        assertThat(results.get(1).query()).isEqualTo("iphone charger");
        assertThat(results.get(1).count()).isEqualTo(20L);
    }
}
