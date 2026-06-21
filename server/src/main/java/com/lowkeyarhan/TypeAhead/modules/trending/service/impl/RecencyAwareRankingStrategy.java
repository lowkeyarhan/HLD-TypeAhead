package com.lowkeyarhan.TypeAhead.modules.trending.service.impl;

import com.lowkeyarhan.TypeAhead.common.config.RankingProperties;
import com.lowkeyarhan.TypeAhead.modules.data.QueryCount;
import com.lowkeyarhan.TypeAhead.modules.suggestion.dto.SuggestResultDTO;
import com.lowkeyarhan.TypeAhead.modules.suggestion.mapper.SuggestionMapper;
import com.lowkeyarhan.TypeAhead.modules.trending.service.RankingStrategy;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

// Recency-aware ranking strategy that decays recent count over time using a half-life formula.
@Component("recency-aware")
public class RecencyAwareRankingStrategy implements RankingStrategy {

    private final RankingProperties rankingProperties;
    private final Clock clock;

    public RecencyAwareRankingStrategy(RankingProperties rankingProperties, Clock clock) {
        this.rankingProperties = rankingProperties;
        this.clock = clock;
    }

    @Override
    public List<SuggestResultDTO> rank(List<QueryCount> candidates, int limit) {
        if (candidates == null) {
            return List.of();
        }

        double halfLife = rankingProperties.getDecayHalfLifeMinutes();
        Instant now = Instant.now(clock);

        return candidates.stream()
                .map(qc -> {
                    double score = calculateScore(qc, now, halfLife);
                    return new CandidateWithScore(qc, score);
                })
                .sorted((c1, c2) -> {
                    int scoreCompare = Double.compare(c2.score(), c1.score());
                    if (scoreCompare != 0) {
                        return scoreCompare;
                    }
                    // Tie-breaker: alphabetical on query text
                    return c1.queryCount().getQueryText().compareTo(c2.queryCount().getQueryText());
                })
                .limit(limit)
                .map(c -> SuggestionMapper.toSuggestResultDTO(c.queryCount()))
                .collect(Collectors.toList());
    }

    // Calculates the decayed score: totalCount + recentCount *
    // 0.5^(minutesSinceLastSearch / halfLife)
    private double calculateScore(QueryCount qc, Instant now, double halfLifeMinutes) {
        long totalCount = qc.getTotalCount();
        long recentCount = qc.getRecentCount();
        Instant lastSearched = qc.getLastSearchedAt();

        if (recentCount <= 0 || lastSearched == null) {
            return totalCount;
        }

        double minutesSinceLastSearch = Duration.between(lastSearched, now).toMillis() / 60000.0;
        // Clamp clock skew defensively
        if (minutesSinceLastSearch < 0) {
            minutesSinceLastSearch = 0;
        }

        double decayFactor = Math.pow(0.5, minutesSinceLastSearch / halfLifeMinutes);
        return totalCount + (recentCount * decayFactor);
    }

    // Local record to carry computed score alongside the candidate QueryCount
    private record CandidateWithScore(QueryCount queryCount, double score) {
    }
}
