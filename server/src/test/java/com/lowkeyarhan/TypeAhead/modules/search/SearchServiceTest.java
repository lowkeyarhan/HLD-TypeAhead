package com.lowkeyarhan.TypeAhead.modules.search;

import com.lowkeyarhan.TypeAhead.common.config.BatchProperties;
import com.lowkeyarhan.TypeAhead.modules.batch.service.BatchFlushScheduler;
import com.lowkeyarhan.TypeAhead.modules.batch.service.SearchEventBuffer;
import com.lowkeyarhan.TypeAhead.modules.search.service.impl.SearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// Unit test verifying SearchService query buffering.
class SearchServiceTest {

    private SearchEventBuffer searchEventBuffer;
    private BatchFlushScheduler batchFlushScheduler;
    private BatchProperties batchProperties;
    private SearchServiceImpl service;

    @BeforeEach
    void setUp() {
        searchEventBuffer = Mockito.mock(SearchEventBuffer.class);
        batchFlushScheduler = Mockito.mock(BatchFlushScheduler.class);
        batchProperties = Mockito.mock(BatchProperties.class);
        Mockito.when(batchProperties.getSizeThreshold()).thenReturn(500);
        service = new SearchServiceImpl(searchEventBuffer, batchFlushScheduler, batchProperties);
    }

    @Test
    void testSubmitSearchCallsBufferIncrement() {
        String rawQuery = "  IpHoNe 15  ";
        String normalized = "iphone 15";

        service.submitSearch(rawQuery);

        verify(searchEventBuffer, times(1)).increment(normalized);
    }
}
