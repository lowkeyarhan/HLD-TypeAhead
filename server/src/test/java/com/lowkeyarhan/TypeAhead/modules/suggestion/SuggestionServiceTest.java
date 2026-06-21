package com.lowkeyarhan.TypeAhead.modules.suggestion;

import com.lowkeyarhan.TypeAhead.modules.data.QueryCount;
import com.lowkeyarhan.TypeAhead.modules.index.PrefixIndexService;
import com.lowkeyarhan.TypeAhead.modules.suggestion.dto.SuggestResultDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// Unit test verifying suggestion service mapping logic.
class SuggestionServiceTest {

    private PrefixIndexService prefixIndexService;
    private SuggestionServiceImpl service;

    @BeforeEach
    void setUp() {
        prefixIndexService = Mockito.mock(PrefixIndexService.class);
        service = new SuggestionServiceImpl(prefixIndexService);
    }

    @Test
    void testGetSuggestionsMapsIndexResultsToDto() {
        List<QueryCount> matching = List.of(
                new QueryCount(1L, "iphone 15", 50L, 0L, null, Instant.now()),
                new QueryCount(2L, "iphone charger", 20L, 0L, null, Instant.now()));
        when(prefixIndexService.search("iphone", 10)).thenReturn(matching);

        List<SuggestResultDTO> results = service.getSuggestions("iphone", 10);
        assertThat(results).hasSize(2);
        assertThat(results.get(0).query()).isEqualTo("iphone 15");
        assertThat(results.get(0).count()).isEqualTo(50L);
        assertThat(results.get(1).query()).isEqualTo("iphone charger");
        assertThat(results.get(1).count()).isEqualTo(20L);
    }
}
