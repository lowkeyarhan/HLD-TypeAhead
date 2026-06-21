package com.lowkeyarhan.TypeAhead.modules.search.service.impl;

import com.lowkeyarhan.TypeAhead.common.config.BatchProperties;
import com.lowkeyarhan.TypeAhead.common.util.QueryNormalizer;
import com.lowkeyarhan.TypeAhead.modules.batch.service.BatchFlushScheduler;
import com.lowkeyarhan.TypeAhead.modules.batch.service.SearchEventBuffer;
import com.lowkeyarhan.TypeAhead.modules.search.service.SearchService;
import org.springframework.stereotype.Service;

// Implementation class for SearchService, buffering search events in-memory.
@Service
public class SearchServiceImpl implements SearchService {

    private final SearchEventBuffer searchEventBuffer;
    private final BatchFlushScheduler batchFlushScheduler;
    private final BatchProperties batchProperties;

    public SearchServiceImpl(SearchEventBuffer searchEventBuffer,
                             BatchFlushScheduler batchFlushScheduler,
                             BatchProperties batchProperties) {
        this.searchEventBuffer = searchEventBuffer;
        this.batchFlushScheduler = batchFlushScheduler;
        this.batchProperties = batchProperties;
    }

    @Override
    public void submitSearch(String queryText) {
        String normalized = QueryNormalizer.normalize(queryText);
        if (normalized.isEmpty()) {
            return;
        }
        
        searchEventBuffer.increment(normalized);

        // Size-triggered early flush
        if (searchEventBuffer.size() >= batchProperties.getSizeThreshold()) {
            batchFlushScheduler.triggerFlushAsync();
        }
    }
}
