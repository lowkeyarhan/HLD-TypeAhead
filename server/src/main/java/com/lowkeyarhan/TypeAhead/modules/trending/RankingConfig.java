package com.lowkeyarhan.TypeAhead.modules.trending;

import com.lowkeyarhan.TypeAhead.common.config.RankingProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import java.util.Map;

// Configuration class to dynamically wire the active RankingStrategy primary bean based on configuration.
@Configuration
public class RankingConfig {

    @Bean
    @Primary
    public RankingStrategy activeRankingStrategy(
            RankingProperties rankingProperties,
            Map<String, RankingStrategy> strategies) {
        
        String strategyName = rankingProperties.getStrategy();
        RankingStrategy strategy = strategies.get(strategyName);
        if (strategy == null) {
            // Default fallback to recency-aware strategy if not specified or unrecognized
            strategy = strategies.get("recency-aware");
        }
        return strategy;
    }
}
