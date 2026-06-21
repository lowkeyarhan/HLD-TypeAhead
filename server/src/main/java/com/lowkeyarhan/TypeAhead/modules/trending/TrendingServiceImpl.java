package com.lowkeyarhan.TypeAhead.modules.trending;

import com.lowkeyarhan.TypeAhead.modules.index.PrefixIndexService;
import com.lowkeyarhan.TypeAhead.modules.suggestion.dto.SuggestResultDTO;
import org.springframework.stereotype.Service;
import java.util.List;

// Implementation of TrendingService using PrefixIndexService and the active primary RankingStrategy.
@Service
public class TrendingServiceImpl implements TrendingService {

    private final PrefixIndexService prefixIndexService;
    private final RankingStrategy rankingStrategy;

    public TrendingServiceImpl(PrefixIndexService prefixIndexService, RankingStrategy rankingStrategy) {
        this.prefixIndexService = prefixIndexService;
        this.rankingStrategy = rankingStrategy;
    }

    @Override
    public List<SuggestResultDTO> getTrending(int limit) {
        return rankingStrategy.rank(prefixIndexService.getAllCandidates(), limit);
    }
}
