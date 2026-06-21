package com.lowkeyarhan.TypeAhead.modules.trending;

import com.lowkeyarhan.TypeAhead.modules.data.QueryCount;
import com.lowkeyarhan.TypeAhead.modules.suggestion.dto.SuggestResultDTO;
import java.util.List;

// Strategy interface for ranking query candidates.
public interface RankingStrategy {
    List<SuggestResultDTO> rank(List<QueryCount> candidates, int limit);
}
