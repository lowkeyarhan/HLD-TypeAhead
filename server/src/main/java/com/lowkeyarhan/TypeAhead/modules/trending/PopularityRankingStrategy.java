package com.lowkeyarhan.TypeAhead.modules.trending;

import com.lowkeyarhan.TypeAhead.modules.data.QueryCount;
import com.lowkeyarhan.TypeAhead.modules.suggestion.dto.SuggestResultDTO;
import org.springframework.stereotype.Component;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

// Popularity-based ranking strategy sorting candidates by total count descending, falling back to query text alphabetically.
@Component("popularity")
public class PopularityRankingStrategy implements RankingStrategy {

    @Override
    public List<SuggestResultDTO> rank(List<QueryCount> candidates, int limit) {
        if (candidates == null) {
            return List.of();
        }
        return candidates.stream()
                .sorted(Comparator.comparing(QueryCount::getTotalCount).reversed()
                        .thenComparing(QueryCount::getQueryText))
                .limit(limit)
                .map(qc -> new SuggestResultDTO(qc.getQueryText(), qc.getTotalCount()))
                .collect(Collectors.toList());
    }
}
