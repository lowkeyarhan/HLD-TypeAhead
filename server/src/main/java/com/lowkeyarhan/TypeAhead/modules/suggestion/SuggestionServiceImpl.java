package com.lowkeyarhan.TypeAhead.modules.suggestion;

import com.lowkeyarhan.TypeAhead.modules.cache.CacheNodeManager;
import com.lowkeyarhan.TypeAhead.modules.index.PrefixIndexService;
import com.lowkeyarhan.TypeAhead.modules.suggestion.dto.SuggestResultDTO;
import com.lowkeyarhan.TypeAhead.modules.trending.RankingStrategy;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

// Implementation class for SuggestionService, retrieving results from prefix index.
@Service
public class SuggestionServiceImpl implements SuggestionService {

    private final PrefixIndexService prefixIndexService;
    private final CacheNodeManager cacheNodeManager;
    private final RankingStrategy rankingStrategy;

    public SuggestionServiceImpl(PrefixIndexService prefixIndexService,
            CacheNodeManager cacheNodeManager,
            RankingStrategy rankingStrategy) {
        this.prefixIndexService = prefixIndexService;
        this.cacheNodeManager = cacheNodeManager;
        this.rankingStrategy = rankingStrategy;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SuggestResultDTO> getSuggestions(String prefix, int limit) {
        String cacheKey = "suggest:" + prefix + ":" + limit;
        Optional<Object> cached = cacheNodeManager.get(cacheKey);
        if (cached.isPresent()) {
            return (List<SuggestResultDTO>) cached.get();
        }

        // Fetch matches from prefix index without limiting too early, allowing the
        // ranking strategy to evaluate all candidates matching this prefix.
        List<SuggestResultDTO> results = rankingStrategy.rank(prefixIndexService.search(prefix, Integer.MAX_VALUE),
                limit);

        cacheNodeManager.put(cacheKey, results);
        return results;
    }
}
