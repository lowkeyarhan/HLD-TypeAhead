package com.lowkeyarhan.TypeAhead.modules.trending;

import com.lowkeyarhan.TypeAhead.common.config.RankingProperties;
import com.lowkeyarhan.TypeAhead.modules.data.QueryCount;
import com.lowkeyarhan.TypeAhead.modules.suggestion.dto.SuggestResultDTO;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

// Unit tests for RecencyAwareRankingStrategy decay calculations and sorting characteristics.
class RecencyAwareRankingStrategyTest {

    @Test
    void testDecayScoringAndTieBreaker() {
        RankingProperties props = new RankingProperties();
        props.setDecayHalfLifeMinutes(30.0);
        props.setStrategy("recency-aware");

        Instant baseTime = Instant.parse("2026-06-21T12:00:00Z");
        Clock clock = Clock.fixed(baseTime, ZoneId.of("UTC"));

        RecencyAwareRankingStrategy strategy = new RecencyAwareRankingStrategy(props, clock);

        // Score logic: totalCount + recentCount * 0.5^(minutesSinceLastSearch /
        // halfLife)
        // qc1: 100 + 50 * 0.5^(0 / 30) = 150.0
        // qc2: 120 + 40 * 0.5^(30 / 30) = 120 + 20 = 140.0
        // qc3: 130 + 60 * 0.5^(60 / 30) = 130 + 15 = 145.0
        // qc4: 140 + 0 = 140.0
        // qc5: 140 + 10 * 0.5^(future_clamped_to_0 / 30) = 150.0
        // qc6: 150 + 0 = 150.0

        QueryCount qc1 = new QueryCount(1L, "query1", 100L, 50L, baseTime, Instant.now());
        QueryCount qc2 = new QueryCount(2L, "query2", 120L, 40L, baseTime.minusSeconds(1800), Instant.now());
        QueryCount qc3 = new QueryCount(3L, "query3", 130L, 60L, baseTime.minusSeconds(3600), Instant.now());
        QueryCount qc4 = new QueryCount(4L, "query4", 140L, 0L, null, Instant.now());
        QueryCount qc5 = new QueryCount(5L, "query5", 140L, 10L, baseTime.plusSeconds(60), Instant.now());
        QueryCount qc6 = new QueryCount(6L, "query6", 150L, 0L, null, Instant.now());

        List<QueryCount> candidates = List.of(qc1, qc2, qc3, qc4, qc5, qc6);

        // Sorting expected order (highest to lowest, secondary tie-breaker
        // alphabetically):
        // 1. query1 (150.0)
        // 2. query5 (150.0)
        // 3. query6 (150.0)
        // 4. query3 (145.0)
        // 5. query2 (140.0)
        // 6. query4 (140.0)
        List<SuggestResultDTO> ranked = strategy.rank(candidates, 10);

        assertThat(ranked).hasSize(6);
        assertThat(ranked.get(0).query()).isEqualTo("query1");
        assertThat(ranked.get(1).query()).isEqualTo("query5");
        assertThat(ranked.get(2).query()).isEqualTo("query6");
        assertThat(ranked.get(3).query()).isEqualTo("query3");
        assertThat(ranked.get(4).query()).isEqualTo("query2");
        assertThat(ranked.get(5).query()).isEqualTo("query4");
    }
}
